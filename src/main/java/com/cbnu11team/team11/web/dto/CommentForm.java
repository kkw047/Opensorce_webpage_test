package com.cbnu11team.team11.web.dto;

import jakarta.validation.constraints.NotEmpty;

/**
 * 댓글 작성/수정 폼을 위한 DTO (Data Transfer Object)
 * @param content 댓글 내용
 */
public record CommentForm(
        @NotEmpty(message = "댓글 내용을 입력해주세요.")
        String content
) {
}