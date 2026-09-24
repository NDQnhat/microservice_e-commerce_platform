package com.ecommerce.common.context;

public final class CorrelationContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlation_id";

    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {
    }

    public static String getCorrelationId() {
        return CURRENT_CORRELATION_ID.get();
    }

    public static void setCorrelationId(String correlationId) {
        CURRENT_CORRELATION_ID.set(correlationId);
    }

    public static void clear() {
        CURRENT_CORRELATION_ID.remove();
    }
}
