package com.ecommerce.notification.api.dto;

import com.ecommerce.notification.domain.model.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateTemplateRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotNull(message = "Status is required")
    private TemplateStatus status;

    public UpdateTemplateRequest() {
    }

    public UpdateTemplateRequest(String subject, String bodyTemplate, TemplateStatus status) {
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
        this.status = status;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
    }
}
