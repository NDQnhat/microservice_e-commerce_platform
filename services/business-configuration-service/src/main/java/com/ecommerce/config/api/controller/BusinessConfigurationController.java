package com.ecommerce.config.api.controller;

import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;
import com.ecommerce.config.security.UserPrincipal;
import com.ecommerce.config.service.BusinessConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/backoffice/configurations", "/api/v1/configurations"})
@Tag(name = "Business Configuration", description = "Dynamic business parameter management with audit and versioning (API-CFG-001, FR-037, BR-019)")
public class BusinessConfigurationController {

    private final BusinessConfigurationService configurationService;

    public BusinessConfigurationController(BusinessConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @Operation(summary = "List all active configurations")
    public ResponseEntity<List<BusinessConfigurationDto>> listActiveConfigurations() {
        List<BusinessConfigurationDto> configs = configurationService.listActiveConfigurations();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "Get active configuration by key")
    public ResponseEntity<BusinessConfigurationDto> getActiveConfiguration(@PathVariable String configKey) {
        BusinessConfigurationDto config = configurationService.getActiveConfiguration(configKey);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{configKey}/history")
    @Operation(summary = "Get configuration version history by key")
    public ResponseEntity<List<BusinessConfigurationDto>> getConfigurationHistory(@PathVariable String configKey) {
        List<BusinessConfigurationDto> history = configurationService.getConfigurationHistory(configKey);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create or update business configuration with versioning (BR-019)")
    public ResponseEntity<BusinessConfigurationDto> updateConfiguration(
            @Valid @RequestBody UpdateConfigurationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        String actorId = resolveActorId(userIdHeader);
        BusinessConfigurationDto saved = configurationService.updateConfiguration(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private String resolveActorId(String userIdHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getId() != null) {
                return principal.getId().toString();
            }
            if (principal.getUsername() != null && !principal.getUsername().isBlank()) {
                return principal.getUsername();
            }
        }
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader;
        }
        return "SYSTEM";
    }
}
