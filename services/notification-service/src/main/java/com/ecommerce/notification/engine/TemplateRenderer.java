package com.ecommerce.notification.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)\\}\\}");

    /**
     * Renders a template string by replacing placeholders {{variable}} with values from parameters map.
     * Supports matching both snake_case and camelCase forms of standard parameters:
     * order_id / orderId, customer_id / customerId, tracking_code / trackingCode, carrier_name / carrierName.
     *
     * @param templateString the raw template containing placeholders
     * @param parameters map of parameter keys to values
     * @return rendered string
     */
    public String render(String templateString, Map<String, Object> parameters) {
        if (templateString == null) {
            return "";
        }
        if (parameters == null || parameters.isEmpty()) {
            // Replace any unresolved placeholder with empty string
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateString);
            return matcher.replaceAll("");
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateString);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = resolveValue(key, parameters);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object resolveValue(String key, Map<String, Object> parameters) {
        if (parameters.containsKey(key)) {
            return parameters.get(key);
        }

        // Try case-insensitive lookup
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }

        // Try snake_case to camelCase conversion
        String camelCaseKey = snakeToCamel(key);
        if (parameters.containsKey(camelCaseKey)) {
            return parameters.get(camelCaseKey);
        }

        // Try camelCase to snake_case conversion
        String snakeCaseKey = camelToSnake(key);
        if (parameters.containsKey(snakeCaseKey)) {
            return parameters.get(snakeCaseKey);
        }

        return null;
    }

    private String snakeToCamel(String str) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String camelToSnake(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
