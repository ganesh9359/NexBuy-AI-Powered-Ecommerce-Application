package com.nexbuy.enums;

public enum ProductStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}