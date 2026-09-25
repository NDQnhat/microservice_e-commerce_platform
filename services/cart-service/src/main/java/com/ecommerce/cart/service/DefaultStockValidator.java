package com.ecommerce.cart.service;

import com.ecommerce.common.error.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultStockValidator implements StockValidator {

    private static final Logger log = LoggerFactory.getLogger(DefaultStockValidator.class);

    @Override
    public void validateStock(UUID skuId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new BusinessRuleException("BR-004", "Requested quantity must be positive: " + requestedQuantity);
        }
        log.debug("Stock validated for SKU: {} with quantity: {}", skuId, requestedQuantity);
    }
}
