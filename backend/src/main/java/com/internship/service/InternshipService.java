package com.internship.service;

import com.internship.dto.InternshipInfoDTO;
import com.internship.entity.Application;
import com.internship.entity.InternshipInfo;
import com.internship.repository.ApplicationRepository;
import com.internship.repository.InternshipRepository;
import com.internship.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class InternshipService {
    private static final Logger log = LoggerFactory.getLogger(InternshipService.class);
    private final InternshipRepository repo;
    private final ApplicationRepository applicationRepo;
    private final BlockchainService blockchain;

    public InternshipService(InternshipRepository repo, ApplicationRepository applicationRepo, BlockchainService blockchain) {
        this.repo = repo;
        this.applicationRepo = applicationRepo;
        this.blockchain = blockchain;
    }

    public InternshipInfo registerInternship(InternshipInfoDTO dto) {
        Application accepted = applicationRepo
                .findByStudentIdAndCompanyIdAndStatus(dto.getStudentId(), dto.getEnterpriseId(), "accepted")
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enterprise has not confirmed this student as hired"));
        repo.findByStudentIdAndEnterpriseId(dto.getStudentId(), dto.getEnterpriseId()).stream()
                .filter(prev -> "ACTIVE".equals(prev.getStatus()))
                .findFirst()
                .ifPresent(prev -> {
                    throw new RuntimeException("This student already has an active internship record in this enterprise");
                });
        InternshipInfo i = new InternshipInfo();
        i.setStudentId(dto.getStudentId()); i.setStudentName(dto.getStudentName());
        i.setStudentIdCard(dto.getStudentIdCard()); i.setEnterpriseId(dto.getEnterpriseId());
        i.setEnterpriseName(dto.getEnterpriseName()); i.setEnterpriseCode(dto.getEnterpriseCode());
        i.setSchoolId(dto.getSchoolId()); i.setSchoolName(dto.getSchoolName());
        i.setPosition(dto.getPosition() != null ? dto.getPosition() : accepted.getJobTitle()); i.setStartDate(dto.getStartDate());
        i.setEndDate(dto.getEndDate()); i.setDepartment(dto.getDepartment());
        i.setMentorName(dto.getMentorName()); i.setMentorPhone(dto.getMentorPhone());
        i.setDescription(dto.getDescription()); i.setStatus("ACTIVE");

        String contentHash = "0x" + HashUtil.sha256(
            dto.getStudentName() + "|" + dto.getEnterpriseName() + "|" + dto.getPosition()
            + "|" + dto.getStartDate() + "|" + dto.getEndDate());
        i.setContentHash(contentHash);

        InternshipInfo saved = repo.save(i);

        // Anchor the internship registration on chain
        try {
            String txHash = blockchain.createCertificate(
                saved.getId(),
                dto.getStudentId() != null ? dto.getStudentId() : 0L,
                contentHash,
                blockchain.getSchoolAccount(), blockchain.getEnterpriseAccount());
            saved.setBlockchainTxHash(txHash);
            log.info("Internship {} anchored on chain, tx: {}", saved.getId(), txHash);
        } catch (Exception e) {
            log.warn("Blockchain anchoring failed for internship {}: {}", saved.getId(), e.getMessage());
            saved.setBlockchainTxHash(null);
        }
        return repo.save(saved);
    }

    public InternshipInfo getInternshipById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
    }

    public List<InternshipInfo> getInternshipsByStudent(Long sid) { return repo.findByStudentId(sid); }
    public List<InternshipInfo> getInternshipsByEnterprise(Long eid) { return repo.findByEnterpriseId(eid); }
    public List<InternshipInfo> getInternshipsBySchool(Long sid) { return repo.findBySchoolId(sid); }

    public InternshipInfo updateInternshipStatus(Long id, String st) {
        InternshipInfo i = getInternshipById(id);
        i.setStatus(st);
        return repo.save(i);
    }

    public InternshipInfo updateInternship(Long id, InternshipInfoDTO dto) {
        InternshipInfo i = getInternshipById(id);
        i.setPosition(dto.getPosition()); i.setDepartment(dto.getDepartment());
        i.setMentorName(dto.getMentorName()); i.setMentorPhone(dto.getMentorPhone());
        i.setDescription(dto.getDescription());
        return repo.save(i);
    }
}
