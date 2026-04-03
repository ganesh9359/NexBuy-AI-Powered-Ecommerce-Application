package com.nexbuy.modules.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminForecastDto {
    private Overview overview;
    private List<ForecastPoint> revenueTrend = new ArrayList<>();
    private List<ForecastPoint> orderTrend = new ArrayList<>();
    private List<CategoryForecast> categoryDemand = new ArrayList<>();
    private List<InventoryRisk> inventoryRisks = new ArrayList<>();
    private List<String> actions = new ArrayList<>();

    public Overview getOverview() {
        return overview;
    }

    public void setOverview(Overview overview) {
        this.overview = overview;
    }

    public List<ForecastPoint> getRevenueTrend() {
        return revenueTrend;
    }

    public void setRevenueTrend(List<ForecastPoint> revenueTrend) {
        this.revenueTrend = revenueTrend;
    }

    public List<ForecastPoint> getOrderTrend() {
        return orderTrend;
    }

    public void setOrderTrend(List<ForecastPoint> orderTrend) {
        this.orderTrend = orderTrend;
    }

    public List<CategoryForecast> getCategoryDemand() {
        return categoryDemand;
    }

    public void setCategoryDemand(List<CategoryForecast> categoryDemand) {
        this.categoryDemand = categoryDemand;
    }

    public List<InventoryRisk> getInventoryRisks() {
        return inventoryRisks;
    }

    public void setInventoryRisks(List<InventoryRisk> inventoryRisks) {
        this.inventoryRisks = inventoryRisks;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public record Overview(long trailingRevenueCents,
                           long trailingOrders,
                           long projectedRevenueCents,
                           long projectedOrders,
                           double revenueGrowthRate,
                           long pendingOrders,
                           long lowStockRiskCount) {
    }

    public record ForecastPoint(String label, long value) {
    }

    public record CategoryForecast(String category,
                                   String slug,
                                   long unitsSold,
                                   long projectedUnits,
                                   double trendRate) {
    }

    public record InventoryRisk(String title,
                                String slug,
                                int stockQty,
                                int lowStockThreshold,
                                long soldLast30Days,
                                String guidance) {
    }
}