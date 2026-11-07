package core.service;

import core.dto.CoordinatesDto;
import core.dto.CoordinatesRequest;
import core.dto.ItemsDto;
import core.model.Coordinates;
import core.repository.CoordinatesRepository;
import core.repository.OrganizationRepository;
import core.service.exception.DataConflictException;
import core.service.exception.EntityInUseException;
import core.service.exception.InvalidDataException;
import core.service.exception.NotFoundException;
import core.ws.UpdateNotifier;
import core.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class CoordinatesService {
    @Inject
    CoordinatesRepository repository;
    @Inject
    OrganizationRepository organizations;
    @Inject
    DataValidator validator;
    @Inject
    UpdateNotifier notifier;
    @Inject
    CurrentUser currentUser;

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
        organizations.lockUniquenessCheck();
        if (repository.findById(id) == null) {
            throw new NotFoundException("координаты", id);
        }
        currentUser.requireWrite(repository.findById(id));
        long usages = repository.countUsages(id);
        if (usages > 0) {
            if (replacementId == null) {
                throw new EntityInUseException("координаты используются организациями (" + usages
                        + " шт.), выберите координаты для замены");
            }
            if (replacementId == id || repository.findById(replacementId) == null) {
                throw new InvalidDataException("некорректные координаты для замены");
            }
        }
        if (usages > 0) {
            currentUser.requireOwnReference(repository.findById(replacementId));
            if (organizations.coordinatesReplacementConflicts(id, replacementId)) {
                throw new DataConflictException("замена координат нарушает уникальность организаций");
            }
        }
        repository.deleteWithReplace(id, usages > 0 ? replacementId : null);
        notifier.broadcast("coordinates");
        notifier.broadcast("organizations");
    }
}
