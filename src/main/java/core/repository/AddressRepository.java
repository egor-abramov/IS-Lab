package core.repository;

import core.model.Address;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AddressRepository extends BaseRepository {

    public List<Address> findAll() {
        return read(em -> em.createQuery("select a from Address a order by a.id", Address.class)
                .getResultList());
    }

    public Address findById(long id) {
        return read(em -> em.find(Address.class, id));
    }

    public Address save(Address a) {
        return inTransaction(em -> em.merge(a));
    }

    public long countUsages(long id) {
        return read(em -> em.createQuery(
                        "select count(o) from Organization o where o.officialAddress.id = :id or o.postalAddress.id = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult());
    }

    public void deleteWithReplace(long id, Long replacementId) {
        inTransaction(em -> {
            if (replacementId != null) {
                Address replacement = em.find(Address.class, replacementId);
                em.createQuery("update Organization o set o.officialAddress = :r where o.officialAddress.id = :id")
                        .setParameter("r", replacement)
                        .setParameter("id", id)
                        .executeUpdate();
                em.createQuery("update Organization o set o.postalAddress = :r where o.postalAddress.id = :id")
                        .setParameter("r", replacement)
                        .setParameter("id", id)
                        .executeUpdate();
            }
            em.remove(em.find(Address.class, id));
            return null;
        });
    }
}
