package com.nexbuy.modules.admin.service.impl;

import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.admin.dto.AdminUploadResponse;
import com.nexbuy.modules.admin.service.AdminMediaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminMediaServiceImpl implements AdminMediaService {

    private final Path uploadRoot;

    public AdminMediaServiceImpl(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public AdminUploadResponse uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("Please choose an image to upload", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new CustomException("Only image files are allowed", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new CustomException("Image size must be 5 MB or smaller", HttpStatus.BAD_REQUEST);
        }

        String extension = detectExtension(file.getOriginalFilename(), contentType);
        String fileName = "product-" + UUID.randomUUID() + extension;
        Path productDir = uploadRoot.resolve("products").normalize();
        Path target = productDir.resolve(fileName).normalize();

        if (!target.startsWith(productDir)) {
            throw new CustomException("Invalid upload path", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(productDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new CustomException("Could not upload image", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new AdminUploadResponse("/uploads/products/" + fileName, fileName);
    }

    private String detectExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            String trimmed = originalFilename.trim();
            int dotIndex = trimmed.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < trimmed.length() - 1) {
                String extension = trimmed.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.(jpg|jpeg|png|webp|gif)$")) {
                    return extension;
                }
            }
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}