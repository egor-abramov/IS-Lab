package core.repository;

import core.model.Address;
import core.model.Organization;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AddressRepository extends BaseRepository {

    public List<Address> findAll() {
        return findAll(Address.class);
    }

    public Address findById(long id) {
        return em.find(Address.class, id);
    }

    public long countUsages(long id) {
        return countReferences(Organization.class, id, "officialAddress", "postalAddress");
    }

    public void deleteWithReplace(long id, Long replacementId) {
        if (replacementId != null) {
            Address replacement = findById(replacementId);
            replaceReferences(Organization.class, "officialAddress", id, replacement);
            replaceReferences(Organization.class, "postalAddress", id, replacement);
        }
        delete(Address.class, id);
    }
}
