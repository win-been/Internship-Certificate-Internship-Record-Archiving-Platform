package com.internship.dto;
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Long schoolId;
    private String organizationName;
    private String major;
    private String identityStatus;

    public String getToken(){return token;} public void setToken(String v){token=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getRealName(){return realName;} public void setRealName(String v){realName=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public Long getSchoolId(){return schoolId;} public void setSchoolId(Long v){schoolId=v;}
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getMajor(){return major;} public void setMajor(String v){major=v;}
    public String getIdentityStatus(){return identityStatus;} public void setIdentityStatus(String v){identityStatus=v;}

    public static LoginResponse of(String t, Long id, String u, String rn, String r, Long sid, String on, String major, String identityStatus) {
        LoginResponse lr = new LoginResponse();
        lr.token = t; lr.userId = id; lr.username = u;
        lr.realName = rn; lr.role = r;
        lr.schoolId = sid; lr.organizationName = on;
        lr.major = major; lr.identityStatus = identityStatus;
        return lr;
    }
}
