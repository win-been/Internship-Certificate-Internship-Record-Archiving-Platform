package com.internship.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long internshipId;
    private Long studentId;
    @Column(length = 50)
    private String certificateNumber;
    @Column(length = 4000)
    private String certificateContent;
    @Column(length = 100)
    private String contentHash;
    @Column(length = 100)
    private String blockchainTxHash;
    private Long blockchainCertificateId;
    @Column(length = 20)
    private String status;
    private Boolean schoolApproved;
    private Boolean enterpriseApproved;
    private Long schoolApproverId;
    private Long enterpriseApproverId;
    private LocalDateTime schoolApprovedAt;
    private LocalDateTime enterpriseApprovedAt;

    public Certificate(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getInternshipId(){return internshipId;} public void setInternshipId(Long v){internshipId=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public String getCertificateNumber(){return certificateNumber;} public void setCertificateNumber(String v){certificateNumber=v;}
    public String getCertificateContent(){return certificateContent;} public void setCertificateContent(String v){certificateContent=v;}
    public String getContentHash(){return contentHash;} public void setContentHash(String v){contentHash=v;}
    public String getBlockchainTxHash(){return blockchainTxHash;} public void setBlockchainTxHash(String v){blockchainTxHash=v;}
    public Long getBlockchainCertificateId(){return blockchainCertificateId;} public void setBlockchainCertificateId(Long v){blockchainCertificateId=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Boolean getSchoolApproved(){return schoolApproved;} public void setSchoolApproved(Boolean v){schoolApproved=v;}
    public Boolean getEnterpriseApproved(){return enterpriseApproved;} public void setEnterpriseApproved(Boolean v){enterpriseApproved=v;}
    public Long getSchoolApproverId(){return schoolApproverId;} public void setSchoolApproverId(Long v){schoolApproverId=v;}
    public Long getEnterpriseApproverId(){return enterpriseApproverId;} public void setEnterpriseApproverId(Long v){enterpriseApproverId=v;}
    public LocalDateTime getSchoolApprovedAt(){return schoolApprovedAt;} public void setSchoolApprovedAt(LocalDateTime v){schoolApprovedAt=v;}
    public LocalDateTime getEnterpriseApprovedAt(){return enterpriseApprovedAt;} public void setEnterpriseApprovedAt(LocalDateTime v){enterpriseApprovedAt=v;}
}
