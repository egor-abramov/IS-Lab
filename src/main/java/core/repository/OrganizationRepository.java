package core.repository;

import core.model.Organization;
import core.model.OrganizationSource;
import core.model.OrganizationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class OrganizationRepository extends BaseRepository {

    public Organization findById(long id) {
        return findById(Organization.class, id);
    }

    public Organization findByIdForUpdate(long id) {
        Organization organization = em.find(Organization.class, id, LockModeType.PESSIMISTIC_WRITE);
        return organization != null && currentUser.canRead(organization) ? organization : null;
    }

    public List<Organization> findAllForUniqueness() {
        return em.createQuery("select o from Organization o", Organization.class).getResultList();
    }

    public boolean coordinatesReplacementConflicts(long oldId, long replacementId) {
        Long oldCount = em.createQuery("select count(o) from Organization o where o.coordinates.id = :id", Long.class)
                .setParameter("id", oldId).getSingleResult();
        Long newCount = em.createQuery("select count(o) from Organization o where o.coordinates.id = :id", Long.class)
                .setParameter("id", replacementId).getSingleResult();
        return oldCount > 0 && newCount > 0;
    }

    public boolean delete(long id) {
        return delete(Organization.class, id);
    }

    public List<Organization> findPage(String filterField, String filterValue,
                                       String sortField, boolean asc, int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Organization> query = cb.createQuery(Organization.class);
        Root<Organization> o = query.from(Organization.class);

        Order order = asc ? cb.asc(o.get(sortField)) : cb.desc(o.get(sortField));
        query.where(cb.and(visible(cb, o), filter(cb, o, filterField, filterValue)))
                .orderBy(order, cb.asc(o.get("id")));

        return em.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long count(String filterField, String filterValue) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Organization> o = query.from(Organization.class);

        query.select(cb.count(o))
                .where(cb.and(visible(cb, o), filter(cb, o, filterField, filterValue)));

        return em.createQuery(query).getSingleResult();
    }

    public void load(List<Organization> organizations) {
        for (Organization o : organizations) {
            em.persist(o);
        }
    }

    public List<Organization> loaded() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Organization> query = cb.createQuery(Organization.class);
        Root<Organization> o = query.from(Organization.class);

        query.where(cb.and(visible(cb, o), cb.equal(o.get("source"), OrganizationSource.FILE)))
                .orderBy(cb.asc(o.get("id")));
        return em.createQuery(query).getResultList();
    }

    private Predicate filter(CriteriaBuilder cb, Root<Organization> o, String field, String value) {
        if (field == null || value == null || value.isEmpty()) {
            return cb.conjunction();
        }
        String substring = value.toLowerCase();

        if (field.equals("type")) {
            List<OrganizationType> types = Arrays.stream(OrganizationType.values())
                    .filter(t -> t.name().toLowerCase().contains(substring))
                    .toList();
            return types.isEmpty() ? cb.disjunction() : o.get("type").in(types);
        }

        if (field.equals("source")) {
            List<OrganizationSource> sources = Arrays.stream(OrganizationSource.values())
                    .filter(s -> s.name().toLowerCase().contains(substring))
                    .toList();
            return sources.isEmpty() ? cb.disjunction() : o.get("source").in(sources);
        }

        return cb.greaterThan(cb.locate(cb.lower(o.get(field)), substring), 0);
    }
}
