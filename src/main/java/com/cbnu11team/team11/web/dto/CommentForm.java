package com.cbnu11team.team11.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CommentForm(
        @NotEmpty(message = "댓글 내용을 입력해주세요.")
        @Size(max = 300, message = "댓글은 300자를 넘을 수 없습니다.")
        String content
) {
}