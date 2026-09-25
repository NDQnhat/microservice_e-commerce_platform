package com.ecommerce.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    private String code;

    @com.fasterxml.jackson.annotation.JsonProperty("violated_rule")
    private String violatedRule;

    @com.fasterxml.jackson.annotation.JsonProperty("correlation_id")
    private String correlationId;

    private Instant timestamp;
    private Map<String, List<String>> errors;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(String type, String title, int status, String detail, String instance,
                            String code, String violatedRule, String correlationId,
                            Map<String, List<String>> errors) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.code = code;
        this.violatedRule = violatedRule;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
        this.errors = errors;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getViolatedRule() {
        return violatedRule;
    }

    public void setViolatedRule(String violatedRule) {
        this.violatedRule = violatedRule;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, List<String>> errors) {
        this.errors = errors;
    }
}
