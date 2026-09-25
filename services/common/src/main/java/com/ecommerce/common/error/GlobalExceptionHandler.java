package com.ecommerce.common.error;

import com.ecommerce.common.context.CorrelationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        Map<String, List<String>> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:validation-error",
                "Validation Error",
                HttpStatus.BAD_REQUEST.value(),
                "Input failed field-level validation",
                request.getRequestURI(),
                "VALIDATION_ERROR",
                null,
                CorrelationContext.getCorrelationId(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(BusinessRuleException ex,
                                                                        HttpServletRequest request) {
        log.warn("Business rule violation: {} - {}", ex.getRuleId(), ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:business-rule-violation",
                "Business Rule Violation",
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "BUSINESS_RULE_VIOLATION",
                ex.getRuleId(),
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(NotFoundException ex,
                                                                    HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:not-found",
                "Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "NOT_FOUND",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflictException(ConflictException ex,
                                                                    HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:conflict",
                "Conflict",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "CONFLICT",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStateException(InvalidStateException ex,
                                                                        HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:invalid-state",
                "Invalid State",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "INVALID_STATE",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DependencyException.class)
    public ResponseEntity<ApiErrorResponse> handleDependencyException(DependencyException ex,
                                                                      HttpServletRequest request) {
        log.error("Downstream dependency failure: {}", ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:dependency-error",
                "Dependency Error",
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "DEPENDENCY_ERROR",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailedException(AuthenticationFailedException ex,
                                                                                HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:authentication-failed",
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "AUTHENTICATION_FAILED",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthorizationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationFailedException(AuthorizationFailedException ex,
                                                                               HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:authorization-failed",
                "Authorization Failed",
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                request.getRequestURI(),
                "AUTHORIZATION_FAILED",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error("Unhandled internal exception: {}", ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                "urn:problem-type:internal-error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal error occurred",
                request.getRequestURI(),
                "INTERNAL_ERROR",
                null,
                CorrelationContext.getCorrelationId(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
