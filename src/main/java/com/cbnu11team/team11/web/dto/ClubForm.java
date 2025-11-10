package com.cbnu11team.team11.web.dto;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public class ClubForm {
    private String name;
    private String description;
    private String regionDo;
    private String regionSi;
    private List<Long> categoryIds;
    private MultipartFile image;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRegionDo() { return regionDo; }
    public void setRegionDo(String regionDo) { this.regionDo = regionDo; }

    public String getRegionSi() { return regionSi; }
    public void setRegionSi(String regionSi) { this.regionSi = regionSi; }

    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }

    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}
