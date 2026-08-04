package com.internship.entity;
import jakarta.persistence.*;
@Entity @Table(name = "assessments")
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long internshipId;
    @Column(length = 50) private Long studentId;
    private Long companyId;
    @Column(length = 50) private String student;
    @Column(length = 20) private String month;
    @Column(length = 50) private String attendance;
    private Integer score;
    @Column(length = 1000) private String comment;
    @Column(length = 20) private String status;
    public Assessment(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getInternshipId(){return internshipId;} public void setInternshipId(Long v){internshipId=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getCompanyId(){return companyId;} public void setCompanyId(Long v){companyId=v;}
    public String getStudent(){return student;} public void setStudent(String v){student=v;}
    public String getMonth(){return month;} public void setMonth(String v){month=v;}
    public String getAttendance(){return attendance;} public void setAttendance(String v){attendance=v;}
    public Integer getScore(){return score;} public void setScore(Integer v){score=v;}
    public String getComment(){return comment;} public void setComment(String v){comment=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}

