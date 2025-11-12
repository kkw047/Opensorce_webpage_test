package com.cbnu11team.team11.web.dto;

public record LoginRequest(
        String loginIdOrEmail,
        String password
) {}
