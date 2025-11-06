package com.cbnu11team.opensource11.club.dto;

public class CategoryView {
    private final String id;    // 카테고리 문자열 그대로 id로 사용
    private final String name;  // 화면 표시명(여기선 동일)
    private final long count;   // 해당 카테고리 모임 수

    public CategoryView(String id, String name, long count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getCount() { return count; }
}
