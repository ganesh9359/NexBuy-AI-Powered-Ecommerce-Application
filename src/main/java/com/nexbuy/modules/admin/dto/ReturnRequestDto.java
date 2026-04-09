package com.nexbuy.modules.admin.dto;

import java.time.LocalDateTime;

public class ReturnRequestDto {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String orderNumber;
    private String status; // requested, approved, accepted, rejected, completed, cancelled
    private String refundStatus; // not_started, pending, processing, processed, failed, cancelled
    private String reason;
    private Double refundAmountInr;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime pickedAt;
    private LocalDateTime updatedAt;

    public ReturnRequestDto(Long id, Long orderId, Long customerId, String orderNumber, String status,
                            String refundStatus, String reason, Double refundAmountInr,
                            LocalDateTime requestedAt, LocalDateTime reviewedAt, LocalDateTime pickedAt,
                            LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.refundStatus = refundStatus;
        this.reason = reason;
        this.refundAmountInr = refundAmountInr;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
        this.pickedAt = pickedAt;
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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getRefundAmountInr() {
        return refundAmountInr;
    }

    public void setRefundAmountInr(Double refundAmountInr) {
        this.refundAmountInr = refundAmountInr;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getPickedAt() {
        return pickedAt;
    }

    public void setPickedAt(LocalDateTime pickedAt) {
        this.pickedAt = pickedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
