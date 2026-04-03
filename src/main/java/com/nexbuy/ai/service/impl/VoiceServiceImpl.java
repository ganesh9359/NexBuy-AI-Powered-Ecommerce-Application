package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.VoiceService;
import com.nexbuy.exception.CustomException;
import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoiceServiceImpl implements VoiceService {

    private final AiCommerceSupport aiCommerceSupport;

    public VoiceServiceImpl(AiCommerceSupport aiCommerceSupport) {
        this.aiCommerceSupport = aiCommerceSupport;
    }

    @Override
    public AiRequest.VoiceSearchResponse search(String email, AiRequest.VoiceSearchRequest request) {
        String transcript = request == null || request.transcript() == null ? "" : request.transcript().trim();
        if (transcript.isBlank()) {
            throw new CustomException("Say or type what you want to shop for", HttpStatus.BAD_REQUEST);
        }

        AiCommerceSupport.SearchPlan plan = aiCommerceSupport.buildSearchPlan(transcript, 8);
        AiCommerceSupport.ShoppingIntent intent = plan.effectiveIntent();
        List<ProductDto.ProductCard> products = plan.products();

        Long userId = aiCommerceSupport.findUserId(email);
        aiCommerceSupport.logSearch(userId, transcript, "voice", products.size());
        aiCommerceSupport.logAiRequest(userId, "voice", transcript, plan.summary());

        return new AiRequest.VoiceSearchResponse(
                transcript,
                plan.summary(),
                plan.relaxed() ? "guided" : "high",
                aiCommerceSupport.toInterpretation(intent),
                products,
                List.of(
                        "Add a brand or budget if you want tighter results.",
                        "Say or type 'only in stock' to filter harder.",
                        "Ask for cheaper, newer, or premium options to refine the list."
                )
        );
    }
}
