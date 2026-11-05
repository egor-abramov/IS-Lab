package core.service;

import core.dto.AddressDto;
import core.dto.AddressRequest;
import core.dto.ItemsDto;
import core.model.Address;
import core.model.Location;
import core.repository.AddressRepository;
import core.repository.LocationRepository;
import core.service.exception.EntityInUseException;
import core.service.exception.InvalidDataException;
import core.service.exception.NotFoundException;
import core.ws.UpdateNotifier;
import core.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Transactional
public class AddressService {
    @Inject
    AddressRepository repository;
    @Inject
    LocationRepository locationRepository;
    @Inject
    DataValidator validator;
    @Inject
    UpdateNotifier notifier;
    @Inject
    CurrentUser currentUser;

    public ItemsDto<AddressDto> list() {
        return new ItemsDto<>(repository.findAll().stream().map(AddressDto::from).toList());
    }

    public AddressDto create(AddressRequest r) {
        List<String> errors = new ArrayList<>();
        Address a = new Address();
        if (r != null && r.townId() != null) {
            Location town = locationRepository.findById(r.townId());
            if (town == null) {
                errors.add(NotFoundException.message("город", r.townId()));
            }
            currentUser.requireOwnReference(town);
            a.setTown(town);
        }
        validator.check(r, errors);
        a.setStreet(r.street());
        validator.check(a);
        Address saved = repository.save(a);
        notifier.broadcast("addresses");
        return AddressDto.from(saved);
    }

    public void delete(long id, Long replacementId) {
        if (repository.findById(id) == null) {
            throw new NotFoundException("адрес", id);
        }
        currentUser.requireWrite(repository.findById(id));
        long usages = repository.countUsages(id);
        if (usages > 0) {
            if (replacementId == null) {
                throw new EntityInUseException("адрес используется организациями (" + usages
                        + " шт.), выберите адрес для замены");
            }
            if (replacementId == id || repository.findById(replacementId) == null) {
                throw new InvalidDataException("некорректный адрес для замены");
            }
        }
        if (usages > 0) {
            currentUser.requireOwnReference(repository.findById(replacementId));
        }
        repository.deleteWithReplace(id, usages > 0 ? replacementId : null);
        notifier.broadcast("addresses");
        notifier.broadcast("organizations");
    }
}
