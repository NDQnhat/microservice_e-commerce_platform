package com.ecommerce.notification.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private TemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TemplateRenderer();
    }

    @Test
    @DisplayName("Substitutes all standard placeholders in template")
    void shouldSubstituteStandardPlaceholders() {
        String template = "Hello {{name}}, Order {{order_id}} for customer {{customer_id}} is {{status}}! Amount: {{amount}}. Tracking: {{tracking_code}} with {{carrier_name}}.";

        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        params.put("order_id", orderId.toString());
        params.put("customer_id", customerId.toString());
        params.put("status", "CONFIRMED");
        params.put("amount", "150.00");
        params.put("tracking_code", "TRK9999");
        params.put("carrier_name", "FedEx");

        String result = renderer.render(template, params);

        assertThat(result).contains("Hello Alice");
        assertThat(result).contains("Order " + orderId);
        assertThat(result).contains("for customer " + customerId);
        assertThat(result).contains("is CONFIRMED!");
        assertThat(result).contains("Amount: 150.00");
        assertThat(result).contains("Tracking: TRK9999 with FedEx.");
    }

    @Test
    @DisplayName("Supports camelCase keys when template uses snake_case placeholders")
    void shouldSupportCamelCaseKeys() {
        String template = "Order: {{order_id}}, Tracking: {{tracking_code}}, Carrier: {{carrier_name}}";

        Map<String, Object> params = new HashMap<>();
        params.put("orderId", "ORD-123");
        params.put("trackingCode", "TRK-456");
        params.put("carrierName", "DHL");

        String result = renderer.render(template, params);

        assertThat(result).isEqualTo("Order: ORD-123, Tracking: TRK-456, Carrier: DHL");
    }

    @Test
    @DisplayName("Missing parameters replaced with empty strings without exception")
    void shouldHandleMissingParametersGracefully() {
        String template = "Hello {{name}}, your order {{order_id}} is ready.";

        Map<String, Object> params = new HashMap<>();
        params.put("order_id", "ORD-888");

        String result = renderer.render(template, params);

        assertThat(result).isEqualTo("Hello , your order ORD-888 is ready.");
    }

    @Test
    @DisplayName("Null template string returns empty string")
    void shouldHandleNullTemplate() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");

        String result = renderer.render(null, params);

        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("Empty parameters replaces all placeholders with empty strings")
    void shouldHandleEmptyParameters() {
        String template = "Code {{code}} for {{user}}";

        String result = renderer.render(template, Map.of());

        assertThat(result).isEqualTo("Code  for ");
    }
}
