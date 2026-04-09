package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ChatService;
import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ChatServiceImpl implements ChatService {

    private final AiCommerceSupport aiCommerceSupport;

    public ChatServiceImpl(AiCommerceSupport aiCommerceSupport) {
        this.aiCommerceSupport = aiCommerceSupport;
    }

    @Override
    public AiRequest.ChatResponse chat(String email, AiRequest.ChatPromptRequest request) {
        String language = normalizeLanguage(request == null ? null : request.language());
        String message = request == null || request.message() == null ? "" : request.message().trim();
        String normalized = message.toLowerCase(Locale.ROOT);
        Long userId = aiCommerceSupport.findUserId(email);

        String intent = detectIntent(normalized);
        List<String> quickReplies = defaultQuickReplies(intent, language);
        List<ProductDto.ProductCard> products = new ArrayList<>();
        List<AiRequest.OrderPreview> orders = new ArrayList<>();
        String headline;
        String answer;
        String nextStep;
        String targetUrl = null;

        switch (intent) {
            case "order" -> {
                headline = phrase(language, "Order help", "Order sahayata", "Order madat");
                orders = aiCommerceSupport.findMentionedOrder(email, message)
                        .map(List::of)
                        .orElse(aiCommerceSupport.loadRecentOrders(email, 3));
                if (orders.isEmpty()) {
                    answer = phrase(
                            language,
                            "Sign in first and I can open your order timeline, refund progress, or return options.",
                            "Pehle sign in kijiye, phir main aapka order timeline, refund progress, ya return options khol sakta hoon.",
                            "Adhi sign in kara, mag mi tumcha order timeline, refund progress, kiwa return options ughadu shakto."
                    );
                    nextStep = phrase(
                            language,
                            "Ask me to track your latest order once you are signed in.",
                            "Sign in ke baad boliye: track my latest order.",
                            "Sign in zhalyanantar mhana: track my latest order."
                    );
                } else {
                    AiRequest.OrderPreview latest = orders.get(0);
                    answer = phrase(
                            language,
                            latest.orderNumber() + " is " + latest.status() + " and payment is " + latest.paymentStatus() + ".",
                            latest.orderNumber() + " abhi " + latest.status() + " hai aur payment " + latest.paymentStatus() + " hai.",
                            latest.orderNumber() + " sadhya " + latest.status() + " aahe ani payment " + latest.paymentStatus() + " aahe."
                    );
                    nextStep = phrase(
                            language,
                            "Opening your orders page now.",
                            "Ab main aapka orders page khol raha hoon.",
                            "Ata mi tumche orders page ughadat aahe."
                    );
                    targetUrl = "/order?highlight=" + latest.orderNumber();
                }
            }
            case "recommend" -> {
                AiCommerceSupport.RecommendationBundle bundle = aiCommerceSupport.buildRecommendations(email, 6);
                headline = phrase(language, bundle.headline(), "Recommended for you", "Tumchya sathi picks");
                products = bundle.products();
                answer = phrase(
                        language,
                        bundle.summary(),
                        "Maine aapke recent shopping pattern ke hisab se strong picks tayar kiye hain.",
                        "Mi tumchya recent shopping pattern pramane strong picks tayar kele aahet."
                );
                nextStep = phrase(
                        language,
                        "Opening the recommendations page.",
                        "Recommendations page khol raha hoon.",
                        "Recommendations page ughadat aahe."
                );
                targetUrl = "/ai";
            }
            case "search" -> {
                AiCommerceSupport.SearchPlan plan = aiCommerceSupport.buildSearchPlan(message, 6);
                products = plan.products();
                headline = phrase(language, "Shopping assistant", "Shopping assistant", "Shopping assistant");
                answer = plan.summary();
                if (shouldOpenSingleProduct(normalized, products)) {
                    targetUrl = "/product/" + products.get(0).slug();
                    nextStep = phrase(
                            language,
                            "Opening the product page.",
                            "Product page khol raha hoon.",
                            "Product page ughadat aahe."
                    );
                } else {
                    targetUrl = aiCommerceSupport.buildCatalogUrl(plan.effectiveIntent(), message);
                    nextStep = phrase(
                            language,
                            "Opening the best matching catalog page.",
                            "Best matching catalog page khol raha hoon.",
                            "Best matching catalog page ughadat aahe."
                    );
                }
            }
            default -> {
                headline = phrase(language, "NexBuy assistant", "NexBuy assistant", "NexBuy assistant");
                answer = phrase(
                        language,
                        "I can help with product search, recommendations, order tracking, refunds, and returns.",
                        "Main product search, recommendations, order tracking, refunds aur returns me help kar sakta hoon.",
                        "Mi product search, recommendations, order tracking, refunds ani returns madhe madat karu shakto."
                );
                nextStep = phrase(
                        language,
                        "Ask for a product type, budget, or your latest order.",
                        "Product type, budget, ya latest order ke bare me poochhiye.",
                        "Product type, budget, kiwa latest order baddal vichara."
                );
            }
        }

        aiCommerceSupport.logAiRequest(userId, "chat", message, headline + ": " + answer);
        return new AiRequest.ChatResponse(language, headline, answer, intent, quickReplies, products, orders, nextStep, targetUrl);
    }

    private String detectIntent(String message) {
        if (containsAny(message,
                "order", "track", "refund", "return", "cancel", "shipment", "delivery",
                "mera order", "track karo", "refund", "return", "cancel",
                "majha order", "refund", "return", "cancel")) {
            return "order";
        }
        if (containsAny(message,
                "recommend", "suggest", "for me", "similar", "based on", "pick for me",
                "recommend karo", "suggest karo", "sujhao", "suchava")) {
            return "recommend";
        }
        if (containsAny(message,
                "find", "show", "need", "looking", "buy", "under", "cheap", "budget", "premium", "search",
                "dikhao", "dhoondo", "chaahiye", "kharid", "budget",
                "dakhava", "shodha", "pahije", "kharedi")) {
            return "search";
        }
        return "general";
    }

    private boolean shouldOpenSingleProduct(String message, List<ProductDto.ProductCard> products) {
        if (products == null || products.size() != 1) {
            return false;
        }
        return containsAny(message, "detail", "details", "spec", "specs", "open", "show this", "buy this");
    }

    private List<String> defaultQuickReplies(String intent, String language) {
        return switch (intent) {
            case "order" -> List.of(
                    phrase(language, "Track my latest order", "Track my latest order", "Track my latest order"),
                    phrase(language, "Open refund progress", "Open refund progress", "Open refund progress"),
                    phrase(language, "Can I return this order?", "Can I return this order?", "Can I return this order?")
            );
            case "recommend" -> List.of(
                    phrase(language, "Recommend phones under 30000", "Recommend phones under 30000", "Recommend phones under 30000"),
                    phrase(language, "Show best value watches", "Show best value watches", "Show best value watches"),
                    phrase(language, "Suggest gaming products", "Suggest gaming products", "Suggest gaming products")
            );
            case "search" -> List.of(
                    phrase(language, "Show mobiles under 20000", "Show mobiles under 20000", "Show mobiles under 20000"),
                    phrase(language, "Find camera phones", "Find camera phones", "Find camera phones"),
                    phrase(language, "Show in-stock laptops", "Show in-stock laptops", "Show in-stock laptops")
            );
            default -> List.of(
                    phrase(language, "Find budget products", "Find budget products", "Find budget products"),
                    phrase(language, "Track my latest order", "Track my latest order", "Track my latest order"),
                    phrase(language, "Show best deals today", "Show best deals today", "Show best deals today")
            );
        };
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("hi")) {
            return "hi";
        }
        if (normalized.startsWith("mr") || normalized.startsWith("ma")) {
            return "mr";
        }
        return "en";
    }

    private String phrase(String language, String english, String hindi, String marathi) {
        return switch (language) {
            case "hi" -> hindi;
            case "mr" -> marathi;
            default -> english;
        };
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
