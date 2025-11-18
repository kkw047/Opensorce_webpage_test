package com.cbnu11team.team11.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostForm(

        @NotBlank(message = "제목은 필수 입력 사항입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @NotBlank(message = "내용은 필수 입력 사항입니다.")
        String content
) { }