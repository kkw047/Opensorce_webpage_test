package com.cbnu11team.opensource11.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name="uk_users_username", columnNames="username")
})
@Getter @Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Email
    @Column(nullable=false, length=120)
    private String username;           // 이메일(로그인 아이디)

    @NotBlank @Size(min=8, max=100)
    @Column(nullable=false, length=100)
    private String password;           // BCrypt 해시 저장

    @NotBlank
    @Column(nullable=false, length=50)
    private String name;               // 표시 이름

    @Column(nullable=false, length=20)
    private String role = "USER";      // USER / ADMIN

    @CreationTimestamp
    private LocalDateTime createdAt;
}
