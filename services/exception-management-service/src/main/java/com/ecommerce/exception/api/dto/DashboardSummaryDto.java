package com.ecommerce.exception.api.dto;

import java.util.Map;

public record DashboardSummaryDto(
        long totalOpenExceptions,
        long totalInvestigatingExceptions,
        long totalResolvedExceptions,
        Map<String, Long> openExceptionsByType
) {
}
