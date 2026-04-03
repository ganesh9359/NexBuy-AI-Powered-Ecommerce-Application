package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ImageSearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
public class ImageSearchController {

    private final ImageSearchService imageSearchService;

    public ImageSearchController(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    @PostMapping(value = "/image-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiRequest.ImageSearchResponse> search(Authentication authentication,
                                                                @RequestParam("file") MultipartFile file,
                                                                @RequestParam(value = "hint", required = false) String hint) {
        return ResponseEntity.ok(imageSearchService.search(authentication == null ? null : authentication.getName(), file, hint));
    }
}