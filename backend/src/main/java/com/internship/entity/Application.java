package com.internship.entity;
import jakarta.persistence.*;
@Entity @Table(name = "applications")
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long studentId;
    private Long companyId;
    @Column(length = 50) private String name;
    @Column(length = 100) private String school;
    @Column(length = 100) private String major;
    private Long jobId;
    @Column(length = 200) private String jobTitle;
    @Column(length = 30) private String applyDate;
    @Column(length = 20) private String status;
    public Application(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getCompanyId(){return companyId;} public void setCompanyId(Long v){companyId=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getSchool(){return school;} public void setSchool(String v){school=v;}
    public String getMajor(){return major;} public void setMajor(String v){major=v;}
    public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
    public String getJobTitle(){return jobTitle;} public void setJobTitle(String v){jobTitle=v;}
    public String getApplyDate(){return applyDate;} public void setApplyDate(String v){applyDate=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}