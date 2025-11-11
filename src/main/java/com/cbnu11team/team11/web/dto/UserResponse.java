package com.cbnu11team.team11.web.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String loginId;
    private String email;
    private String nickname;
}
