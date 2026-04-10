package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.impl.AiCommerceSupport;
import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class SearchAssistController {

    private final AiCommerceSupport aiCommerceSupport;

    public SearchAssistController(AiCommerceSupport aiCommerceSupport) {
        this.aiCommerceSupport = aiCommerceSupport;
    }

    @PostMapping("/search-assist")
    public ResponseEntity<AiRequest.VoiceSearchResponse> searchAssist(
            @RequestBody AiRequest.VoiceSearchRequest request) {

        String query = request == null || request.transcript() == null ? "" : request.transcript().trim();

        if (query.isEmpty()) {
            return ResponseEntity.ok(new AiRequest.VoiceSearchResponse(
                    query, "Please provide a search query.", "low",
                    new AiRequest.SearchInterpretation(null, null, null, null, null, null, null, null),
                    List.of(),
                    List.of("Try searching for phones", "Show me laptops", "Find headphones under 2000")
            ));
        }

        AiCommerceSupport.SearchPlan plan = aiCommerceSupport.buildSearchPlan(query, 8);
        AiRequest.SearchInterpretation interpretation = aiCommerceSupport.toInterpretation(plan.effectiveIntent());
        List<ProductDto.ProductCard> products = plan.products();

        String summary = plan.summary();
        String confidence = products.isEmpty() ? "low" : plan.relaxed() ? "medium" : "high";

        List<String> followUps = List.of(
                "Show me similar products",
                "Filter by price",
                "Show in-stock only"
        );

        return ResponseEntity.ok(new AiRequest.VoiceSearchResponse(
                query, summary, confidence, interpretation, products, followUps
        ));
    }
}
