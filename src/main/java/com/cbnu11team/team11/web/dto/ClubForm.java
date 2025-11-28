package com.cbnu11team.team11.web.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ClubForm(
        String name,
        String description,
        String regionDo,
        String regionSi,
        MultipartFile imageFile,
        List<Long> categoryIds,
        String newCategoryName
) {}
