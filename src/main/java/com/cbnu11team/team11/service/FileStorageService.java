package com.cbnu11team.team11.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService() {
        this.root = Paths.get(System.getProperty("user.dir"), "uploads");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create uploads directory", e);
        }
    }

    /** 파일 저장 후, 웹에서 접근 가능한 URL("/uploads/파일명") 리턴 */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String ext = getExt(file.getOriginalFilename());
        String name = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        try {
            Files.copy(file.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private String getExt(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return (i == -1) ? "" : filename.substring(i + 1);
    }
}
