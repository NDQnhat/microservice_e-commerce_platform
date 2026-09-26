package com.ecommerce.config.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;
import com.ecommerce.config.domain.model.BusinessConfiguration;
import com.ecommerce.config.domain.repository.BusinessConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BusinessConfigurationServiceImpl implements BusinessConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(BusinessConfigurationServiceImpl.class);

    private final BusinessConfigurationRepository configurationRepository;
    private final BusinessConfigurationOutboxService outboxService;

    public BusinessConfigurationServiceImpl(
            BusinessConfigurationRepository configurationRepository,
            BusinessConfigurationOutboxService outboxService) {
        this.configurationRepository = configurationRepository;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessConfigurationDto> listActiveConfigurations() {
        return configurationRepository.findByIsActiveTrue()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessConfigurationDto getActiveConfiguration(String configKey) {
        BusinessConfiguration config = configurationRepository.findActiveByKey(configKey)
                .orElseThrow(() -> new NotFoundException("Active configuration not found for key: " + configKey));
        return toDto(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessConfigurationDto> getConfigurationHistory(String configKey) {
        return configurationRepository.findByConfigKeyOrderByVersionDesc(configKey)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BusinessConfigurationDto updateConfiguration(UpdateConfigurationRequest request, String actorId) {
        String resolvedActor = actorId != null && !actorId.isBlank() ? actorId : "SYSTEM";
        Optional<BusinessConfiguration> currentActiveOpt = configurationRepository.findActiveByKey(request.configKey());

        String beforeValue = null;
        int nextVersion = 1;
        Instant now = Instant.now();
        BusinessConfiguration savedNew;

        if (currentActiveOpt.isPresent()) {
            BusinessConfiguration current = currentActiveOpt.get();
            beforeValue = current.getConfigValue();
            nextVersion = current.getVersion() + 1;

            savedNew = current.createNextVersion(request.configValue(), request.description(), resolvedActor);
            configurationRepository.save(current);
            savedNew = configurationRepository.save(savedNew);
            log.info("Deprecating config [{}] v{} and creating v{}", request.configKey(), current.getVersion(), nextVersion);
        } else {
            savedNew = new BusinessConfiguration(
                    UUID.randomUUID(),
                    request.configKey(),
                    request.configValue(),
                    request.description(),
                    1,
                    true,
                    now,
                    null,
                    resolvedActor,
                    now,
                    now
            );
            savedNew = configurationRepository.save(savedNew);
            log.info("Creating initial config [{}] v1", request.configKey());
        }

        // Record outbox event within the same atomic database transaction (Internal Event Contracts Section 13)
        outboxService.recordConfigChanged(
                request.configKey(),
                beforeValue,
                request.configValue(),
                nextVersion,
                savedNew.getEffectiveFrom(),
                resolvedActor,
                request.reason()
        );

        return toDto(savedNew);
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
