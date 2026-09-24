package core.repository;

import core.model.Location;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class LocationRepository extends BaseRepository {

    public List<Location> findAll() {
        return read(em -> em.createQuery("select l from Location l order by l.id", Location.class)
                .getResultList());
    }

    public Location findById(long id) {
        return read(em -> em.find(Location.class, id));
    }

    public Location save(Location l) {
        return inTransaction(em -> em.merge(l));
    }

    public long countUsages(long id) {
        return read(em -> em.createQuery("select count(a) from Address a where a.town.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult());
    }

    public void deleteWithReplace(long id, Long replacementId) {
        inTransaction(em -> {
            if (replacementId != null) {
                em.createQuery("update Address a set a.town = :r where a.town.id = :id")
                        .setParameter("r", em.find(Location.class, replacementId))
                        .setParameter("id", id)
                        .executeUpdate();
            }
            em.remove(em.find(Location.class, id));
            return null;
        });
    }
}
