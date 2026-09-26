package com.ecommerce.payment.client;

import com.ecommerce.common.context.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultExceptionClient implements ExceptionClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionClient.class);

    private final RestClient restClient;

    public DefaultExceptionClient(@Value("${services.exception.url:http://localhost:8092}") String exceptionUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(exceptionUrl)
                .build();
    }

    @Override
    public void createExceptionRecord(String exceptionType, String referenceId, String referenceType,
                                      String errorCode, String errorMessage, String payload) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("exceptionType", exceptionType);
            body.put("sourceService", "payment-service");
            body.put("referenceId", referenceId);
            body.put("referenceType", referenceType);
            body.put("errorCode", errorCode);
            body.put("errorMessage", errorMessage);
            body.put("payload", payload);

            restClient.post()
                    .uri("/api/v1/exceptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-Id", CorrelationContext.getCorrelationId())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully recorded exception record of type {} for ref {}: {}", exceptionType, referenceId, errorCode);
        } catch (Exception ex) {
            log.warn("Remote call to exception-management-service failed for {}: {}", exceptionType, ex.getMessage());
        }
    }
}
