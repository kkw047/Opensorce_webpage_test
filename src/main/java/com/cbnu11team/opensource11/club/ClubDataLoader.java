package com.cbnu11team.opensource11.club;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class ClubDataLoader implements CommandLineRunner {

    private final ClubRepository clubRepo;
    private final MembershipRepository memRepo;

    public ClubDataLoader(ClubRepository clubRepo, MembershipRepository memRepo) {
        this.clubRepo = clubRepo; this.memRepo = memRepo;
    }

    @Override
    public void run(String... args) {
        if (clubRepo.count() > 0) return; // 이미 있으면 Skip

        List<String> cats = List.of("운동","게임","공부","자기계발","취미","독서","악기");
        List<String> regs = List.of("청주","상당구","서원구","흥덕구","청원구","세종","대전","서울");

        // 샘플 클럽 9개
        for (int i=1; i<=9; i++) {
            Club c = new Club();
            c.setName("샘플 모임 " + i);
            c.setIntro("이 모임은 샘플 데이터입니다. 카드 UI/검색/필터 확인용.");
            c.setCategory(cats.get(i % cats.size()));
            c.setRegion(regs.get(i % regs.size()));
            // 썸네일은 비워두면 '이미지' 박스가 보임. URL을 넣고 싶으면 이미지 주소 기입.
            c.setThumbnailUrl("");
            clubRepo.save(c);
        }

        // 각 모임에 랜덤 멤버 수(0~5) 넣어보기
        Random r = new Random();
        clubRepo.findAll().forEach(c -> {
            int n = r.nextInt(6); // 0~5
            for (int i=0; i<n; i++) {
                Membership m = new Membership();
                m.setClubId(c.getId());
                m.setUserId((long) r.nextInt(1000)); // 샘플 유저 id
                memRepo.save(m);
            }
        });
    }
}
