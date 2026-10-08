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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AddressService {
    @Inject
    AddressRepository repository;
    @Inject
    LocationRepository locationRepository;
    @Inject
    DataValidator validator;
    @Inject
    UpdateNotifier notifier;

    public ItemsDto<AddressDto> list() {
        return new ItemsDto<>(repository.findAll().stream().map(AddressDto::from).toList());
    }

    public AddressDto create(AddressRequest r) {
        validator.check(r);
        Address a = new Address();
        a.setStreet(r.street());
        if (r.townId() != null) {
            Location town = locationRepository.findById(r.townId());
            if (town == null) {
                throw new InvalidDataException(NotFoundException.message("Location", r.townId()));
            }
            a.setTown(town);
        }
        validator.check(a);
        Address saved = repository.save(a);
        notifier.broadcast("addresses");
        return AddressDto.from(saved);
    }

    public void delete(long id, Long replacementId) {
        if (repository.findById(id) == null) {
            throw new NotFoundException("Address", id);
        }
        long usages = repository.countUsages(id);
        if (usages > 0) {
            if (replacementId == null) {
                throw new EntityInUseException("address is used by " + usages
                        + " organizations, choose a replacement address");
            }
            if (replacementId == id || repository.findById(replacementId) == null) {
                throw new InvalidDataException("invalid replacement address");
            }
        }
        repository.deleteWithReplace(id, usages > 0 ? replacementId : null);
        notifier.broadcast("addresses");
        notifier.broadcast("organizations");
    }
}
