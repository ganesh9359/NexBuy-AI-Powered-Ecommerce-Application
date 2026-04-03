package com.nexbuy.modules.admin.dto;

public class AdminProductStockRequest {
    private Integer stockQty;
    private Integer stockDelta;

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public Integer getStockDelta() {
        return stockDelta;
    }

    public void setStockDelta(Integer stockDelta) {
        this.stockDelta = stockDelta;
    }
}
