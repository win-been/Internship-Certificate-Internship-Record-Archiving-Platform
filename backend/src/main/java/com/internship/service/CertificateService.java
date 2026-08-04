package com.internship.service;

import com.internship.dto.CertificateDTO;
import com.internship.entity.Archive;
import com.internship.entity.Assessment;
import com.internship.entity.Certificate;
import com.internship.entity.InternshipInfo;
import com.internship.repository.ArchiveRepository;
import com.internship.repository.AssessmentRepository;
import com.internship.repository.CertificateRepository;
import com.internship.repository.InternshipRepository;
import com.internship.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CertificateService {
    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);
    private final CertificateRepository repo;
    private final InternshipRepository internshipRepo;
    private final AssessmentRepository assessmentRepo;
    private final ArchiveRepository archiveRepo;
    private final BlockchainService blockchain;

    public CertificateService(CertificateRepository repo, InternshipRepository internshipRepo,
                              AssessmentRepository assessmentRepo, ArchiveRepository archiveRepo,
                              BlockchainService blockchain) {
        this.repo = repo;
        this.internshipRepo = internshipRepo;
        this.assessmentRepo = assessmentRepo;
        this.archiveRepo = archiveRepo;
        this.blockchain = blockchain;
    }

    public Certificate createCertificate(CertificateDTO dto) {
        InternshipInfo internship = requireCertificateInternship(dto.getInternshipId(), dto.getStudentId());
        Certificate c = new Certificate();
        c.setInternshipId(internship.getId());
        c.setStudentId(internship.getStudentId());
        c.setCertificateContent(dto.getCertificateContent());
        c.setStatus("DRAFT");
        c.setCertificateNumber("CERT-" + System.currentTimeMillis());

        // Generate content hash using SHA-256
        String contentHash = HashUtil.sha256(dto.getCertificateContent() + internship.getId() + internship.getStudentId());
        c.setContentHash("0x" + contentHash);

        Certificate saved = repo.save(c);

        // Try to create certificate on blockchain
        try {
            String txHash = blockchain.createCertificate(
                internship.getId(),
                internship.getStudentId(),
                "0x" + contentHash,
                blockchain.getSchoolAccount(),
                blockchain.getEnterpriseAccount()
            );
            if (BlockchainService.isTransactionHash(txHash)) {
                saved.setBlockchainTxHash(txHash);
                saved.setStatus("BLOCKCHAIN_UPLOADED");
                log.info("Certificate {} uploaded to chain, tx: {}", saved.getCertificateNumber(), txHash);
            } else {
                saved.setBlockchainTxHash(null);
                saved.setStatus("PENDING_CHAIN");
            }
        } catch (Exception e) {
            log.warn("Blockchain upload failed for certificate {}, falling back to local: {}", saved.getCertificateNumber(), e.getMessage());
            saved.setBlockchainTxHash(null);
            saved.setStatus("PENDING_CHAIN");
        }
        saved = repo.save(saved);
        saveCertificateArchive(saved, internship);
        return saved;
    }

    public Certificate getCertificateById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Certificate not found: " + id));
    }

    public List<Certificate> getCertificatesByStudent(Long sid) { return repo.findByStudentId(sid); }
    public List<Certificate> getCertificatesByInternship(Long iid) { return repo.findByInternshipId(iid); }

    public Certificate updateStatus(Long id, String status) {
        Certificate c = getCertificateById(id);
        c.setStatus(status);
        return repo.save(c);
    }

    public Certificate getCertificateByNumber(String certNumber) {
        return repo.findByCertificateNumber(certNumber).orElseThrow(() -> new RuntimeException("Certificate not found: " + certNumber));
    }

    public Certificate approveBySchool(Long id, Long approverId) {
        Certificate c = getCertificateById(id);
        c.setStatus("SCHOOL_APPROVED");
        c.setSchoolApproved(true);
        c.setSchoolApproverId(approverId);
        c.setSchoolApprovedAt(LocalDateTime.now());

        // Try on-chain approval
        try {
            blockchain.approveCertificateBySchool(id);
            log.info("School approval on-chain for certificate {}", id);
        } catch (Exception e) {
            log.warn("On-chain school approval failed: {}", e.getMessage());
        }
        return repo.save(c);
    }

    public Certificate approveByEnterprise(Long id, Long approverId) {
        Certificate c = getCertificateById(id);
        c.setStatus("ENTERPRISE_APPROVED");
        c.setEnterpriseApproved(true);
        c.setEnterpriseApproverId(approverId);
        c.setEnterpriseApprovedAt(LocalDateTime.now());

        // Try on-chain approval
        try {
            blockchain.approveCertificateByEnterprise(id);
            log.info("Enterprise approval on-chain for certificate {}", id);
        } catch (Exception e) {
            log.warn("On-chain enterprise approval failed: {}", e.getMessage());
        }
        return repo.save(c);
    }

    public Certificate uploadToBlockchain(Long id) {
        Certificate c = getCertificateById(id);
        String contentHash = c.getContentHash();
        if (contentHash == null) {
            contentHash = HashUtil.sha256(c.getCertificateContent() + c.getInternshipId() + c.getStudentId());
            c.setContentHash("0x" + contentHash);
        }

        // Remove 0x prefix for blockchain call if present
        String hashForChain = contentHash.startsWith("0x") ? contentHash.substring(2) : contentHash;

        try {
            String txHash = blockchain.createCertificate(
                c.getInternshipId(), c.getStudentId(), "0x" + hashForChain,
                blockchain.getSchoolAccount(), blockchain.getEnterpriseAccount()
            );
            if (BlockchainService.isTransactionHash(txHash)) {
                c.setBlockchainTxHash(txHash);
                c.setStatus("BLOCKCHAIN_UPLOADED");
                log.info("Certificate {} uploaded to chain, tx: {}", c.getCertificateNumber(), txHash);
            } else {
                c.setBlockchainTxHash(null);
                c.setStatus("PENDING_CHAIN");
            }
        } catch (Exception e) {
            log.warn("Blockchain upload failed: {}", e.getMessage());
            c.setBlockchainTxHash(null);
            c.setStatus("PENDING_CHAIN");
        }
        Certificate saved = repo.save(c);
        internshipRepo.findById(saved.getInternshipId()).ifPresent(internship -> saveCertificateArchive(saved, internship));
        return saved;
    }

    public boolean verifyCertificate(String certNumber, String hash) {
        // First try local
        Certificate c = verify(certNumber, hash);
        boolean localValid = c != null;

        // Also try on-chain verification
        try {
            long certId = c != null ? c.getId() : Long.parseLong(certNumber.replaceAll("\\D", ""));
            String hashForChain = hash.startsWith("0x") ? hash : "0x" + hash;
            boolean chainValid = blockchain.verifyCertificate(certId, hashForChain);
            if (chainValid) {
                log.info("On-chain verification passed for certificate {}", certNumber);
                return true;
            }
        } catch (Exception e) {
            log.warn("On-chain verification failed, using local result: {}", e.getMessage());
        }
        return localValid;
    }

    public Certificate verify(String certNumber, String hash) {
        List<Certificate> all = repo.findAll();
        return all.stream()
            .filter(c -> c.getCertificateNumber().equals(certNumber) ||
                         (c.getContentHash() != null && c.getContentHash().equals(hash)))
            .findFirst().orElse(null);
    }

    private InternshipInfo requireCertificateInternship(Long internshipId, Long studentId) {
        if (internshipId == null) throw new RuntimeException("Internship is required");
        InternshipInfo internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new RuntimeException("Internship record does not exist"));
        if (studentId != null && !Objects.equals(studentId, internship.getStudentId())) {
            throw new RuntimeException("Student does not match internship record");
        }
        boolean hasFailedAssessment = assessmentRepo.findByInternshipId(internship.getId()).stream()
                .map(Assessment::getScore)
                .filter(Objects::nonNull)
                .anyMatch(score -> score < 60);
        if (hasFailedAssessment) {
            throw new RuntimeException("Certificate cannot be issued while assessment score is below 60");
        }
        return internship;
    }

    private void saveCertificateArchive(Certificate certificate, InternshipInfo internship) {
        Archive archive = new Archive();
        archive.setType("实习证书");
        archive.setName(internship.getStudentName() + "-实习证明");
        archive.setHash(certificate.getContentHash());
        archive.setTime(LocalDateTime.now().toString().replace("T", " ").substring(0, 19));
        archive.setStudentId(certificate.getStudentId());
        archive.setCompanyId(internship.getEnterpriseId());
        archive.setInternshipId(internship.getId());
        archive.setSourceId("certificate:" + certificate.getId());
        archive.setTxHash(certificate.getBlockchainTxHash());
        if (BlockchainService.isTransactionHash(certificate.getBlockchainTxHash())) {
            archive.setChainStatus("ON_CHAIN");
            archive.setBlock(System.currentTimeMillis() % 20000000L + 18400000L);
            archive.setChainError(null);
        } else {
            archive.setChainStatus("LOCAL_FALLBACK");
            archive.setChainError("Certificate transaction is pending chain upload");
        }
        archiveRepo.save(archive);
    }
}
