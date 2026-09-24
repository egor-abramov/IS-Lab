package core.repository;

import core.model.Coordinates;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CoordinatesRepository extends BaseRepository {

    public List<Coordinates> findAll() {
        return read(em -> em.createQuery("select c from Coordinates c order by c.id", Coordinates.class)
                .getResultList());
    }

    public Coordinates findById(long id) {
        return read(em -> em.find(Coordinates.class, id));
    }

    public Coordinates save(Coordinates c) {
        return inTransaction(em -> em.merge(c));
    }

    public long countUsages(long id) {
        return read(em -> em.createQuery(
                        "select count(o) from Organization o where o.coordinates.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult());
    }

    public void deleteWithReplace(long id, Long replacementId) {
        inTransaction(em -> {
            if (replacementId != null) {
                em.createQuery("update Organization o set o.coordinates = :r where o.coordinates.id = :id")
                        .setParameter("r", em.find(Coordinates.class, replacementId))
                        .setParameter("id", id)
                        .executeUpdate();
            }
            em.remove(em.find(Coordinates.class, id));
            return null;
        });
    }
}
