package core.service;

import core.dto.OrganizationDto;
import core.dto.OrganizationPageDto;
import core.dto.OrganizationRequest;
import core.model.Address;
import core.model.Coordinates;
import core.model.Organization;
import core.repository.AddressRepository;
import core.repository.CoordinatesRepository;
import core.repository.OrganizationRepository;
import core.service.exception.InvalidDataException;
import core.service.exception.NotFoundException;
import core.ws.UpdateNotifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class OrganizationService {
    private static final Set<String> FILTER_FIELDS = Set.of("name", "fullName", "type");
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "creationDate", "annualTurnover",
            "employeesCount", "rating", "fullName", "type");

    @Inject
    OrganizationRepository repository;
    @Inject
    CoordinatesRepository coordinatesRepository;
    @Inject
    AddressRepository addressRepository;
    @Inject
    EntityValidator validator;
    @Inject
    UpdateNotifier notifier;

    public OrganizationPageDto list(int page, int size, String filterField, String filterValue,
                                    String sortField, String sortDir) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidDataException("page must be >= 0 and size must be between 1 and 100");
        }
        if (filterField != null && !FILTER_FIELDS.contains(filterField)) {
            throw new InvalidDataException("filtering is allowed only by " + FILTER_FIELDS);
        }
        if (sortField == null || sortField.isEmpty()) {
            sortField = "id";
        }
        if (!SORT_FIELDS.contains(sortField)) {
            throw new InvalidDataException("sorting is allowed only by " + SORT_FIELDS);
        }
        boolean asc = !"desc".equalsIgnoreCase(sortDir);
        List<OrganizationDto> items = repository.findPage(filterField, filterValue, sortField, asc, page, size)
                .stream().map(OrganizationDto::from).toList();
        long total = repository.count(filterField, filterValue);
        return new OrganizationPageDto(items, total, page, size);
    }

    public OrganizationDto get(long id) {
        return OrganizationDto.from(find(id));
    }

    public OrganizationDto create(OrganizationRequest request) {
        return save(new Organization(), request);
    }

    public OrganizationDto update(long id, OrganizationRequest request) {
        return save(find(id), request);
    }

    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException("Organization", id);
        }
        notifier.broadcast("organizations");
    }

    private OrganizationDto save(Organization o, OrganizationRequest request) {
        validator.check(request);
        fill(o, request);
        validator.check(o);
        Organization saved = repository.save(o);
        notifier.broadcast("organizations");
        return OrganizationDto.from(saved);
    }

    private Organization find(long id) {
        Organization o = repository.findById(id);
        if (o == null) {
            throw new NotFoundException("Organization", id);
        }
        return o;
    }

    private void fill(Organization o, OrganizationRequest r) {
        o.setName(r.name() == null ? null : r.name().trim());
        o.setCoordinates(r.coordinatesId() == null ? null : findCoordinates(r.coordinatesId()));
        o.setOfficialAddress(r.officialAddressId() == null ? null : findAddress(r.officialAddressId()));
        o.setPostalAddress(r.postalAddressId() == null ? null : findAddress(r.postalAddressId()));
        o.setAnnualTurnover(r.annualTurnover());
        o.setEmployeesCount(r.employeesCount());
        o.setRating(r.rating());
        o.setFullName(r.fullName() == null || r.fullName().isBlank() ? null : r.fullName().trim());
        o.setType(r.type());
    }

    private Coordinates findCoordinates(long id) {
        Coordinates c = coordinatesRepository.findById(id);
        if (c == null) {
            throw new InvalidDataException(NotFoundException.message("Coordinates", id));
        }
        return c;
    }

    private Address findAddress(long id) {
        Address a = addressRepository.findById(id);
        if (a == null) {
            throw new InvalidDataException(NotFoundException.message("Address", id));
        }
        return a;
    }
}
