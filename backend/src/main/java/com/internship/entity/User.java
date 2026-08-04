package com.internship.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    @Column(nullable = false, length = 200)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @Column(unique = true, length = 100)
    private String email;
    @Column(length = 50)
    private String realName;
    private Long schoolId;
    @Column(length = 30)
    private String role;
    @Column(length = 200)
    private String organizationName;
    @Column(length = 100)
    private String major;
    @Column(length = 50)
    private String organizationCode;
    @Column(length = 20)
    private String phone;
    @Column(length = 20)
    private String idCard;
    @Column(length = 100)
    private String walletAddress;
    @Column(length = 200)
    @JsonIgnore
    private String privateKey;
    private Boolean enabled;
    @Column(length = 20)
    private String status;
    @Column(length = 20)
    private String identityStatus;
    private Boolean approved;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    @Column(length = 500)
    private String rejectReason;

    public User(){}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getRealName(){return realName;} public void setRealName(String v){realName=v;}
    public Long getSchoolId(){return schoolId;} public void setSchoolId(Long v){schoolId=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getMajor(){return major;} public void setMajor(String v){major=v;}
    public String getOrganizationCode(){return organizationCode;} public void setOrganizationCode(String v){organizationCode=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getIdCard(){return idCard;} public void setIdCard(String v){idCard=v;}
    public String getWalletAddress(){return walletAddress;} public void setWalletAddress(String v){walletAddress=v;}
    public String getPrivateKey(){return privateKey;} public void setPrivateKey(String v){privateKey=v;}
    public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getIdentityStatus(){return identityStatus;} public void setIdentityStatus(String v){identityStatus=v;}
    public Boolean getApproved(){return approved;} public void setApproved(Boolean v){approved=v;}
    public Long getApprovedBy(){return approvedBy;} public void setApprovedBy(Long v){approvedBy=v;}
    public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime v){approvedAt=v;}
    public String getRejectReason(){return rejectReason;} public void setRejectReason(String v){rejectReason=v;}
}
