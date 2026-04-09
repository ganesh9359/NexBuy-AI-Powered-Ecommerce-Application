package com.nexbuy.modules.admin.dto;

import java.time.LocalDateTime;

public class RefundStatusDto {
    private Long id;
    private Long orderId;
    private Long paymentId;
    private Integer amountCents;
    private String currency;
    private String status; // pending, processing, processed, failed, cancelled
    private String providerRefundId;
    private String note;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private LocalDateTime updatedAt;

    public RefundStatusDto(Long id, Long orderId, Long paymentId, Integer amountCents, String currency,
                          String status, String providerRefundId, String note,
                          LocalDateTime requestedAt, LocalDateTime processedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.providerRefundId = providerRefundId;
        this.note = note;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
