package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ImageSearchService;
import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ImageSearchServiceImpl implements ImageSearchService {

    private final AiCommerceSupport aiCommerceSupport;

    public ImageSearchServiceImpl(AiCommerceSupport aiCommerceSupport) {
        this.aiCommerceSupport = aiCommerceSupport;
    }

    @Override
    public AiRequest.ImageSearchResponse search(String email, MultipartFile file, String hint) {
        AiCommerceSupport.ImageInsight insight = aiCommerceSupport.analyzeImage(file, hint);
        List<ProductDto.ProductCard> products = aiCommerceSupport.searchByImageInsight(insight, 8);
        Long userId = aiCommerceSupport.findUserId(email);

        String summary;
        if (insight.extractedHint() != null && !products.isEmpty()) {
            summary = "Matched this image against the live catalog using \"" + insight.extractedHint() + "\" and found " + products.size() + " relevant products.";
        } else if (!products.isEmpty()) {
            summary = "The image did not expose many useful product words, so I used a graceful fallback and surfaced strong live catalog matches.";
        } else {
            summary = "I could not read enough detail from the image alone. Add a short hint like brand, product type, or use-case for a tighter match.";
        }

        aiCommerceSupport.logSearch(userId, insight.extractedHint() == null ? "image search" : insight.extractedHint(), "image", products.size());
        aiCommerceSupport.logAiRequest(userId, "image_search", insight.fileName(), summary);

        return new AiRequest.ImageSearchResponse(
                summary,
                insight.extractedHint(),
                insight.confidence(),
                insight.palette(),
                products,
                List.of(
                        "Add a short hint like 'gaming phone' or 'formal watch' for sharper matches.",
                        "Use a product screenshot for better keyword extraction.",
                        "Try voice search if you want to combine the visual with a spoken budget."
                )
        );
    }
}