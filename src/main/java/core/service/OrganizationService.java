package core.service;

import core.dto.OrganizationDto;
import core.dto.OrganizationPageDto;
import core.dto.OrganizationRequest;
import core.model.Address;
import core.model.Coordinates;
import core.model.Organization;
import core.model.OrganizationSource;
import core.repository.AddressRepository;
import core.repository.CoordinatesRepository;
import core.repository.OrganizationRepository;
import core.service.exception.InvalidDataException;
import core.service.exception.DataConflictException;
import core.service.exception.NotFoundException;
import core.ws.UpdateNotifier;
import core.security.CurrentUser;
import core.model.OwnedEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
@Transactional
public class OrganizationService {
    private static final Set<String> FILTER_FIELDS = Set.of("name", "fullName", "type", "source");
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
    @Inject
    CurrentUser currentUser;

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
        repository.lockUniquenessCheck();
        return save(findForUpdate(id), request);
    }

    public void delete(long id) {
        findForUpdate(id);
        if (!repository.delete(id)) {
            throw new NotFoundException("организация", id);
        }
        notifier.broadcast("organizations");
    }

    public void load(List<Organization> organizations) {
        for (Organization o: organizations) {
            validator.check(o);
            o.setSource(OrganizationSource.FILE);
        }
        for (Organization o : organizations) {
            prepareImported(o);
            prepareImported(o.getCoordinates());
            prepareAddress(o.getOfficialAddress());
            prepareAddress(o.getPostalAddress());
        }
        repository.lockUniquenessCheck();
        checkUniqueness(organizations);
        repository.load(organizations);
        notifier.broadcast("organizations");
    }

    private void prepareImported(OwnedEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() != null) {
            throw new InvalidDataException("id генерируется сервером и не должен передаваться при импорте");
        }
        entity.setOwnerId(currentUser.id());
    }

    private void prepareAddress(Address address) {
        if (address != null) {
            prepareImported(address);
            prepareImported(address.getTown());
        }
    }

    private OrganizationDto save(Organization o, OrganizationRequest r) {
        if (r == null) {
            throw new InvalidDataException("пустое тело запроса");
        }

        if (o.getId() != null) {
            currentUser.requireWrite(o);
        }
        repository.lockUniquenessCheck();
        List<String> errors = new ArrayList<>();

        o.setCoordinates(findCoordinates(r.coordinatesId(), errors));
        o.setOfficialAddress(findAddress(r.officialAddressId(), errors));
        o.setPostalAddress(findAddress(r.postalAddressId(), errors));
        validator.check(r, errors);
        currentUser.requireOwnReference(o.getCoordinates());
        currentUser.requireOwnReference(o.getOfficialAddress());
        currentUser.requireOwnReference(o.getPostalAddress());

        o.setName(r.name().trim());
        o.setAnnualTurnover(r.annualTurnover());
        o.setEmployeesCount(r.employeesCount());
        o.setRating(r.rating());
        o.setFullName(r.fullName() == null || r.fullName().isBlank() ? null : r.fullName().trim());
        o.setType(r.type());
        o.setSource(OrganizationSource.USER);

        validator.check(o);
        checkUniqueness(List.of(o));
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

    private Organization findForUpdate(long id) {
        Organization organization = repository.findByIdForUpdate(id);
        if (organization == null) {
            throw new NotFoundException("организация", id);
        }
        return organization;
    }

    private void checkUniqueness(List<Organization> candidates) {
        List<Organization> checked = new ArrayList<>(repository.findAllForUniqueness());
        for (Organization candidate : candidates) {
            for (Organization existing : checked) {
                if (candidate == existing || candidate.getId() != null && candidate.getId().equals(existing.getId())) {
                    continue;
                }
                if (normalized(candidate.getName()).equals(normalized(existing.getName()))) {
                    throw new DataConflictException("организация с таким name уже существует");
                }
                if (candidate.getFullName() != null && !candidate.getFullName().isBlank()
                        && existing.getFullName() != null && !existing.getFullName().isBlank()
                        && normalized(candidate.getFullName()).equals(normalized(existing.getFullName()))) {
                    throw new DataConflictException("организация с таким fullName уже существует");
                }
                if (Float.compare(candidate.getCoordinates().getX(), existing.getCoordinates().getX()) == 0
                        && candidate.getCoordinates().getY() == existing.getCoordinates().getY()) {
                    throw new DataConflictException("организация с такими coordinates уже существует");
                }
            }
            checked.add(candidate);
        }
    }

    private String normalized(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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
