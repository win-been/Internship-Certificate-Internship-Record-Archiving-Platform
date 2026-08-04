package com.internship.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "approvals")
public class Approval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    @Column(length = 80) private String type;
    @Column(length = 120) private String name;
    @Column(length = 80) private String code;
    @Column(length = 120) private String school;
    @Column(length = 100) private String major;
    @Column(length = 80) private String contact;
    @Column(length = 30) private String phone;
    @Column(length = 20) private String status;
    @Column(length = 30) private String date;

    public Approval(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getSchool(){return school;} public void setSchool(String v){school=v;}
    public String getMajor(){return major;} public void setMajor(String v){major=v;}
    public String getContact(){return contact;} public void setContact(String v){contact=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
}
