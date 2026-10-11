package core.dto;

import core.model.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrganizationRequest(@NotBlank(message = "name не может быть пустым") String name,
                                  @NotNull(message = "coordinates обязательное поле") Long coordinatesId,
                                  Long officialAddressId,
                                  @Positive(message = "annualTurnover должно быть больше 0") Float annualTurnover,
                                  @NotNull(message = "employeesCount обязательное поле") @Positive(message = "employeesCount должно быть больше 0") Integer employeesCount,
                                  @Positive(message = "rating должно быть больше 0") Long rating, String fullName,
                                  OrganizationType type,
                                  @NotNull(message = "postalAddress обязательное поле") Long postalAddressId) {
}
