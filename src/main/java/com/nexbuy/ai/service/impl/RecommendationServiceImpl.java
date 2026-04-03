package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.RecommendationService;
import org.springframework.stereotype.Service;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final AiCommerceSupport aiCommerceSupport;

    public RecommendationServiceImpl(AiCommerceSupport aiCommerceSupport) {
        this.aiCommerceSupport = aiCommerceSupport;
    }

    @Override
    public AiRequest.RecommendationResponse getRecommendations(String email) {
        AiCommerceSupport.RecommendationBundle bundle = aiCommerceSupport.buildRecommendations(email, 8);
        Long userId = aiCommerceSupport.findUserId(email);
        aiCommerceSupport.logAiRequest(userId, "recommend", bundle.summary(), bundle.headline());
        return new AiRequest.RecommendationResponse(
                bundle.personalized(),
                bundle.headline(),
                bundle.summary(),
                bundle.signals(),
                bundle.products()
        );
    }
}