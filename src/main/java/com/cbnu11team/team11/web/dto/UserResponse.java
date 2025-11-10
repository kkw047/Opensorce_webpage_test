package com.cbnu11team.team11.web.dto;

import java.util.List;

public class UserResponse {
    public Long id;
    public String email;
    public String nickname;
    public String regionDo;
    public String regionSi;
    public List<String> categories;

    public UserResponse(Long id, String email, String nickname,
                        String regionDo, String regionSi, List<String> categories) {
        this.id = id; this.email = email; this.nickname = nickname;
        this.regionDo = regionDo; this.regionSi = regionSi; this.categories = categories;
    }
}
