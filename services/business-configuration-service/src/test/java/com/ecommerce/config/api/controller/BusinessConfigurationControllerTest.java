package com.ecommerce.config.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;
import com.ecommerce.config.service.BusinessConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessConfigurationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BusinessConfigurationService configurationService;

    @InjectMocks
    private BusinessConfigurationController controller;

    private BusinessConfigurationDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleDto = new BusinessConfigurationDto(
                UUID.randomUUID(),
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "15",
                "Cutoff window for order cancellation in minutes",
                1,
                true,
                Instant.now(),
                null,
                "SUPER_ADMIN",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/configurations: returns 200 OK with list of active configs")
    void shouldListActiveConfigurations() throws Exception {
        when(configurationService.listActiveConfigurations()).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/v1/backoffice/configurations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"))
                .andExpect(jsonPath("$[0].configValue").value("15"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/configurations: alias endpoint returns 200 OK")
    void shouldListActiveConfigurationsAlias() throws Exception {
        when(configurationService.listActiveConfigurations()).thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/v1/configurations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/configurations/{configKey}: returns 200 OK with active config")
    void shouldGetActiveConfigurationByKey() throws Exception {
        when(configurationService.getActiveConfiguration("ORDER_CANCELLATION_TIMEOUT_MINUTES"))
                .thenReturn(sampleDto);

        mockMvc.perform(get("/api/v1/backoffice/configurations/ORDER_CANCELLATION_TIMEOUT_MINUTES")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/configurations/{configKey}: returns 404 NOT_FOUND RFC 7807 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(configurationService.getActiveConfiguration("NON_EXISTENT"))
                .thenThrow(new NotFoundException("Active configuration not found for key: NON_EXISTENT"));

        mockMvc.perform(get("/api/v1/backoffice/configurations/NON_EXISTENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/configurations/{configKey}/history: returns 200 OK with version history")
    void shouldGetConfigurationHistory() throws Exception {
        when(configurationService.getConfigurationHistory("ORDER_CANCELLATION_TIMEOUT_MINUTES"))
                .thenReturn(List.of(sampleDto));

        mockMvc.perform(get("/api/v1/backoffice/configurations/ORDER_CANCELLATION_TIMEOUT_MINUTES/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/configurations: creates/updates config and returns 201 Created (API-CFG-001)")
    void shouldUpdateConfiguration() throws Exception {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "30",
                "Updated timeout window",
                "Super Admin policy update"
        );

        BusinessConfigurationDto updatedDto = new BusinessConfigurationDto(
                UUID.randomUUID(),
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "30",
                "Updated timeout window",
                2,
                true,
                Instant.now(),
                null,
                "SUPER_ADMIN_01",
                Instant.now(),
                Instant.now()
        );

        when(configurationService.updateConfiguration(any(UpdateConfigurationRequest.class), eq("SUPER_ADMIN_01")))
                .thenReturn(updatedDto);

        mockMvc.perform(post("/api/v1/backoffice/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "SUPER_ADMIN_01")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"))
                .andExpect(jsonPath("$.configValue").value("30"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/configurations: alias returns 201 Created")
    void shouldUpdateConfigurationAlias() throws Exception {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "RESERVATION_TTL_MINUTES",
                "20",
                "Updated TTL",
                "Maintenance change"
        );

        when(configurationService.updateConfiguration(any(UpdateConfigurationRequest.class), any()))
                .thenReturn(sampleDto);

        mockMvc.perform(post("/api/v1/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configKey").value("ORDER_CANCELLATION_TIMEOUT_MINUTES"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/configurations: returns 400 VALIDATION_ERROR on missing or blank fields")
    void shouldReturn400OnValidationError() throws Exception {
        UpdateConfigurationRequest invalidRequest = new UpdateConfigurationRequest(
                "", // Blank key
                "", // Blank value
                "Description",
                ""  // Blank reason (BR-019 required)
        );

        mockMvc.perform(post("/api/v1/backoffice/configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"));
    }
}
