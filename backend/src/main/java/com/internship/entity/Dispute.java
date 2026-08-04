package com.internship.entity;
import jakarta.persistence.*;
@Entity @Table(name = "disputes")
public class Dispute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long assessmentId;
    private Long studentId;
    private Long companyId;
    @Column(length = 50) private String student;
    @Column(length = 1000) private String reason;
    @Column(length = 30) private String date;
    @Column(length = 20) private String status;
    @Column(length = 1000) private String opinion;
    @Column(length = 30) private String resolvedDate;
    public Dispute(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getAssessmentId(){return assessmentId;} public void setAssessmentId(Long v){assessmentId=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getCompanyId(){return companyId;} public void setCompanyId(Long v){companyId=v;}
    public String getStudent(){return student;} public void setStudent(String v){student=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getOpinion(){return opinion;} public void setOpinion(String v){opinion=v;}
    public String getResolvedDate(){return resolvedDate;} public void setResolvedDate(String v){resolvedDate=v;}
}

