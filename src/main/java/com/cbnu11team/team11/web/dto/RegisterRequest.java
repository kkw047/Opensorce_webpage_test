package com.cbnu11team.team11.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterRequest {
    /** 프로젝트에 따라 username 대신 loginId 를 쓰는 경우가 있어 둘 다 지원 */
    private String username;   // 선호하는 이름
    private String loginId;    // 기존 코드에서 쓰던 이름(있을 수도, 없을 수도)

    private String password;
    private String email;

    /** 가입 시 선호 카테고리(체크박스) 선택값이 있다면 사용 */
    private List<Long> preferredCategoryIds;

    /** 컨트롤러에서 form.getUsername()을 호출해도 항상 값이 나오도록 폴백 제공 */
    public String getUsername() {
        if (username != null && !username.isBlank()) return username;
        return loginId; // loginId 로만 들어오는 폼도 지원
    }
}
