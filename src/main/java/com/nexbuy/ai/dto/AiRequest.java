package com.nexbuy.ai.dto;

import com.nexbuy.modules.product.dto.ProductDto;

import java.time.LocalDateTime;
import java.util.List;

public final class AiRequest {

    private AiRequest() {
    }

    public record ChatPromptRequest(String message, String language) {
    }

    public record OrderPreview(String orderNumber,
                               String status,
                               String paymentStatus,
                               LocalDateTime placedAt) {
    }

    public record ChatResponse(String language,
                               String headline,
                               String answer,
                               String intent,
                               List<String> quickReplies,
                               List<ProductDto.ProductCard> products,
                               List<OrderPreview> orders,
                               String nextStep,
                               String targetUrl) {
    }

    public record SearchInterpretation(String query,
                                       String category,
                                       String brand,
                                       String tag,
                                       Integer minPrice,
                                       Integer maxPrice,
                                       Boolean inStock,
                                       String sort) {
    }

    public record VoiceSearchRequest(String transcript) {
    }

    public record VoiceSearchResponse(String transcript,
                                      String summary,
                                      String confidence,
                                      SearchInterpretation interpretation,
                                      List<ProductDto.ProductCard> products,
                                      List<String> followUps) {
    }

    public record ImageSearchResponse(String summary,
                                      String extractedHint,
                                      String confidence,
                                      List<String> palette,
                                      SearchInterpretation interpretation,
                                      List<ProductDto.ProductCard> products,
                                      List<String> followUps) {
    }

    public record RecommendationResponse(boolean personalized,
                                         String headline,
                                         String summary,
                                         List<String> signals,
                                         List<ProductDto.ProductCard> products) {
    }
}
