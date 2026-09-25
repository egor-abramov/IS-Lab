package core.dto;

import java.util.List;

public record OrganizationPageDto(List<OrganizationDto> items, long total, int page, int size) {
}
