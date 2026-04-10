package com.nexbuy.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        
        for (ProductStatus status : ProductStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown ProductStatus value: " + value);
    }
}