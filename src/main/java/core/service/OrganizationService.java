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
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@Transactional
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
    DataValidator validator;
    @Inject
    UpdateNotifier notifier;

    public OrganizationPageDto list(int page, int size, String filterField, String filterValue,
                                    String sortField, String sortDir) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidDataException("номер страницы должен быть >= 0, а размер от 1 до 100");
        }
        if (filterField != null && !FILTER_FIELDS.contains(filterField)) {
            throw new InvalidDataException("фильтрация возможна только по полям " + FILTER_FIELDS);
        }
        if (sortField == null || sortField.isEmpty()) {
            sortField = "id";
        }
        if (!SORT_FIELDS.contains(sortField)) {
            throw new InvalidDataException("сортировка возможна только по полям " + SORT_FIELDS);
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
            throw new NotFoundException("организация", id);
        }
        notifier.broadcast("organizations");
    }

    private OrganizationDto save(Organization o, OrganizationRequest r) {
        List<String> errors = new ArrayList<>();
        validator.check(r, errors);

        o.setCoordinates(findCoordinates(r.coordinatesId(), errors));
        o.setOfficialAddress(findAddress(r.officialAddressId(), errors));
        o.setPostalAddress(findAddress(r.postalAddressId(), errors));

        o.setName(r.name().trim());
        o.setAnnualTurnover(r.annualTurnover());
        o.setEmployeesCount(r.employeesCount());
        o.setRating(r.rating());
        o.setFullName(r.fullName() == null || r.fullName().isBlank() ? null : r.fullName().trim());
        o.setType(r.type());

        validator.check(o);
        Organization saved = repository.save(o);
        notifier.broadcast("organizations");
        return OrganizationDto.from(saved);
    }

    private Organization find(long id) {
        Organization o = repository.findById(id);
        if (o == null) {
            throw new NotFoundException("организация", id);
        }
        return o;
    }

    private Coordinates findCoordinates(Long id, List<String> errors) {
        if (id == null) {
            return null;
        }
        Coordinates c = coordinatesRepository.findById(id);
        if (c == null) {
            errors.add(NotFoundException.message("координаты", id));
        }
        return c;
    }

    private Address findAddress(Long id, List<String> errors) {
        if (id == null) {
            return null;
        }
        Address a = addressRepository.findById(id);
        if (a == null) {
            errors.add(NotFoundException.message("адрес", id));
        }
        return a;
    }
}
