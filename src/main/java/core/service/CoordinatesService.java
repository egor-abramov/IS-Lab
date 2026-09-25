package core.service;

import core.dto.CoordinatesDto;
import core.dto.CoordinatesRequest;
import core.dto.ItemsDto;
import core.model.Coordinates;
import core.repository.CoordinatesRepository;
import core.service.exception.EntityInUseException;
import core.service.exception.InvalidDataException;
import core.service.exception.NotFoundException;
import core.ws.UpdateNotifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CoordinatesService {
    @Inject
    CoordinatesRepository repository;
    @Inject
    EntityValidator validator;
    @Inject
    UpdateNotifier notifier;

    public ItemsDto<CoordinatesDto> list() {
        return new ItemsDto<>(repository.findAll().stream().map(CoordinatesDto::from).toList());
    }

    public CoordinatesDto create(CoordinatesRequest r) {
        validator.check(r);
        Coordinates c = new Coordinates();
        c.setX(r.x());
        c.setY(r.y());
        validator.check(c);
        Coordinates saved = repository.save(c);
        notifier.broadcast("coordinates");
        return CoordinatesDto.from(saved);
    }

    public void delete(long id, Long replacementId) {
        if (repository.findById(id) == null) {
            throw new NotFoundException("Coordinates", id);
        }
        long usages = repository.countUsages(id);
        if (usages > 0) {
            if (replacementId == null) {
                throw new EntityInUseException("coordinates are used by " + usages
                        + " organizations, choose replacement coordinates");
            }
            if (replacementId == id || repository.findById(replacementId) == null) {
                throw new InvalidDataException("invalid replacement coordinates");
            }
        }
        repository.deleteWithReplace(id, usages > 0 ? replacementId : null);
        notifier.broadcast("coordinates");
        notifier.broadcast("organizations");
    }
}
