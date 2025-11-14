package com.cbnu11team.team11.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.root:#{systemProperties['user.home'] + '/team11-uploads'}}")
    private String storageRoot; // 예: C:\Users\USER\team11-uploads  또는  /home/ubuntu/team11-uploads

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot).toLowerCase();

            String savedName = UUID.randomUUID().toString().replace("-", "") + ext;

            Path dir = Paths.get(storageRoot).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path dest = dir.resolve(savedName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, REPLACE_EXISTING);
            }
            log.info("[FileStorage] saved: {} -> {}", original, dest);
            return savedName;
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    public Resource loadAsResource(String nameOrAbsolutePath) {
        try {
            if (nameOrAbsolutePath == null || nameOrAbsolutePath.isBlank()) return null;
            Path p = resolvePath(nameOrAbsolutePath);
            if (!Files.exists(p) || !Files.isReadable(p)) return null;
            return new ByteArrayResource(Files.readAllBytes(p));
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] loadAsBytes(String nameOrAbsolutePath) {
        try {
            if (nameOrAbsolutePath == null || nameOrAbsolutePath.isBlank()) return null;
            Path p = resolvePath(nameOrAbsolutePath);
            if (!Files.exists(p) || !Files.isReadable(p)) return null;
            return Files.readAllBytes(p);
        } catch (Exception e) {
            return null;
        }
    }

    private Path resolvePath(String nameOrAbsolutePath) {
        Path path = Paths.get(nameOrAbsolutePath);
        // 절대경로나 드라이브 경로면 그대로, 아니면 저장루트/파일명
        if (path.isAbsolute() || looksAbsoluteOnWindows(nameOrAbsolutePath)) {
            return path.normalize();
        }
        return Paths.get(storageRoot).toAbsolutePath().normalize().resolve(nameOrAbsolutePath).normalize();
    }

    private boolean looksAbsoluteOnWindows(String s) {
        // 예: C:\..., D:\...
        return s != null && s.length() >= 3 && Character.isLetter(s.charAt(0)) && s.charAt(1) == ':' && (s.charAt(2) == '\\' || s.charAt(2) == '/');
    }
}
