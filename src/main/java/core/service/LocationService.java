package core.service;

import core.dto.ItemsDto;
import core.dto.LocationDto;
import core.dto.LocationRequest;
import core.model.Location;
import core.repository.LocationRepository;
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
public class LocationService {
    @Inject
    LocationRepository repository;
    @Inject
    DataValidator validator;
    @Inject
    UpdateNotifier notifier;
    @Inject
    CurrentUser currentUser;

    public ItemsDto<LocationDto> list() {
        return new ItemsDto<>(repository.findAll().stream().map(LocationDto::from).toList());
    }

    public LocationDto create(LocationRequest r) {
        validator.check(r);
        Location l = new Location();
        l.setX(r.x());
        l.setY(r.y());
        l.setName(r.name());
        validator.check(l);
        Location saved = repository.save(l);
        notifier.broadcast("locations");
        return LocationDto.from(saved);
    }

    public void delete(long id, Long replacementId) {
        if (repository.findById(id) == null) {
            throw new NotFoundException("город", id);
        }
        currentUser.requireWrite(repository.findById(id));
        long usages = repository.countUsages(id);
        if (usages > 0) {
            if (replacementId == null) {
                throw new EntityInUseException("город используется адресами (" + usages
                        + " шт.), выберите город для замены");
            }
            if (replacementId == id || repository.findById(replacementId) == null) {
                throw new InvalidDataException("некорректный город для замены");
            }
        }
        if (usages > 0) {
            currentUser.requireOwnReference(repository.findById(replacementId));
        }
        repository.deleteWithReplace(id, usages > 0 ? replacementId : null);
        notifier.broadcast("locations");
        notifier.broadcast("addresses");
        notifier.broadcast("organizations");
    }
}
