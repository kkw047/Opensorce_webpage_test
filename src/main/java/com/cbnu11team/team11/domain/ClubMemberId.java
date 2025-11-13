package com.cbnu11team.team11.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClubMemberId implements Serializable {

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "user_id")
    private Long userId;
}