package com.cbnu11team.team11.web.dto;

import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {
    private String loginId;
    private String password;
    private String email;
    private String nickname;
    private List<Long> categoryIds;
}
