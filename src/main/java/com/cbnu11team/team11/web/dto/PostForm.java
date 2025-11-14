package com.cbnu11team.team11.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시물 생성 폼(Form)에서 데이터를 받아오는 DTO (Data Transfer Object)
 * Java 14 이상에서 제공하는 record 타입을 사용. (UserResponse 등과 동일)
 */
public record PostForm(

        /**
         * @NotBlank: null, "", " " (공백만 있는 문자열)을 모두 허용하지 않음.
         * @Size(max=100): 최대 길이를 100자로 제한 (Post 엔티티의 title 길이와 일치).
         */
        @NotBlank(message = "제목은 필수 입력 사항입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        /**
         * @NotBlank: null, "", " " (공백만 있는 문자열)을 모두 허용하지 않음.
         */
        @NotBlank(message = "내용은 필수 입력 사항입니다.")
        String content
) {
        // record는 별도의 getter, setter, 생성자가 필요 없습니다.
}