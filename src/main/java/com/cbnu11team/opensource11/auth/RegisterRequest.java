package com.cbnu11team.opensource11.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank @Email
    private String username;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    private String name;
}
