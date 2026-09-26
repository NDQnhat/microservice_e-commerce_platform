package com.ecommerce.config.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;
import com.ecommerce.config.domain.model.BusinessConfiguration;
import com.ecommerce.config.domain.repository.BusinessConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessConfigurationServiceTest {

    @Mock
    private BusinessConfigurationRepository configurationRepository;

    @Mock
    private BusinessConfigurationOutboxService outboxService;

    @InjectMocks
    private BusinessConfigurationServiceImpl configurationService;

    private BusinessConfiguration sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = BusinessConfiguration.createNew(
                "RESERVATION_TTL_MINUTES",
                "15",
                "Inventory reservation TTL",
                "SUPER_ADMIN"
        );
    }

    @Test
    @DisplayName("listActiveConfigurations: returns active configurations as DTOs")
    void shouldListActiveConfigurations() {
        when(configurationRepository.findByIsActiveTrue()).thenReturn(List.of(sampleConfig));

        List<BusinessConfigurationDto> result = configurationService.listActiveConfigurations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).configKey()).isEqualTo("RESERVATION_TTL_MINUTES");
        assertThat(result.get(0).configValue()).isEqualTo("15");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("getActiveConfiguration: returns active DTO when found by key")
    void shouldGetActiveConfigurationWhenFound() {
        when(configurationRepository.findActiveByKey("RESERVATION_TTL_MINUTES"))
                .thenReturn(Optional.of(sampleConfig));

        BusinessConfigurationDto dto = configurationService.getActiveConfiguration("RESERVATION_TTL_MINUTES");

        assertThat(dto).isNotNull();
        assertThat(dto.configKey()).isEqualTo("RESERVATION_TTL_MINUTES");
        assertThat(dto.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("getActiveConfiguration: throws NotFoundException when active config does not exist")
    void shouldThrowNotFoundWhenNotPresent() {
        when(configurationRepository.findActiveByKey("NON_EXISTENT_KEY"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> configurationService.getActiveConfiguration("NON_EXISTENT_KEY"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Active configuration not found");
    }

    @Test
    @DisplayName("getConfigurationHistory: returns version history ordered descending")
    void shouldGetConfigurationHistory() {
        Instant now = Instant.now();
        BusinessConfiguration v1 = new BusinessConfiguration(
                UUID.randomUUID(), "KEY", "V1", "Desc", 1, false, now.minusSeconds(3600), now, "USER1", now, now
        );
        BusinessConfiguration v2 = new BusinessConfiguration(
                UUID.randomUUID(), "KEY", "V2", "Desc", 2, true, now, null, "USER2", now, now
        );

        when(configurationRepository.findByConfigKeyOrderByVersionDesc("KEY"))
                .thenReturn(List.of(v2, v1));

        List<BusinessConfigurationDto> history = configurationService.getConfigurationHistory("KEY");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).version()).isEqualTo(2);
        assertThat(history.get(1).version()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateConfiguration: creates version 1 when no active config exists (BR-019)")
    void shouldCreateVersion1WhenConfigDoesNotExist() {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "NEW_FEATURE_FLAG",
                "true",
                "Enable experimental feature",
                "New release deployment"
        );

        when(configurationRepository.findActiveByKey("NEW_FEATURE_FLAG")).thenReturn(Optional.empty());
        when(configurationRepository.save(any(BusinessConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessConfigurationDto result = configurationService.updateConfiguration(request, "SUPER_ADMIN_1");

        assertThat(result).isNotNull();
        assertThat(result.configKey()).isEqualTo("NEW_FEATURE_FLAG");
        assertThat(result.configValue()).isEqualTo("true");
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.isActive()).isTrue();
        assertThat(result.createdBy()).isEqualTo("SUPER_ADMIN_1");

        verify(outboxService).recordConfigChanged(
                eq("NEW_FEATURE_FLAG"),
                isNull(),
                eq("true"),
                eq(1),
                any(Instant.class),
                eq("SUPER_ADMIN_1"),
                eq("New release deployment")
        );
    }

    @Test
    @DisplayName("updateConfiguration: deprecates active version and creates next version (BR-019, NFR-CONFIG-001)")
    void shouldDeprecateActiveAndCreateNextVersion() {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "RESERVATION_TTL_MINUTES",
                "30",
                "Extended TTL",
                "Peak load adjustment"
        );

        when(configurationRepository.findActiveByKey("RESERVATION_TTL_MINUTES"))
                .thenReturn(Optional.of(sampleConfig));
        when(configurationRepository.save(any(BusinessConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessConfigurationDto result = configurationService.updateConfiguration(request, "SUPER_ADMIN_2");

        assertThat(result).isNotNull();
        assertThat(result.configKey()).isEqualTo("RESERVATION_TTL_MINUTES");
        assertThat(result.configValue()).isEqualTo("30");
        assertThat(result.version()).isEqualTo(2);
        assertThat(result.isActive()).isTrue();
        assertThat(result.createdBy()).isEqualTo("SUPER_ADMIN_2");

        // Verify sampleConfig (v1) was deprecated
        assertThat(sampleConfig.getIsActive()).isFalse();
        assertThat(sampleConfig.getEffectiveTo()).isNotNull();

        // Verify two save invocations: one for deprecated v1, one for new v2
        ArgumentCaptor<BusinessConfiguration> captor = ArgumentCaptor.forClass(BusinessConfiguration.class);
        verify(configurationRepository, times(2)).save(captor.capture());
        List<BusinessConfiguration> saved = captor.getAllValues();
        assertThat(saved.get(0).getIsActive()).isFalse();
        assertThat(saved.get(1).getIsActive()).isTrue();
        assertThat(saved.get(1).getVersion()).isEqualTo(2);

        // Verify outbox emission
        verify(outboxService).recordConfigChanged(
                eq("RESERVATION_TTL_MINUTES"),
                eq("15"),
                eq("30"),
                eq(2),
                any(Instant.class),
                eq("SUPER_ADMIN_2"),
                eq("Peak load adjustment")
        );
    }
}
