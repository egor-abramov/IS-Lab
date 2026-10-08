package core.repository;

import core.model.Address;
import core.model.Location;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class LocationRepository extends BaseRepository {

    public List<Location> findAll() {
        return findAll(Location.class);
    }

    public Location findById(long id) {
        return em.find(Location.class, id);
    }

    public long countUsages(long id) {
        return countReferences(Address.class, id, "town");
    }

    public void deleteWithReplace(long id, Long replacementId) {
        if (replacementId != null) {
            replaceReferences(Address.class, "town", id, findById(replacementId));
        }
        delete(Location.class, id);
    }
}
