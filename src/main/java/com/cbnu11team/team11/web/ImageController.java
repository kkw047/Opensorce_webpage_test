package com.cbnu11team.team11.web;

import com.cbnu11team.team11.domain.Club;
import com.cbnu11team.team11.repository.ClubRepository;
import com.cbnu11team.team11.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ClubRepository clubRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/images/club/{id}")
    @ResponseBody
    public ResponseEntity<?> clubImage(@PathVariable Long id) {
        Optional<Club> opt = clubRepository.findById(id);
        if (opt.isEmpty()) return okPlaceholder();

        Club c = opt.get();
        String ref = c.getImageUrl();

        if (ref == null || ref.isBlank()) {
            return okPlaceholder();
        }

        // 외부 URL인 경우 그대로 리다이렉트 (ex. https://... or http://...)
        String lower = ref.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return ResponseEntity.status(302).location(URI.create(ref)).build();
        }

        // 파일시스템에서 로딩
        Resource res = fileStorageService.loadAsResource(ref);
        if (res == null) return okPlaceholder();

        String contentType = "application/octet-stream";
        try {
            // 파일 확장자로 추정
            contentType = guessContentType(ref);
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000") // 30일
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"club-" + id + "\"")
                .body(res);
    }

    private ResponseEntity<ByteArrayResource> okPlaceholder() {
        byte[] png = placeholder1x1();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.CONTENT_TYPE, "image/png")
                .body(new ByteArrayResource(png));
    }

    private String guessContentType(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private byte[] placeholder1x1() {
        return new byte[] {
                (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
                0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,0x06,0x00,0x00,0x00,0x1F,(byte)0x15,(byte)0xC4,
                (byte)0x89,0x00,0x00,0x00,0x0A,0x49,0x44,0x41,0x54,0x78,(byte)0xDA,0x63,0x00,0x01,0x00,0x00,
                0x05,0x00,0x01,(byte)0x0D,0x0A,0x2D,(byte)0xB4,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82
        };
    }
}
