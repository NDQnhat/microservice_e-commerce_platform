package com.ecommerce.config.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;
import com.ecommerce.config.domain.model.BusinessConfiguration;
import com.ecommerce.config.domain.model.OutboxEventRecord;
import com.ecommerce.config.domain.repository.BusinessConfigurationRepository;
import com.ecommerce.config.domain.repository.OutboxEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/configurations")
@Tag(name = "Business Configuration", description = "Dynamic business parameter management with audit and versioning (API-CFG-001, FR-037, BR-019)")
public class BusinessConfigurationController {

    private final BusinessConfigurationRepository configurationRepository;
    private final OutboxEventRepository outboxEventRepository;

    public BusinessConfigurationController(BusinessConfigurationRepository configurationRepository,
                                          OutboxEventRepository outboxEventRepository) {
        this.configurationRepository = configurationRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping
    @Operation(summary = "List all active configurations")
    public ResponseEntity<List<BusinessConfigurationDto>> listActiveConfigurations() {
        List<BusinessConfigurationDto> configs = configurationRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "Get active configuration by key")
    public ResponseEntity<BusinessConfigurationDto> getActiveConfiguration(@PathVariable String configKey) {
        BusinessConfiguration config = configurationRepository.findActiveByKey(configKey)
                .orElseThrow(() -> new NotFoundException("Active configuration not found for key: " + configKey));
        return ResponseEntity.ok(toDto(config));
    }

    @GetMapping("/{configKey}/history")
    @Operation(summary = "Get configuration version history by key")
    public ResponseEntity<List<BusinessConfigurationDto>> getConfigurationHistory(@PathVariable String configKey) {
        List<BusinessConfigurationDto> history = configurationRepository.findByConfigKeyOrderByVersionDesc(configKey)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create or update business configuration with versioning (BR-019)")
    public ResponseEntity<BusinessConfigurationDto> updateConfiguration(
            @Valid @RequestBody UpdateConfigurationRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {

        Optional<BusinessConfiguration> currentActiveOpt = configurationRepository.findActiveByKey(request.configKey());
        int nextVersion = 1;
        Instant now = Instant.now();

        if (currentActiveOpt.isPresent()) {
            BusinessConfiguration current = currentActiveOpt.get();
            current.deprecate(now);
            configurationRepository.save(current);
            nextVersion = current.getVersion() + 1;
        }

        BusinessConfiguration newVersion = new BusinessConfiguration(
                UUID.randomUUID(),
                request.configKey(),
                request.configValue(),
                request.description(),
                nextVersion,
                true,
                now,
                null,
                userId,
                now,
                now
        );
        BusinessConfiguration saved = configurationRepository.save(newVersion);

        // Outbox event record for distributed config synchronization
        String payload = String.format("{\"configKey\":\"%s\",\"version\":%d,\"updatedBy\":\"%s\",\"reason\":\"%s\"}",
                request.configKey(), nextVersion, userId, request.reason());
        OutboxEventRecord outbox = OutboxEventRecord.createPending("CONFIGURATION", saved.getId().toString(), "BUSINESS_CONFIGURATION_CHANGED", payload);
        outboxEventRepository.save(outbox);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    private BusinessConfigurationDto toDto(BusinessConfiguration config) {
        return new BusinessConfigurationDto(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription(),
                config.getVersion(),
                config.getIsActive(),
                config.getEffectiveFrom(),
                config.getEffectiveTo(),
                config.getCreatedBy(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
