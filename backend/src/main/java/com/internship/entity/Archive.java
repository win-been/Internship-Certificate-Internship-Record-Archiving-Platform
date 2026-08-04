package com.internship.entity;
import jakarta.persistence.*;
@Entity @Table(name = "archives")
public class Archive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 50) private String type;
    @Column(length = 200) private String name;
    @Column(length = 100) private String hash;
    @Column(length = 50) private String time;
    private Long studentId;
    private Long companyId;
    private Long internshipId;
    @Column(length = 100) private String sourceId;
    private Long block;
    @Column(length = 30) private String chainStatus;
    @Column(length = 100) private String txHash;
    @Column(length = 1000) private String chainError;
    public Archive(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getHash(){return hash;} public void setHash(String v){hash=v;}
    public String getTime(){return time;} public void setTime(String v){time=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public Long getCompanyId(){return companyId;} public void setCompanyId(Long v){companyId=v;}
    public Long getInternshipId(){return internshipId;} public void setInternshipId(Long v){internshipId=v;}
    public String getSourceId(){return sourceId;} public void setSourceId(String v){sourceId=v;}
    public Long getBlock(){return block;} public void setBlock(Long v){block=v;}
    public String getChainStatus(){return chainStatus;} public void setChainStatus(String v){chainStatus=v;}
    public String getTxHash(){return txHash;} public void setTxHash(String v){txHash=v;}
    public String getChainError(){return chainError;} public void setChainError(String v){chainError=v;}
}

