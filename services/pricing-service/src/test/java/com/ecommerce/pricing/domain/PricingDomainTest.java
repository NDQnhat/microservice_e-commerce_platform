package com.ecommerce.pricing.domain;

import com.ecommerce.pricing.domain.model.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PricingDomainTest {

    @Test
    @DisplayName("New price has no effective_to date, representing active state")
    void newPriceShouldBeActive() {
        UUID skuId = UUID.randomUUID();
        Price price = new Price(skuId, new BigDecimal("299000.00"), "VND");

        assertThat(price.getId()).isNotNull();
        assertThat(price.getBasePrice()).isEqualTo(new BigDecimal("299000.00"));
        assertThat(price.getCurrency()).isEqualTo("VND");
        assertThat(price.getEffectiveTo()).isNull();
    }

    @Test
    @DisplayName("Superseding price closes effective_to date")
    void supersedingPriceShouldCloseOldPrice() {
        UUID skuId = UUID.randomUUID();
        Price oldPrice = new Price(skuId, new BigDecimal("299000.00"), "VND");
        Instant closedAt = Instant.now();
        oldPrice.setEffectiveTo(closedAt);

        assertThat(oldPrice.getEffectiveTo()).isEqualTo(closedAt);
    }
}
