package com.ecommerce.config.service;

import com.ecommerce.config.api.dto.BusinessConfigurationDto;
import com.ecommerce.config.api.dto.UpdateConfigurationRequest;

import java.util.List;

public interface BusinessConfigurationService {

    List<BusinessConfigurationDto> listActiveConfigurations();

    BusinessConfigurationDto getActiveConfiguration(String configKey);

    List<BusinessConfigurationDto> getConfigurationHistory(String configKey);

    BusinessConfigurationDto updateConfiguration(UpdateConfigurationRequest request, String actorId);
}
