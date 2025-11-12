package com.cbnu11team.team11.web.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String loginId,
        String email,
        String nickname,
        String regionDo,
        String regionSi,
        List<String> categories
) {}
