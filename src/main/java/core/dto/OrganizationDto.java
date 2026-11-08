package core.dto;

import core.model.Organization;
import core.model.OrganizationType;

import java.time.LocalDateTime;

public record OrganizationDto(Long id,
                              String name,
                              CoordinatesDto coordinates,
                              LocalDateTime creationDate,
                              AddressDto officialAddress,
                              Float annualTurnover,
                              int employeesCount,
                              Long rating,
                              String fullName,
                              OrganizationType type,
                              AddressDto postalAddress,
                              Long ownerId) {

    public static OrganizationDto from(Organization o) {
        return new OrganizationDto(
                o.getId(),
                o.getName(),
                CoordinatesDto.from(o.getCoordinates()),
                o.getCreationDate(),
                AddressDto.from(o.getOfficialAddress()),
                o.getAnnualTurnover(),
                o.getEmployeesCount(),
                o.getRating(),
                o.getFullName(),
                o.getType(),
                AddressDto.from(o.getPostalAddress()),
                o.getOwnerId());
    }
}
