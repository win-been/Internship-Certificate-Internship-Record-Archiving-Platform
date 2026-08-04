package com.internship.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_records")
public class DailyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long internshipId;
    private Long studentId;
    private LocalDate recordDate;
    @Column(length = 2000)
    private String content;
    @Column(length = 100)
    private String contentHash;
    @Column(length = 100)
    private String blockchainTxHash;
    private Long recordIndex;
    private Double workHours;
    @Column(length = 500)
    private String taskCompleted;
    @Column(length = 500)
    private String learningPoints;
    @Column(length = 500)
    private String attachmentUrl;
    @Column(length = 20)
    private String status;
    private Long mentorReviewId;
    @Column(length = 500)
    private String mentorComment;

    public DailyRecord(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getInternshipId(){return internshipId;} public void setInternshipId(Long v){internshipId=v;}
    public Long getStudentId(){return studentId;} public void setStudentId(Long v){studentId=v;}
    public LocalDate getRecordDate(){return recordDate;} public void setRecordDate(LocalDate v){recordDate=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getContentHash(){return contentHash;} public void setContentHash(String v){contentHash=v;}
    public String getBlockchainTxHash(){return blockchainTxHash;} public void setBlockchainTxHash(String v){blockchainTxHash=v;}
    public Long getRecordIndex(){return recordIndex;} public void setRecordIndex(Long v){recordIndex=v;}
    public Double getWorkHours(){return workHours;} public void setWorkHours(Double v){workHours=v;}
    public String getTaskCompleted(){return taskCompleted;} public void setTaskCompleted(String v){taskCompleted=v;}
    public String getLearningPoints(){return learningPoints;} public void setLearningPoints(String v){learningPoints=v;}
    public String getAttachmentUrl(){return attachmentUrl;} public void setAttachmentUrl(String v){attachmentUrl=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Long getMentorReviewId(){return mentorReviewId;} public void setMentorReviewId(Long v){mentorReviewId=v;}
    public String getMentorComment(){return mentorComment;} public void setMentorComment(String v){mentorComment=v;}
}
