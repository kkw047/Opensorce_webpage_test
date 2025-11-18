package com.cbnu11team.team11.domain;

import jakarta.persistence.*; // JPA 어노테이션을 위해 import
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate; // '날짜' 정보만 저장하기 위해 사용

@Entity // 1. JPA에게 이 클래스가 데이터베이스 테이블과 매핑됨을 알림
@Getter @Setter // 2. Lombok: Get/Set 메소드를 자동으로 생성해 코드를 깔끔하게 함
public class ClubSchedule {

    @Id // 3. 이 필드가 테이블의 기본 키(Primary Key)임을 알림
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. DB가 ID를 자동으로 생성(증가)하도록 함
    @Column(name = "club_schedule_id") // DB 테이블의 컬럼 이름을 명시 (선택 사항)
    private Long id;

    // 5. '예정 내역' (예: "10월 정기 회식")을 저장할 컬럼
    private String title;

    // 6. '상세 설명' (선택 사항)
    @Column(length = 1000) // 텍스트 길이를 좀 더 길게 설정 (선택 사항)
    private String details;

    // 7. "일자로 쭈욱" 표시를 위한 '시작 날짜'
    private LocalDate startDate;

    // 8. "일자로 쭈욱" 표시를 위한 '종료 날짜'
    private LocalDate endDate;

    // 9. '참여비' (예: "10000원", "무료", "1/N" 등 유연하게 받기 위해 String 추천)
    private String fee;

    // 10. 이 일정이 '어떤 모임'에 속해있는지 (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY) // 11. (성능 최적화: 필요할 때만 Club 정보 로드)
    @JoinColumn(name = "club_id") // 12. DB에서 'club_id'라는 이름의 외래 키(Foreign Key)로 연결
    private Club club;

    // 13. 이 일정을 '누가 제안'했는지 (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id") // 'user_id' 또는 'creator_id' 등 원하는 이름 사용
    private User creator;

    // 14. (참가자 목록: 이 Entity에서는 직접 관리하지 않고, ScheduleParticipant가 관리)
    // @OneToMany(mappedBy = "clubSchedule")
    // private List<ScheduleParticipant> participants = new ArrayList<>();
}