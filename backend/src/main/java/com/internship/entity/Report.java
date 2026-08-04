package com.internship.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reports")
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long internshipId;
    private Long studentId;
    private Long enterpriseId;
    @Column(length = 80) private String studentName;
    @Column(length = 160) private String enterpriseName;
    @Column(length = 200) private String title;
    @Column(length = 2000) private String content;
    @Column(length = 30) private String date;
    private Double hours;
    private Boolean submitted;

    public Report(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getInternshipId(){return internshipId;} public void setInternshipId(Long v){internshipId=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getEnterpriseId(){return enterpriseId;} public void setEnterpriseId(Long v){enterpriseId=v;}
    public String getStudentName(){return studentName;} public void setStudentName(String v){studentName=v;}
    public String getEnterpriseName(){return enterpriseName;} public void setEnterpriseName(String v){enterpriseName=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public Double getHours(){return hours;} public void setHours(Double v){hours=v;}
    public Boolean getSubmitted(){return submitted;} public void setSubmitted(Boolean v){submitted=v;}
}
