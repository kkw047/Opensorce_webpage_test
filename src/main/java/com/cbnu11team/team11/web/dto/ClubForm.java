package com.cbnu11team.team11.web.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ClubForm {
    private String name;
    private String description;
    private String regionDo;
    private String regionSi;
    private List<Long> categoryIds;
    private List<String> newCategoryNames;
    private MultipartFile image;
}
