package com.nexbuy.modules.admin.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminDashboardDto {
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long newOrders;
    private long deliveredOrders;
    private Map<String, Long> orderStatusCounts = new LinkedHashMap<>();

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getNewOrders() {
        return newOrders;
    }

    public void setNewOrders(long newOrders) {
        this.newOrders = newOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public Map<String, Long> getOrderStatusCounts() {
        return orderStatusCounts;
    }

    public void setOrderStatusCounts(Map<String, Long> orderStatusCounts) {
        this.orderStatusCounts = orderStatusCounts;
    }
}