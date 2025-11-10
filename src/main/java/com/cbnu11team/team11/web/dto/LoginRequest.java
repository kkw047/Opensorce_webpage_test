package com.cbnu11team.team11.web.dto;

public class LoginRequest {
    // 폼에서 name="username"을 그대로 받아도 됨
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // 서비스/리포지토리는 loginId로 처리하므로 헬퍼 추가
    public String getLoginId() { return username; }
}
