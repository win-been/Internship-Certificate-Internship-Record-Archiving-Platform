package com.internship.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class PlatformMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 200) private String title;
    @Column(length = 2000) private String content;
    @Column(length = 80) private String fromName;
    @Column(length = 30) private String date;
    private Long schoolId;
    private Boolean readFlag;

    public PlatformMessage(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getFromName(){return fromName;} public void setFromName(String v){fromName=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public Long getSchoolId(){return schoolId;} public void setSchoolId(Long v){schoolId=v;}
    public Boolean getReadFlag(){return readFlag;} public void setReadFlag(Boolean v){readFlag=v;}
}
