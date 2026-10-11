package core.service;

import core.dto.AverageDto;
import core.dto.CountDto;
import core.dto.ItemsDto;
import core.dto.OrganizationDto;
import core.dto.RatingsDto;
import core.repository.SpecialOpsRepository;
import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpecialOpsService {
    @Inject
    SpecialOpsRepository repository;

    public CountDto countFullNameGreater(String fullName) {
        if (fullName == null) {
            throw new InvalidDataException("fullName обязательное поле");
        }
        return new CountDto(repository.countFullNameGreater(fullName));
    }

    public ItemsDto<OrganizationDto> findNameContains(String sub) {
        if (sub == null || sub.isEmpty()) {
            throw new InvalidDataException("подстрока обязательна");
        }
        return new ItemsDto<>(repository.findNameContains(sub).stream().map(OrganizationDto::from).toList());
    }

    public RatingsDto uniqueRatings() {
        return new RatingsDto(repository.uniqueRatings());
    }

    public ItemsDto<OrganizationDto> top5ByTurnover() {
        return new ItemsDto<>(repository.top5ByTurnover().stream().map(OrganizationDto::from).toList());
    }

    public AverageDto avgEmployeesTop10() {
        return new AverageDto(repository.avgEmployeesTop10());
    }
}
