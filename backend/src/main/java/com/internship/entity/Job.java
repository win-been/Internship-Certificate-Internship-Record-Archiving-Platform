package com.internship.entity;
import jakarta.persistence.*;
@Entity @Table(name = "jobs")
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 200) private String title;
    private Long companyId;
    @Column(length = 200) private String company;
    @Column(length = 50) private String location;
    @Column(length = 100) private String salary;
    @Column(length = 50) private String type;
    @Column(length = 1000) private String description;
    @Column(length = 20) private String status;
    private Integer count = 0;
    public Job(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public Long getCompanyId(){return companyId;} public void setCompanyId(Long v){companyId=v;}
    public String getCompany(){return company;} public void setCompany(String v){company=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getSalary(){return salary;} public void setSalary(String v){salary=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Integer getCount(){return count;} public void setCount(Integer v){count=v;}
}