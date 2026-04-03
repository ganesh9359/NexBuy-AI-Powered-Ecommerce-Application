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
        AiCommerceSupport.ShoppingIntent interpretation = aiCommerceSupport.toImageShoppingIntent(insight);
        AiCommerceSupport.ImageSearchMatch match = aiCommerceSupport.searchByImageInsight(insight, 8);
        List<ProductDto.ProductCard> products = match.products();
        Long userId = aiCommerceSupport.findUserId(email);

        String summary;
        if (match.similarFallback() && !products.isEmpty()) {
            if (interpretation.category() != null) {
                summary = match.appliedThreshold() > 0
                        ? "Exact match not found. Showing " + products.size() + " similar " + interpretation.category() + " products with a softer visual threshold of " + match.appliedThreshold() + "."
                        : "Exact match not found. Showing " + products.size() + " similar " + interpretation.category() + " products instead of a blank result.";
            } else {
                summary = match.appliedThreshold() > 0
                        ? "Exact match not found. Showing the " + products.size() + " closest visually similar products from the live catalog with a softer threshold of " + match.appliedThreshold() + "."
                        : "Exact match not found. Showing the closest visually similar products from the live catalog instead of a blank result.";
            }
        } else if (insight.extractedHint() != null && !products.isEmpty()) {
            summary = "Found " + products.size() + " catalog matches with a visual match score above " + match.appliedThreshold() + " using \"" + insight.extractedHint() + "\".";
        } else if (!products.isEmpty()) {
            summary = "Found visually similar live catalog products with a confidence threshold above " + match.appliedThreshold() + ".";
        } else {
            summary = "I could not find a confident exact match yet, so I am showing the closest live catalog options.";
        }

        aiCommerceSupport.logSearch(userId, insight.extractedHint() == null ? "image search" : insight.extractedHint(), "image", products.size());
        aiCommerceSupport.logAiRequest(userId, "image_search", insight.fileName(), summary);

        return new AiRequest.ImageSearchResponse(
                summary,
                insight.extractedHint(),
                insight.confidence(),
                insight.palette(),
                aiCommerceSupport.toInterpretation(interpretation),
                products,
                List.of(
                        "Use a clear product photo with the item centered for stronger visual matching.",
                        "A short hint like 'gaming phone' or 'formal watch' can still sharpen the result.",
                        "If the exact item is not in the catalog, NexBuy will now fall back to similar products."
                )
        );
    }
}
