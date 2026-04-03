package com.nexbuy.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAIClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String apiKey;
    private final String searchModel;
    private final String visionModel;

    public OpenAIClient(ObjectMapper objectMapper,
                        @Value("${app.ai.openai.api-key:}") String apiKey,
                        @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
                        @Value("${app.ai.openai.search-model:gpt-4.1-mini}") String searchModel,
                        @Value("${app.ai.openai.vision-model:gpt-4.1-mini}") String visionModel) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.searchModel = searchModel == null || searchModel.isBlank() ? "gpt-4.1-mini" : searchModel.trim();
        this.visionModel = visionModel == null || visionModel.isBlank() ? "gpt-4.1-mini" : visionModel.trim();
        this.webClient = WebClient.builder()
                .baseUrl((baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com/v1" : baseUrl.trim())
                .defaultHeader("Authorization", "Bearer " + this.apiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public Optional<SearchInference> interpretSearch(String rawText,
                                                     List<String> categories,
                                                     List<String> brands,
                                                     List<String> tags) {
        if (!isConfigured() || rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        String prompt = """
                You convert messy shopper queries into a structured ecommerce search plan for an Indian storefront.

                Known category slugs: %s
                Known brand slugs: %s
                Known tags: %s

                Return JSON only with this shape:
                {
                  "query": "short shopper-friendly query or null",
                  "category": "known category slug or null",
                  "brand": "known brand slug or null",
                  "tag": "known tag or null",
                  "minPriceRupees": 0,
                  "maxPriceRupees": 0,
                  "inStock": true,
                  "sort": "relevance|price_asc|price_desc|newest|title|stock_desc",
                  "fallbackQuery": "short relaxed query or null",
                  "reason": "one short sentence"
                }

                Rules:
                - Understand INR budgets such as 1000, 30k, 1 lakh, 10 thousand.
                - Use only provided category or brand slugs when you set those fields.
                - Keep "query" short and searchable, not a full sentence.
                - If the budget looks too strict, keep the intent but suggest a relaxed fallbackQuery.
                - Prefer null instead of guessing.
                """.formatted(
                abbreviate(categories, 24),
                abbreviate(brands, 30),
                abbreviate(tags, 40)
        );

        return executeJsonPrompt(searchModel, prompt, rawText)
                .flatMap(this::parseSearchInference);
    }

    public Optional<ImageInference> analyzeProductImage(byte[] fileBytes,
                                                        String contentType,
                                                        String fileName,
                                                        String hint,
                                                        List<String> categories,
                                                        List<String> brands,
                                                        List<String> tags) {
        if (!isConfigured() || fileBytes == null || fileBytes.length == 0) {
            return Optional.empty();
        }

        StringBuilder text = new StringBuilder("""
                Look at this shopper-provided image and describe it for a live ecommerce catalog search.

                Known category slugs: %s
                Known brand slugs: %s
                Known tags: %s

                Return JSON only:
                {
                  "category": "known category slug or null",
                  "brand": "known brand slug or null",
                  "tags": ["known tag or descriptor"],
                  "colors": ["plain color words"],
                  "searchHint": "short search phrase",
                  "confidence": "high|guided|fallback",
                  "reason": "one short sentence"
                }

                Rules:
                - Prefer the closest category and brand from the provided lists.
                - Keep searchHint short, concrete, and searchable.
                - Use tags only when they look visually plausible.
                - Do not invent unsupported details.
                """.formatted(
                abbreviate(categories, 24),
                abbreviate(brands, 30),
                abbreviate(tags, 40)
        ));
        if (hint != null && !hint.isBlank()) {
            text.append("\nShopper hint: ").append(hint.trim());
        }
        return executeImagePrompt(visionModel, text.toString(), contentType, fileBytes)
                .flatMap(this::parseImageInference);
    }

    private Optional<JsonNode> executeJsonPrompt(String model, String instruction, String userText) {
        List<Object> input = new ArrayList<>();
        input.add(message("system", List.of(textContent(instruction))));
        input.add(message("user", List.of(textContent(userText))));
        return executeRequest(model, input, 500);
    }

    private Optional<JsonNode> executeImagePrompt(String model, String instruction, String contentType, byte[] fileBytes) {
        String mimeType = (contentType == null || contentType.isBlank()) ? "image/jpeg" : contentType.trim();
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
        List<Object> input = new ArrayList<>();
        input.add(message("system", List.of(textContent("Return JSON only. Keep the output concise and factual."))));
        input.add(message("user", List.of(
                textContent(instruction),
                imageContent(dataUrl)
        )));
        return executeRequest(model, input, 500);
    }

    private Optional<JsonNode> executeRequest(String model, List<Object> input, int maxOutputTokens) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("max_output_tokens", maxOutputTokens);

        try {
            JsonNode root = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(REQUEST_TIMEOUT);
            if (root == null) {
                return Optional.empty();
            }
            String outputText = extractOutputText(root);
            if (outputText == null || outputText.isBlank()) {
                return Optional.empty();
            }
            String normalized = stripCodeFence(outputText);
            return Optional.of(objectMapper.readTree(normalized));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<SearchInference> parseSearchInference(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(new SearchInference(
                textValue(node, "query"),
                textValue(node, "category"),
                textValue(node, "brand"),
                textValue(node, "tag"),
                intValue(node, "minPriceRupees"),
                intValue(node, "maxPriceRupees"),
                booleanValue(node, "inStock"),
                textValue(node, "sort"),
                textValue(node, "fallbackQuery"),
                textValue(node, "reason")
        ));
    }

    private Optional<ImageInference> parseImageInference(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Optional.empty();
        }
        return Optional.of(new ImageInference(
                textValue(node, "category"),
                textValue(node, "brand"),
                stringArray(node, "tags"),
                stringArray(node, "colors"),
                textValue(node, "searchHint"),
                textValue(node, "confidence"),
                textValue(node, "reason")
        ));
    }

    private Object message(String role, List<Object> content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Object textContent(String text) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "input_text");
        content.put("text", text);
        return content;
    }

    private Object imageContent(String dataUrl) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "input_image");
        content.put("image_url", dataUrl);
        return content;
    }

    private String extractOutputText(JsonNode root) {
        String outputText = textValue(root, "output_text");
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        JsonNode outputs = root.path("output");
        if (!outputs.isArray()) {
            return null;
        }
        for (JsonNode output : outputs) {
            JsonNode contents = output.path("content");
            if (!contents.isArray()) {
                continue;
            }
            for (JsonNode content : contents) {
                JsonNode text = content.get("text");
                if (text == null || text.isNull()) {
                    continue;
                }
                if (text.isTextual()) {
                    return text.asText();
                }
                String value = textValue(text, "value");
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        String withoutFence = trimmed.replaceFirst("^```[a-zA-Z0-9_-]*", "").trim();
        if (withoutFence.endsWith("```")) {
            withoutFence = withoutFence.substring(0, withoutFence.length() - 3).trim();
        }
        return withoutFence;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer intValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asText("").replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String normalized = value.asText("").trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return null;
    }

    private List<String> stringArray(JsonNode node, String fieldName) {
        JsonNode array = node.path(fieldName);
        if (!array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String abbreviate(List<String> values, int maxItems) {
        if (values == null || values.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", values.stream().limit(maxItems).toList());
    }

    public record SearchInference(String query,
                                  String category,
                                  String brand,
                                  String tag,
                                  Integer minPriceRupees,
                                  Integer maxPriceRupees,
                                  Boolean inStock,
                                  String sort,
                                  String fallbackQuery,
                                  String reason) {
    }

    public record ImageInference(String category,
                                 String brand,
                                 List<String> tags,
                                 List<String> colors,
                                 String searchHint,
                                 String confidence,
                                 String reason) {
    }
}
