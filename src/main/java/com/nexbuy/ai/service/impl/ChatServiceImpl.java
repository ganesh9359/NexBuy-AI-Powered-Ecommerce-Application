package com.nexbuy.ai.service.impl;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ChatService;
import com.nexbuy.modules.product.dto.ProductDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        switch (intent) {
            case "order" -> {
                headline = t(language, "Order help", "ऑर्डर सहायता", "ऑर्डर मदत");
                orders = aiCommerceSupport.findMentionedOrder(email, message)
                        .map(List::of)
                        .orElse(aiCommerceSupport.loadRecentOrders(email, 3));
                if (orders.isEmpty()) {
                    answer = t(
                            language,
                            "I can help with tracking, cancellations, refunds, and returns, but I need you to sign in before I can see any orders.",
                            "मैं ट्रैकिंग, कैंसलेशन, रिफंड और रिटर्न में मदद कर सकता हूँ, लेकिन ऑर्डर देखने के लिए आपको पहले साइन इन करना होगा।",
                            "मी ट्रॅकिंग, कॅन्सल, रिफंड आणि रिटर्नमध्ये मदत करू शकतो, पण ऑर्डर पाहण्यासाठी तुम्ही आधी साइन इन करणे गरजेचे आहे."
                    );
                    nextStep = t(
                            language,
                            "Sign in, then ask me to track your latest order.",
                            "साइन इन करने के बाद मुझसे अपना लेटेस्ट ऑर्डर ट्रैक करने को कहें।",
                            "साइन इन केल्यानंतर मला तुमचा नवीनतम ऑर्डर ट्रॅक करायला सांगा."
                    );
                } else {
                    AiRequest.OrderPreview latest = orders.get(0);
                    String placedText = formatPlacedAt(language, latest.placedAt());
                    String status = localizeOrderStatus(language, latest.status());
                    String paymentStatus = localizePaymentStatus(language, latest.paymentStatus());
                    answer = switch (language) {
                        case "hi" -> latest.orderNumber() + " अभी " + status + " है और पेमेंट " + paymentStatus + " है। यह " + placedText + " प्लेस किया गया था।";
                        case "mr" -> latest.orderNumber() + " सध्या " + status + " आहे आणि पेमेंट " + paymentStatus + " आहे. हा ऑर्डर " + placedText + " प्लेस झाला.";
                        default -> latest.orderNumber() + " is currently " + status + " with payment marked " + paymentStatus + ". It was placed " + placedText + ".";
                    };
                    nextStep = t(
                            language,
                            "Open your orders page if you want the full timeline and refund or return options.",
                            "अगर आपको पूरा टाइमलाइन और रिफंड या रिटर्न विकल्प चाहिए, तो अपना ऑर्डर पेज खोलें।",
                            "पूर्ण टाइमलाइन आणि रिफंड किंवा रिटर्न पर्याय पाहायचे असतील तर तुमचे ऑर्डर पेज उघडा."
                    );
                }
            }
            case "recommend" -> {
                AiCommerceSupport.RecommendationBundle bundle = aiCommerceSupport.buildRecommendations(email, 6);
                headline = t(language, bundle.headline(), "आपके लिए चुनी गई सिफारिशें", "तुमच्यासाठी निवडलेल्या शिफारसी");
                products = bundle.products();
                answer = switch (language) {
                    case "hi" -> bundle.personalized()
                            ? "ये सुझाव आपके हाल के ब्राउज़िंग, सर्च और ऑर्डर गतिविधि से तैयार किए गए हैं।"
                            : "मैंने अभी लाइव स्टोर की मजबूत और लोकप्रिय पिक्स चुनी हैं।";
                    case "mr" -> bundle.personalized()
                            ? "या शिफारसी तुमच्या अलीकडच्या ब्राउझिंग, सर्च आणि ऑर्डर क्रियाकलापांवर आधारित आहेत."
                            : "सध्या मी लाईव्ह स्टोअरमधील मजबूत आणि लोकप्रिय पर्याय निवडले आहेत.";
                    default -> bundle.summary();
                };
                nextStep = bundle.personalized()
                        ? t(
                                language,
                                "Open a few of these products to sharpen the next recommendation round.",
                                "इनमें से कुछ प्रोडक्ट खोलें, ताकि अगला राउंड और बेहतर हो सके।",
                                "यातील काही प्रॉडक्ट उघडा, म्हणजे पुढची शिफारस आणखी अचूक होईल."
                        )
                        : t(
                                language,
                                "Sign in and browse a little more to unlock sharper recommendations.",
                                "बेहतर सिफारिशों के लिए साइन इन करें और थोड़ा और ब्राउज़ करें।",
                                "अधिक अचूक शिफारसींसाठी साइन इन करा आणि थोडे अजून ब्राउझ करा."
                        );
            }
            case "search" -> {
                AiCommerceSupport.ShoppingIntent shoppingIntent = aiCommerceSupport.interpretShoppingIntent(message);
                products = aiCommerceSupport.searchProducts(shoppingIntent, 6);
                headline = t(language, "Shopping assistant", "शॉपिंग असिस्टेंट", "शॉपिंग सहाय्यक");
                answer = products.isEmpty()
                        ? t(
                                language,
                                "I could not find a strong match from that phrasing, so I would try a clearer product type, brand, or budget.",
                                "मुझे इस तरह से कहे गए वाक्य से सही मैच नहीं मिला, इसलिए प्रोडक्ट टाइप, ब्रांड या बजट थोड़ा और साफ बताइए।",
                                "या प्रकारे सांगितलेल्या वाक्यांतून मला योग्य मॅच मिळाला नाही, म्हणून प्रॉडक्ट टाइप, ब्रँड किंवा बजेट थोडे स्पष्ट सांगा."
                        )
                        : localizeSearchSummary(language, shoppingIntent, products.size());
                nextStep = products.isEmpty()
                        ? t(
                                language,
                                "Try something like 'phones under 30000', 'Samsung watches', or 'gaming laptops'.",
                                "'30000 के अंदर फोन', 'Samsung watches' या 'gaming laptops' जैसा कुछ ट्राय करें।",
                                "'30000 च्या आत फोन', 'Samsung watches' किंवा 'gaming laptops' असे काहीतरी ट्राय करा."
                        )
                        : t(
                                language,
                                "Open any result or ask me to narrow it by brand, stock, or price.",
                                "किसी भी रिज़ल्ट को खोलें या मुझसे इसे ब्रांड, स्टॉक या कीमत से और संकरा करने को कहें।",
                                "कोणताही रिजल्ट उघडा किंवा मला ब्रँड, स्टॉक किंवा किमतीनुसार आणखी कमी पर्याय दाखवायला सांगा."
                        );
            }
            default -> {
                AiCommerceSupport.RecommendationBundle bundle = aiCommerceSupport.buildRecommendations(email, 4);
                products = bundle.products();
                headline = t(language, "NexBuy assistant", "NexBuy सहायक", "NexBuy सहाय्यक");
                answer = t(
                        language,
                        "I can help you find products, explain deals, track orders, and suggest what to buy next.",
                        "मैं आपको प्रोडक्ट ढूँढने, डील समझने, ऑर्डर ट्रैक करने और आगे क्या खरीदना चाहिए यह सुझाने में मदद कर सकता हूँ।",
                        "मी तुम्हाला प्रॉडक्ट शोधण्यात, डील समजावण्यात, ऑर्डर ट्रॅक करण्यात आणि पुढे काय घ्यावे हे सुचवण्यात मदत करू शकतो."
                );
                nextStep = t(
                        language,
                        "Ask for budget options, order tracking, or recommendations based on your activity.",
                        "बजट ऑप्शन, ऑर्डर ट्रैकिंग या आपकी गतिविधि पर आधारित सिफारिशें पूछें।",
                        "बजेट पर्याय, ऑर्डर ट्रॅकिंग किंवा तुमच्या activity वर आधारित शिफारसी विचारा."
                );
            }
        }

        aiCommerceSupport.logAiRequest(userId, "chat", message, headline + ": " + answer);
        return new AiRequest.ChatResponse(language, headline, answer, intent, quickReplies, products, orders, nextStep);
    }

    private String detectIntent(String message) {
        if (containsAny(message,
                "order", "track", "refund", "return", "cancel", "shipment", "delivery",
                "ऑर्डर", "ट्रैक", "रिफंड", "रिटर्न", "कैंसल", "रद्द", "डिलिवरी",
                "ऑर्डर", "ट्रॅक", "रिफंड", "रिटर्न", "रद्द", "डिलिव्हरी")) {
            return "order";
        }
        if (containsAny(message,
                "recommend", "suggest", "for me", "similar", "based on", "pick for me",
                "सुझाव", "सिफारिश", "रेकमेंड",
                "शिफारस", "सुचवा")) {
            return "recommend";
        }
        if (containsAny(message,
                "find", "show", "need", "looking", "buy", "under", "cheap", "budget", "premium", "search",
                "दिखाओ", "ढूंढो", "चाहिए", "खरीद", "बजट", "सस्ता",
                "दाखवा", "शोध", "पाहिजे", "खरेदी", "बजेट", "स्वस्त")) {
            return "search";
        }
        return "general";
    }

    private List<String> defaultQuickReplies(String intent, String language) {
        return switch (intent) {
            case "order" -> switch (language) {
                case "hi" -> List.of("मेरा लेटेस्ट ऑर्डर ट्रैक करो", "क्या मैं यह ऑर्डर कैंसल कर सकता हूँ?", "मेरा रिफंड स्टेटस क्या है?");
                case "mr" -> List.of("माझा नवीनतम ऑर्डर ट्रॅक करा", "हा ऑर्डर मी रद्द करू शकतो का?", "माझ्या रिफंडची स्थिती काय आहे?");
                default -> List.of("Track my latest order", "Can I cancel this order?", "What is my refund status?");
            };
            case "recommend" -> switch (language) {
                case "hi" -> List.of("30000 के अंदर फोन सुझाओ", "बेस्ट वैल्यू पिक्स दिखाओ", "गेमर के लिए गिफ्ट सुझाओ");
                case "mr" -> List.of("30000 च्या आत फोन सुचवा", "best value picks दाखवा", "gamer साठी gift सुचवा");
                default -> List.of("Recommend phones under 30000", "Show me value picks", "Suggest gifts for a gamer");
            };
            case "search" -> switch (language) {
                case "hi" -> List.of("इन-स्टॉक डील्स दिखाओ", "प्रीमियम वॉचेस ढूंढो", "ब्रांड से सर्च करो");
                case "mr" -> List.of("in-stock deals दाखवा", "premium watches शोधा", "brand नुसार search करा");
                default -> List.of("Show in-stock deals", "Find premium watches", "Search by brand");
            };
            default -> switch (language) {
                case "hi" -> List.of("मेरे लिए कुछ सुझाओ", "मेरा ऑर्डर ट्रैक करो", "बजट प्रोडक्ट ढूंढो");
                case "mr" -> List.of("माझ्यासाठी काही सुचवा", "माझा ऑर्डर ट्रॅक करा", "budget products शोधा");
                default -> List.of("Recommend for me", "Track my order", "Find budget products");
            };
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

    private String t(String language, String english, String hindi, String marathi) {
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

    private String formatPlacedAt(String language, LocalDateTime placedAt) {
        if (placedAt == null) {
            return t(language, "recently", "हाल ही में", "अलीकडे");
        }
        Locale locale = switch (language) {
            case "hi" -> new Locale("hi", "IN");
            case "mr" -> new Locale("mr", "IN");
            default -> Locale.ENGLISH;
        };
        return placedAt.format(DateTimeFormatter.ofPattern("d MMM, h:mm a", locale));
    }

    private String localizeOrderStatus(String language, String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pending" -> t(language, "pending", "पेंडिंग", "प्रलंबित");
            case "paid" -> t(language, "paid", "पेड", "पेड");
            case "processing" -> t(language, "processing", "प्रोसेस में", "प्रोसेस मध्ये");
            case "shipped" -> t(language, "shipped", "शिप्ड", "पाठवलेला");
            case "delivered" -> t(language, "delivered", "डिलीवर", "डिलिव्हर");
            case "cancelled" -> t(language, "cancelled", "कैंसल", "रद्द");
            case "refund pending" -> t(language, "refund pending", "रिफंड प्रगति में", "रिफंड सुरू आहे");
            case "refunded" -> t(language, "refunded", "रिफंड पूरा", "रिफंड पूर्ण");
            default -> status == null || status.isBlank() ? t(language, "updated", "अपडेटेड", "अपडेटेड") : status;
        };
    }

    private String localizePaymentStatus(String language, String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pending" -> t(language, "pending", "पेंडिंग", "प्रलंबित");
            case "success", "paid", "captured" -> t(language, "successful", "सफल", "यशस्वी");
            case "failed" -> t(language, "failed", "असफल", "अयशस्वी");
            case "refund_pending" -> t(language, "refund pending", "रिफंड प्रगति में", "रिफंड सुरू आहे");
            case "refunded" -> t(language, "refunded", "रिफंड पूरा", "रिफंड पूर्ण");
            default -> status == null || status.isBlank() ? t(language, "updated", "अपडेटेड", "अपडेटेड") : status;
        };
    }

    private String localizeSearchSummary(String language, AiCommerceSupport.ShoppingIntent intent, int resultsCount) {
        List<String> focus = new ArrayList<>();
        if (intent.category() != null) {
            focus.add(intent.category());
        }
        if (intent.brand() != null) {
            focus.add(intent.brand());
        }
        if (intent.tag() != null) {
            focus.add(intent.tag());
        }
        if (!intent.query().isBlank()) {
            focus.add(intent.query());
        }
        String focusText = focus.isEmpty() ? t(language, "the live catalog", "लाइव कैटलॉग", "लाईव्ह कॅटलॉग") : String.join(" / ", focus);

        if ("hi".equals(language)) {
            return resultsCount + " मैच " + focusText + " के लिए मिले।";
        }
        if ("mr".equals(language)) {
            return focusText + " साठी " + resultsCount + " मॅचेस सापडले.";
        }
        return aiCommerceSupport.summarizeShoppingIntent(intent, resultsCount);
    }
}
