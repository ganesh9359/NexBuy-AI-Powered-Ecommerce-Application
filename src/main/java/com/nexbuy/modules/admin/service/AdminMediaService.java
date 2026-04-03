package com.nexbuy.modules.admin.service;

import com.nexbuy.modules.admin.dto.AdminUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AdminMediaService {
    AdminUploadResponse uploadProductImage(MultipartFile file);
}