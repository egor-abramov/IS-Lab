package core.repository;

import core.model.Coordinates;
import core.model.Organization;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CoordinatesRepository extends BaseRepository {

    public List<Coordinates> findAll() {
        return findAll(Coordinates.class);
    }

    public Coordinates findById(long id) {
        return em.find(Coordinates.class, id);
    }

    public long countUsages(long id) {
        return countReferences(Organization.class, id, "coordinates");
    }

    public void deleteWithReplace(long id, Long replacementId) {
        if (replacementId != null) {
            replaceReferences(Organization.class, "coordinates", id, findById(replacementId));
        }
        delete(Coordinates.class, id);
    }
}
