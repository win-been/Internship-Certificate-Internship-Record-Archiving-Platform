package com.internship.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "internships")
public class InternshipInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentIdCard;
    private Long enterpriseId;
    private String enterpriseName;
    private String enterpriseCode;
    private Long schoolId;
    private String schoolName;
    @Column(length = 100)
    private String position;
    private LocalDate startDate;
    private LocalDate endDate;
    private String department;
    private String mentorName;
    private String mentorPhone;
    @Column(length = 500)
    private String description;
    @Column(length = 20)
    private String status;
    @Column(length = 100)
    private String contentHash;
    @Column(length = 100)
    private String blockchainTxHash;
    private Boolean agreementSigned;
    @Column(length = 30)
    private String agreementSignedAt;

    public InternshipInfo(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public String getStudentName(){return studentName;} public void setStudentName(String v){studentName=v;}
    public String getStudentIdCard(){return studentIdCard;} public void setStudentIdCard(String v){studentIdCard=v;}
    public Long getEnterpriseId(){return enterpriseId;} public void setEnterpriseId(Long v){enterpriseId=v;}
    public String getEnterpriseName(){return enterpriseName;} public void setEnterpriseName(String v){enterpriseName=v;}
    public String getEnterpriseCode(){return enterpriseCode;} public void setEnterpriseCode(String v){enterpriseCode=v;}
    public Long getSchoolId(){return schoolId;} public void setSchoolId(Long v){schoolId=v;}
    public String getSchoolName(){return schoolName;} public void setSchoolName(String v){schoolName=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    public String getDepartment(){return department;} public void setDepartment(String v){department=v;}
    public String getMentorName(){return mentorName;} public void setMentorName(String v){mentorName=v;}
    public String getMentorPhone(){return mentorPhone;} public void setMentorPhone(String v){mentorPhone=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getContentHash(){return contentHash;} public void setContentHash(String v){contentHash=v;}
    public String getBlockchainTxHash(){return blockchainTxHash;} public void setBlockchainTxHash(String v){blockchainTxHash=v;}
    public Boolean getAgreementSigned(){return agreementSigned;} public void setAgreementSigned(Boolean v){agreementSigned=v;}
    public String getAgreementSignedAt(){return agreementSignedAt;} public void setAgreementSignedAt(String v){agreementSignedAt=v;}
}
