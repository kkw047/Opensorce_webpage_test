package com.cbnu11team.team11.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank
    private String loginId;

    @NotBlank
    private String password;
}
