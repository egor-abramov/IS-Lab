package core.service;

import core.model.*;
import core.repository.OrganizationRepository;
import core.security.CurrentUser;
import core.service.exception.InvalidDataException;
import core.ws.UpdateNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationImportTest {
    OrganizationService service;

    @BeforeEach
    void setup() {
        service = new OrganizationService();
        service.repository = mock(OrganizationRepository.class);
        service.validator = mock(DataValidator.class);
        service.notifier = mock(UpdateNotifier.class);
        service.currentUser = new CurrentUser();
        AppUser user = new AppUser();
        user.setId(1L);
        service.currentUser.set(user);
    }

    @Test
    void importedGraphAlwaysBelongsToCurrentUser() {
        Organization org = new Organization();
        org.setOwnerId(99L);
        org.setCoordinates(new Coordinates());
        Address address = new Address();
        address.setOwnerId(99L);
        address.setTown(new Location());
        org.setPostalAddress(address);
        org.setOfficialAddress(address);
        service.load(List.of(org));
        assertEquals(1L, org.getOwnerId());
        assertEquals(1L, org.getCoordinates().getOwnerId());
        assertEquals(1L, address.getOwnerId());
        assertEquals(1L, address.getTown().getOwnerId());
        verify(service.repository).load(List.of(org));
    }

    @Test
    void existingNestedIdPreventsSavingTheEntireList() {
        Organization first = new Organization();
        Organization second = new Organization();
        Address address = new Address();
        address.setId(7L);
        second.setPostalAddress(address);
        assertThrows(InvalidDataException.class, () -> service.load(List.of(first, second)));
        verifyNoInteractions(service.repository, service.notifier);
    }
}
