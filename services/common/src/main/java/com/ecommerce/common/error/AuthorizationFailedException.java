package com.ecommerce.common.error;

public class AuthorizationFailedException extends RuntimeException {
    public AuthorizationFailedException(String message) {
        super(message);
    }
}
