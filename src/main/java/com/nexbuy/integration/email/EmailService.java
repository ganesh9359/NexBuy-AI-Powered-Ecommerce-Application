package com.nexbuy.integration.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    public void sendOtp(String to, String code, String purpose) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "Use this secure code to complete your NexBuy " + purposeLabel(purpose) + ".");
        vars.put("eyebrow", "SECURE " + purposeLabel(purpose).toUpperCase());
        vars.put("headline", "Your NexBuy verification code");
        vars.put("summary", "Use this one-time password to complete your " + purposeLabel(purpose) + ".");
        vars.put("purposeLabel", purposeLabel(purpose));
        vars.put("otpCode", defaultString(code));
        vars.put("footerNote", "This code expires in 10 minutes. If you did not request it, you can safely ignore this email.");
        vars.put("ctaLabel", "Open NexBuy");
        vars.put("ctaUrl", frontendUrl);
        sendHtml(to, "Your NexBuy OTP", "templates/email/otp.html", vars, Collections.emptyMap());
    }

    public void sendWelcome(String to, String name) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "Welcome to NexBuy. Your account is active and ready.");
        vars.put("eyebrow", "WELCOME TO NEXBUY");
        vars.put("headline", "Welcome aboard, " + nameOrFallback(name));
        vars.put("summary", "Your account is active and ready for a faster, smarter shopping journey.");
        vars.put("bodyLineOne", "Discover personalized recommendations, save carts across devices, and move through checkout with less friction.");
        vars.put("bodyLineTwo", "We are excited to help you find better deals, better picks, and better shopping moments every day.");
        vars.put("ctaLabel", "Start shopping");
        vars.put("ctaUrl", frontendUrl);
        sendHtml(to, "Welcome to NexBuy", "templates/email/welcome.html", vars, Collections.emptyMap());
    }

    public void sendPasswordResetSuccess(String to, String name) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "Your NexBuy password was changed successfully.");
        vars.put("eyebrow", "ACCOUNT SECURITY");
        vars.put("headline", "Password updated successfully");
        vars.put("summary", "Hi " + nameOrFallback(name) + ", your NexBuy password has been changed.");
        vars.put("bodyLineOne", "You can now sign in using your new password and continue shopping right where you left off.");
        vars.put("bodyLineTwo", "If you did not make this change, contact our support team immediately so we can help secure your account.");
        vars.put("ctaLabel", "Sign in to NexBuy");
        vars.put("ctaUrl", frontendUrl + "/auth/login");
        sendHtml(to, "Your NexBuy password was updated", "templates/email/password-reset-success.html", vars, Collections.emptyMap());
    }

    public void sendOfferNotification(String to, String offerTitle, String description, String ctaUrl) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "A new NexBuy offer is live for you.");
        vars.put("eyebrow", "LIMITED-TIME OFFER");
        vars.put("headline", defaultString(offerTitle));
        vars.put("summary", defaultString(description));
        vars.put("bodyLineOne", "Fresh deals just landed, and this one is worth a closer look.");
        vars.put("bodyLineTwo", "Tap below to open the offer and explore the latest savings curated for you.");
        vars.put("ctaLabel", "View offer");
        vars.put("ctaUrl", isBlank(ctaUrl) ? frontendUrl : ctaUrl);
        sendHtml(to, "New offer from NexBuy", "templates/email/offer.html", vars, Collections.emptyMap());
    }

    public void sendSubscriptionConfirmation(String to) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "You are subscribed to NexBuy updates.");
        vars.put("eyebrow", "SUBSCRIPTION CONFIRMED");
        vars.put("headline", "You are in for the best of NexBuy");
        vars.put("summary", "You will now receive offer alerts, launch updates, and smart shopping picks from us.");
        vars.put("bodyLineOne", "We keep our updates focused on deals, drops, and product moments that are actually worth your time.");
        vars.put("bodyLineTwo", "Whenever a strong offer lands, you will be among the first to know.");
        vars.put("ctaLabel", "Browse deals");
        vars.put("ctaUrl", frontendUrl);
        sendHtml(to, "You are subscribed to NexBuy offers", "templates/email/subscription.html", vars, Collections.emptyMap());
    }

    public void sendOrderReceipt(String to, String orderNumber, int totalCents, List<String> itemLines) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", "Your NexBuy order is confirmed.");
        vars.put("eyebrow", "ORDER CONFIRMED");
        vars.put("headline", "Thanks for shopping with NexBuy");
        vars.put("summary", "Your order has been confirmed and we are getting it ready.");
        vars.put("orderNumber", defaultString(orderNumber));
        vars.put("totalAmount", String.format("INR %.2f", totalCents / 100.0));
        vars.put("placedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        vars.put("ctaLabel", "Track your order");
        vars.put("ctaUrl", frontendUrl + "/order");

        Map<String, String> rawVars = new LinkedHashMap<>();
        rawVars.put("itemsHtml", buildItemsHtml(itemLines));
        sendHtml(to, "Your NexBuy order " + defaultString(orderNumber), "templates/email/order-receipt.html", vars, rawVars);
    }

    public void sendOrderCancelled(String to, String orderNumber, boolean refundExpected, Integer refundAmountCents) {
        sendOrderUpdate(
                to,
                "Your NexBuy order was cancelled",
                "ORDER CANCELLED",
                "Your order has been cancelled",
                refundExpected
                        ? "We cancelled your order and started the refund process for the paid amount."
                        : "We cancelled your order before shipment and no further action is needed from you.",
                refundExpected
                        ? "Open the order detail page to track the refund progress from request to completion."
                        : "You can return to the catalog at any time and place a fresh order.",
                refundExpected ? "Refund amount" : "Order number",
                refundExpected ? formatAmount(refundAmountCents) : defaultString(orderNumber),
                "View order",
                frontendUrl + "/order"
        );
    }

    public void sendOrderDelivered(String to, String orderNumber) {
        sendOrderUpdate(
                to,
                "Your NexBuy order was delivered",
                "ORDER DELIVERED",
                "Your order has been delivered",
                "The shipment was marked as delivered. If something is not right, you can open the order detail page and start a return request.",
                "We hope everything reached you in great shape.",
                "Order number",
                defaultString(orderNumber),
                "Open order",
                frontendUrl + "/order"
        );
    }

    public void sendRefundUpdate(String to, String orderNumber, int amountCents, String refundStatus) {
        String normalized = defaultString(refundStatus).trim().toLowerCase();
        String headline = switch (normalized) {
            case "processed", "refunded" -> "Your refund has been completed";
            case "failed" -> "Your refund needs attention";
            default -> "Your refund is in progress";
        };
        String summary = switch (normalized) {
            case "processed", "refunded" -> "The paid amount has been sent back to your original payment method.";
            case "failed" -> "We could not complete the refund automatically. Our team will help finish it.";
            default -> "We started the refund and the payment provider is processing it now.";
        };
        sendOrderUpdate(
                to,
                "Refund update for your NexBuy order",
                "REFUND UPDATE",
                headline,
                summary,
                "Open the order detail page anytime to see the latest refund progress.",
                "Refund amount",
                String.format("INR %.2f", amountCents / 100.0),
                "View order",
                frontendUrl + "/order"
        );
    }

    public void sendReturnRequested(String to, String orderNumber) {
        sendOrderUpdate(
                to,
                "We received your NexBuy return request",
                "RETURN REQUESTED",
                "Your return request is recorded",
                "We have saved your return request and it now appears in the order timeline.",
                "Once the return is reviewed, any refund progress will show inside the same order detail page.",
                "Order number",
                defaultString(orderNumber),
                "View order",
                frontendUrl + "/order"
        );
    }

    private void sendOrderUpdate(String to,
                                 String subject,
                                 String eyebrow,
                                 String headline,
                                 String summary,
                                 String body,
                                 String highlightLabel,
                                 String highlightValue,
                                 String ctaLabel,
                                 String ctaUrl) {
        Map<String, String> vars = baseVars();
        vars.put("preheader", summary);
        vars.put("eyebrow", eyebrow);
        vars.put("headline", headline);
        vars.put("summary", summary);
        vars.put("bodyLine", body);
        vars.put("highlightLabel", highlightLabel);
        vars.put("highlightValue", highlightValue);
        vars.put("ctaLabel", ctaLabel);
        vars.put("ctaUrl", ctaUrl);
        sendHtml(to, subject, "templates/email/order-update.html", vars, Collections.emptyMap());
    }

    private void sendHtml(String to, String subject, String templatePath, Map<String, String> variables, Map<String, String> rawVariables) {
        try {
            String html = renderTemplate(templatePath, variables, rawVariables);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (!isBlank(fromAddress)) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject {}", to, subject);
        } catch (Exception ex) {
            log.warn("Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private String renderTemplate(String templatePath, Map<String, String> variables, Map<String, String> rawVariables) throws IOException {
        String template = StreamUtils.copyToString(
                new ClassPathResource(templatePath).getInputStream(),
                StandardCharsets.UTF_8
        );

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", HtmlUtils.htmlEscape(defaultString(entry.getValue())));
        }
        for (Map.Entry<String, String> entry : rawVariables.entrySet()) {
            template = template.replace("{{{" + entry.getKey() + "}}}", defaultString(entry.getValue()));
        }
        return template;
    }

    private Map<String, String> baseVars() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("brandName", "NexBuy");
        vars.put("brandTagline", "Smart shopping, made sharper.");
        vars.put("supportEmail", isBlank(fromAddress) ? "support@nexbuy.com" : fromAddress);
        vars.put("frontendUrl", frontendUrl);
        vars.put("currentYear", String.valueOf(LocalDateTime.now().getYear()));
        return vars;
    }

    private String buildItemsHtml(List<String> itemLines) {
        if (itemLines == null || itemLines.isEmpty()) {
            return "<tr><td style=\"padding:14px 0;color:#475569;font-size:14px;\">Your order items will appear here shortly.</td></tr>";
        }

        StringBuilder itemsHtml = new StringBuilder();
        for (String item : itemLines) {
            itemsHtml.append("<tr><td style=\"padding:14px 0;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:14px;line-height:1.6;\">")
                    .append(HtmlUtils.htmlEscape(defaultString(item)))
                    .append("</td></tr>");
        }
        return itemsHtml.toString();
    }

    private String formatAmount(Integer amountCents) {
        int cents = amountCents == null ? 0 : amountCents;
        return String.format("INR %.2f", cents / 100.0);
    }

    private String purposeLabel(String purpose) {
        if ("register".equalsIgnoreCase(purpose)) {
            return "account verification";
        }
        if ("reset".equalsIgnoreCase(purpose)) {
            return "password reset";
        }
        if ("login".equalsIgnoreCase(purpose)) {
            return "sign in";
        }
        return "verification";
    }

    private String nameOrFallback(String name) {
        return isBlank(name) ? "there" : name;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
