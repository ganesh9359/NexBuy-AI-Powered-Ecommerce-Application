package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ResponseEntity<AiRequest.RecommendationResponse> getRecommendations(Authentication authentication) {
        return ResponseEntity.ok(recommendationService.getRecommendations(authentication == null ? null : authentication.getName()));
    }
}