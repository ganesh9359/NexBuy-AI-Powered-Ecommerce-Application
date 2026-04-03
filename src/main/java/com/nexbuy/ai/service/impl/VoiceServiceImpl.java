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

        AiCommerceSupport.ShoppingIntent intent = aiCommerceSupport.interpretShoppingIntent(transcript);
        List<ProductDto.ProductCard> products = aiCommerceSupport.searchProducts(intent, 8);
        if (products.isEmpty()) {
            products = aiCommerceSupport.getTrendingProducts(6);
        }

        Long userId = aiCommerceSupport.findUserId(email);
        aiCommerceSupport.logSearch(userId, transcript, "voice", products.size());
        aiCommerceSupport.logAiRequest(userId, "voice", transcript, aiCommerceSupport.summarizeShoppingIntent(intent, products.size()));

        return new AiRequest.VoiceSearchResponse(
                transcript,
                aiCommerceSupport.summarizeShoppingIntent(intent, products.size()),
                products.isEmpty() ? "low" : "guided",
                new AiRequest.SearchInterpretation(
                        intent.query(),
                        intent.category(),
                        intent.brand(),
                        intent.tag(),
                        intent.minPrice(),
                        intent.maxPrice(),
                        intent.inStock(),
                        intent.sort()
                ),
                products,
                List.of(
                        "Try adding a brand name or budget next.",
                        "Say 'only in stock' to filter harder.",
                        "Ask for newer or cheaper options to refine the list."
                )
        );
    }
}