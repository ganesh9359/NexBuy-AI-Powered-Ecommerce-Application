package com.nexbuy.modules.payment.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexbuy.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentGatewayClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayBaseUrl;

    public PaymentGatewayClient(ObjectMapper objectMapper,
                                @Value("${app.payment.razorpay.key-id:}") String razorpayKeyId,
                                @Value("${app.payment.razorpay.key-secret:}") String razorpayKeySecret,
                                @Value("${app.payment.razorpay.base-url:https://api.razorpay.com}") String razorpayBaseUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        this.razorpayKeyId = normalizeBlank(razorpayKeyId);
        this.razorpayKeySecret = normalizeBlank(razorpayKeySecret);
        this.razorpayBaseUrl = stripTrailingSlash(razorpayBaseUrl == null ? "https://api.razorpay.com" : razorpayBaseUrl.trim());
    }

    public GatewayIntent createIntent(String provider, String orderNumber, int amountCents, String currency) {
        String normalized = normalizeProvider(provider);
        if ("razorpay".equals(normalized)) {
            ensureRazorpayConfigured();
            return createRazorpayIntent(orderNumber, amountCents, currency);
        }

        String providerOrderId = normalized.toUpperCase(Locale.ROOT) + "-ORDER-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
        String checkoutLabel = switch (normalized) {
            case "cod" -> "Cash on delivery";
            case "razorpay" -> "Razorpay checkout";
            default -> "Card payment";
        };
        boolean requiresAction = !"cod".equals(normalized);
        return new GatewayIntent(normalized, providerOrderId, amountCents, currency, checkoutLabel, requiresAction);
    }

    public GatewayRefund refund(String provider,
                                String providerPaymentId,
                                int amountCents,
                                String currency,
                                String receipt,
                                String note) {
        String normalized = normalizeProvider(provider);
        if ("razorpay".equals(normalized)) {
            ensureRazorpayConfigured();
            return refundRazorpayPayment(providerPaymentId, amountCents, currency, receipt, note);
        }
        if ("stripe".equals(normalized)) {
            return new GatewayRefund(
                    "STRIPE-RFND-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT),
                    "processed",
                    amountCents,
                    currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase(Locale.ROOT),
                    note
            );
        }
        throw new CustomException("Refund is not available for this payment provider", HttpStatus.BAD_REQUEST);
    }

    public boolean isProviderConfigured(String provider) {
        String normalized = normalizeProvider(provider);
        return switch (normalized) {
            case "razorpay" -> isRazorpayConfigured();
            case "stripe", "cod" -> true;
            default -> false;
        };
    }

    public String resolvePublicKey(String provider) {
        return "razorpay".equals(normalizeProvider(provider)) && isRazorpayConfigured() ? razorpayKeyId : null;
    }

    public RazorpayVerification verifyRazorpayPayment(String storedProviderOrderId,
                                                      String reportedProviderOrderId,
                                                      String providerPaymentId,
                                                      String signature,
                                                      int expectedAmountCents,
                                                      String expectedCurrency) {
        ensureRazorpayConfigured();
        String trustedOrderId = requireText(storedProviderOrderId, "Razorpay order id is missing on the order");
        String reportedOrderId = requireText(reportedProviderOrderId, "Razorpay did not return an order id");
        String paymentId = requireText(providerPaymentId, "Razorpay did not return a payment id");
        String returnedSignature = requireText(signature, "Razorpay did not return a payment signature");

        if (!trustedOrderId.equals(reportedOrderId)) {
            throw new CustomException("Razorpay order id mismatch", HttpStatus.BAD_REQUEST);
        }

        verifyCheckoutSignature(trustedOrderId, paymentId, returnedSignature);

        JsonNode payment = fetchRazorpayPayment(paymentId);
        String paymentOrderId = requiredText(payment, "order_id");
        if (!trustedOrderId.equals(paymentOrderId)) {
            throw new CustomException("Razorpay payment does not belong to this order", HttpStatus.BAD_REQUEST);
        }

        int amount = payment.path("amount").asInt(-1);
        if (amount != expectedAmountCents) {
            throw new CustomException("Razorpay payment amount did not match the order total", HttpStatus.BAD_REQUEST);
        }

        String currency = requiredText(payment, "currency");
        if (!currency.equalsIgnoreCase(expectedCurrency)) {
            throw new CustomException("Razorpay payment currency did not match the order", HttpStatus.BAD_REQUEST);
        }

        String status = normalizeProvider(requiredText(payment, "status"));
        if ("authorized".equals(status)) {
            payment = captureRazorpayPayment(paymentId, expectedAmountCents, expectedCurrency);
            status = normalizeProvider(requiredText(payment, "status"));
        }

        return new RazorpayVerification(trustedOrderId, paymentId, status);
    }

    private GatewayIntent createRazorpayIntent(String orderNumber, int amountCents, String currency) {
        String normalizedCurrency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase(Locale.ROOT);
        String receipt = orderNumber == null || orderNumber.isBlank()
                ? "NBY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
                : orderNumber;
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("nexbuyOrderNumber", receipt);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountCents);
        payload.put("currency", normalizedCurrency);
        payload.put("receipt", receipt);
        payload.put("notes", notes);

        JsonNode response = sendRazorpayRequest("POST", "/v1/orders", payload);

        return new GatewayIntent(
                "razorpay",
                requiredText(response, "id"),
                amountCents,
                normalizedCurrency,
                "Razorpay checkout",
                true
        );
    }

    private GatewayRefund refundRazorpayPayment(String providerPaymentId,
                                                int amountCents,
                                                String currency,
                                                String receipt,
                                                String note) {
        String paymentId = requireText(providerPaymentId, "Razorpay payment id is missing for refund");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountCents);
        payload.put("receipt", normalizeBlank(receipt));
        payload.put("speed", "normal");
        Map<String, Object> notes = new LinkedHashMap<>();
        if (normalizeBlank(note) != null) {
            notes.put("note", note.trim());
        }
        if (!notes.isEmpty()) {
            payload.put("notes", notes);
        }

        JsonNode response = sendRazorpayRequest("POST", "/v1/payments/" + urlEncode(paymentId) + "/refund", payload);
        return new GatewayRefund(
                requiredText(response, "id"),
                normalizeProvider(requiredText(response, "status")),
                response.path("amount").asInt(amountCents),
                response.path("currency").asText(currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase(Locale.ROOT)),
                note
        );
    }

    private JsonNode fetchRazorpayPayment(String paymentId) {
        return sendRazorpayRequest("GET", "/v1/payments/" + urlEncode(paymentId), null);
    }

    private JsonNode captureRazorpayPayment(String paymentId, int amountCents, String currency) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountCents);
        payload.put("currency", currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase(Locale.ROOT));
        return sendRazorpayRequest("POST", "/v1/payments/" + urlEncode(paymentId) + "/capture", payload);
    }

    private JsonNode sendRazorpayRequest(String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(razorpayBaseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", basicAuthorization())
                    .header("Accept", "application/json");

            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = parseJson(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return json;
            }

            String description = json.path("error").path("description").asText();
            String message = description == null || description.isBlank()
                    ? "Razorpay request failed with status " + response.statusCode()
                    : "Razorpay request failed: " + description;
            throw new CustomException(message, HttpStatus.BAD_GATEWAY);
        } catch (IOException ex) {
            throw new CustomException("Could not parse the Razorpay response", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException("The Razorpay request was interrupted", HttpStatus.BAD_GATEWAY);
        }
    }

    private void verifyCheckoutSignature(String providerOrderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = providerOrderId + "|" + paymentId;
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!expected.equalsIgnoreCase(signature)) {
                throw new CustomException("Razorpay signature verification failed", HttpStatus.BAD_REQUEST);
            }
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException("Could not verify the Razorpay signature", HttpStatus.BAD_REQUEST);
        }
    }

    private JsonNode parseJson(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new CustomException("Razorpay response is missing " + fieldName, HttpStatus.BAD_GATEWAY);
        }
        return value.asText();
    }

    private String requireText(String value, String message) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            throw new CustomException(message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void ensureRazorpayConfigured() {
        if (!isRazorpayConfigured()) {
            throw new CustomException("Razorpay is not configured on this server", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isRazorpayConfigured() {
        return razorpayKeyId != null && razorpayKeySecret != null;
    }

    private String basicAuthorization() {
        String token = razorpayKeyId + ":" + razorpayKeySecret;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stripTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record GatewayIntent(String provider,
                                String providerOrderId,
                                int amountCents,
                                String currency,
                                String checkoutLabel,
                                boolean requiresAction) {
    }

    public record GatewayRefund(String providerRefundId,
                                String status,
                                int amountCents,
                                String currency,
                                String note) {
    }

    public record RazorpayVerification(String providerOrderId,
                                       String providerPaymentId,
                                       String status) {
    }
}