package core.dto;

import core.model.OrganizationType;
import jakarta.validation.constraints.NotNull;

public record OrganizationRequest(
        String name,
        Long coordinatesId,
        Long officialAddressId,
        Float annualTurnover,
        @NotNull(message = "employees count is required") Integer employeesCount,
        Long rating,
        String fullName,
        OrganizationType type,
        Long postalAddressId) {
}
