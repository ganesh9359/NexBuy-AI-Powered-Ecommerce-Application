package com.nexbuy.ai.service;

import com.nexbuy.ai.dto.AiRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ImageSearchService {
    AiRequest.ImageSearchResponse search(String email, MultipartFile file, String hint);
}