package com.ecommerce.payment.client;

public interface ExceptionClient {
    void createExceptionRecord(String exceptionType, String referenceId, String referenceType, String errorCode, String errorMessage, String payload);
}
