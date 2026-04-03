package com.nexbuy.ai.service;

import com.nexbuy.ai.dto.AiRequest;

public interface RecommendationService {
    AiRequest.RecommendationResponse getRecommendations(String email);
}