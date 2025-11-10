package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id")
})
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 아이디(대소문자 구분 없이 중복 방지)
    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    // 비밀번호(BCrypt 해시 저장)
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    // 사용자가 선택한 카테고리 (선택 기능 사용할 경우)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_categories",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    public User() {}

    public User(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
    }

    public Long getId() { return id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }
}
