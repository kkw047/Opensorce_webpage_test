package com.cbnu11team.team11.web.dto;

import java.util.List;

public record RegisterRequest(
        String loginId,
        String email,
        String password,
        String nickname,
        String regionDo,
        String regionSi,
        List<Long> categoryIds
) {}
