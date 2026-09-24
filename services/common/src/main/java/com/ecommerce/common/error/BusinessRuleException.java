package com.ecommerce.common.error;

public class BusinessRuleException extends RuntimeException {

    private final String ruleId;

    public BusinessRuleException(String message) {
        this("BR-GENERAL", message);
    }

    public BusinessRuleException(String ruleId, String message) {
        super(ruleId != null ? "[" + ruleId + "] " + message : message);
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }
}
