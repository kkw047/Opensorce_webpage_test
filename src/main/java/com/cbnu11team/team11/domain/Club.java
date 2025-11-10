package com.cbnu11team.team11.domain;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "clubs")
public class Club {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "region_do", length = 50)
    private String regionDo;

    @Column(name = "region_si", length = 50)
    private String regionSi;

    // 이미지 저장: DB에 이미 LONGBLOB 존재 → 엔티티 기대값도 LONGBLOB로 고정
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "club_categories",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    public Club() {}

    // getter/setter
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRegionDo() { return regionDo; }
    public String getRegionSi() { return regionSi; }
    public byte[] getImageData() { return imageData; }
    public Set<Category> getCategories() { return categories; }

    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public void setDescription(String description) { this.description = description == null ? null : description.trim(); }
    public void setRegionDo(String regionDo) { this.regionDo = regionDo; }
    public void setRegionSi(String regionSi) { this.regionSi = regionSi; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public void setCategories(Set<Category> categories) { this.categories = categories == null ? new LinkedHashSet<>() : categories; }
}
