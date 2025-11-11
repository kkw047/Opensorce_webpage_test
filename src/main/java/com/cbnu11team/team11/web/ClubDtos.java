package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Category;
import com.cbnu11team.team11.domain.Club;

import java.util.List;

public final class ClubDtos {
    private ClubDtos() {}

    public static ClubCard toCard(Club c) {
        return new ClubCard(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getImageUrl(),
                c.getRegionDo(),
                c.getRegionSi(),
                c.getCategories() == null
                        ? List.of()
                        : c.getCategories().stream().map(Category::getName).toList()
        );
    }

    public record ClubCard(
            Long id,
            String name,
            String description,
            String imageUrl,
            String regionDo,
            String regionSi,
            List<String> categories
    ) {}
}
