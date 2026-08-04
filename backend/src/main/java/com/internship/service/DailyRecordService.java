package com.internship.service;

import com.internship.dto.DailyRecordDTO;
import com.internship.entity.DailyRecord;
import com.internship.entity.InternshipInfo;
import com.internship.repository.DailyRecordRepository;
import com.internship.repository.InternshipRepository;
import com.internship.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DailyRecordService {
    private static final Logger log = LoggerFactory.getLogger(DailyRecordService.class);
    private final DailyRecordRepository repo;
    private final InternshipRepository internshipRepo;
    private final BlockchainService blockchain;

    public DailyRecordService(DailyRecordRepository repo, InternshipRepository internshipRepo, BlockchainService blockchain) {
        this.repo = repo;
        this.internshipRepo = internshipRepo;
        this.blockchain = blockchain;
    }

    public DailyRecord createRecord(DailyRecordDTO dto) {
        InternshipInfo internship = internshipRepo.findById(dto.getInternshipId())
                .orElseThrow(() -> new RuntimeException("Internship record does not exist"));
        if (!Objects.equals(internship.getStudentId(), dto.getStudentId())) {
            throw new RuntimeException("Daily report must belong to the selected internship student");
        }
        if (!"ACTIVE".equals(internship.getStatus())) {
            throw new RuntimeException("Daily report can only be submitted for an active internship");
        }
        DailyRecord r = new DailyRecord();
        r.setInternshipId(dto.getInternshipId()); r.setStudentId(dto.getStudentId());
        r.setRecordDate(dto.getRecordDate()); r.setContent(dto.getContent());
        r.setWorkHours(dto.getWorkHours()); r.setTaskCompleted(dto.getTaskCompleted());
        r.setLearningPoints(dto.getLearningPoints()); r.setStatus("DRAFT");

        String contentHash = "0x" + HashUtil.sha256(dto.getContent() + dto.getInternshipId() + dto.getStudentId());
        String recordDateHash = "0x" + HashUtil.sha256(String.valueOf(dto.getRecordDate()) + dto.getInternshipId());
        r.setContentHash(contentHash);
        // recordIndex mirrors the append order in the on-chain contract for this internship
        long index = dto.getInternshipId() != null ? repo.findByInternshipId(dto.getInternshipId()).size() : 0L;
        r.setRecordIndex(index);

        DailyRecord saved = repo.save(r);

        // Anchor the daily record on chain
        try {
            String txHash = blockchain.createRecord(
                dto.getInternshipId() != null ? dto.getInternshipId() : 0L,
                dto.getStudentId() != null ? dto.getStudentId() : 0L,
                recordDateHash, contentHash,
                blockchain.getSchoolAccount(), blockchain.getEnterpriseAccount());
            saved.setBlockchainTxHash(txHash);
            saved.setStatus("BLOCKCHAIN_UPLOADED");
            log.info("Daily record {} uploaded to chain, tx: {}", saved.getId(), txHash);
        } catch (Exception e) {
            log.warn("Blockchain upload failed for daily record {}: {}", saved.getId(), e.getMessage());
            saved.setBlockchainTxHash(null);
            saved.setStatus("PENDING_CHAIN");
        }
        return repo.save(saved);
    }

    public DailyRecord getRecordById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
    }

    public List<DailyRecord> getRecordsByStudent(Long sid) { return repo.findByStudentId(sid); }
    public List<DailyRecord> getRecordsByInternship(Long iid) { return repo.findByInternshipId(iid); }

    public DailyRecord updateStatus(Long id, String status) {
        DailyRecord r = getRecordById(id);
        r.setStatus(status);
        return repo.save(r);
    }

    public void deleteRecord(Long id) {
        repo.deleteById(id);
    }

    public List<DailyRecord> getRecordsByDateRange(Long internshipId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return repo.findByInternshipIdAndRecordDateBetween(internshipId, startDate, endDate);
    }

    public DailyRecord updateRecord(Long id, DailyRecordDTO dto) {
        DailyRecord r = getRecordById(id);
        if (dto.getContent() != null) r.setContent(dto.getContent());
        if (dto.getWorkHours() != null) r.setWorkHours(dto.getWorkHours());
        if (dto.getTaskCompleted() != null) r.setTaskCompleted(dto.getTaskCompleted());
        if (dto.getLearningPoints() != null) r.setLearningPoints(dto.getLearningPoints());
        return repo.save(r);
    }

    public DailyRecord reviewRecord(Long id, Long mentorId, String comment, boolean approved) {
        DailyRecord r = getRecordById(id);
        r.setMentorReviewId(mentorId);
        r.setMentorComment(comment);
        r.setStatus(approved ? "APPROVED" : "REJECTED");

        // Reflect the review decision on chain
        try {
            long internshipId = r.getInternshipId() != null ? r.getInternshipId() : 0L;
            long recordIndex = r.getRecordIndex() != null ? r.getRecordIndex() : 0L;
            String txHash = approved
                ? blockchain.approveRecord(internshipId, recordIndex)
                : blockchain.rejectRecord(internshipId, recordIndex);
            log.info("Daily record {} review ({}) on chain, tx: {}", id, approved ? "approved" : "rejected", txHash);
        } catch (Exception e) {
            log.warn("On-chain review failed for daily record {}: {}", id, e.getMessage());
        }
        return repo.save(r);
    }
}
