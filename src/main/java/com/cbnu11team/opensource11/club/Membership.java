package com.cbnu11team.opensource11.club;

import jakarta.persistence.*;

@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(name="uk_membership", columnNames = {"club_id","user_id"}))
public class Membership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="club_id", nullable=false)
    private Long clubId;

    @Column(name="user_id", nullable=false)
    private Long userId;

    // --- getter/setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
