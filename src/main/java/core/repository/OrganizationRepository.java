package core.repository;

import core.model.Organization;
import core.model.OrganizationType;
import jakarta.enterprise.context.ApplicationScoped;
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
        return em.find(Organization.class, id);
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
        query.where(filter(cb, o, filterField, filterValue))
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
                .where(filter(cb, o, filterField, filterValue));

        return em.createQuery(query).getSingleResult();
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

        return cb.greaterThan(cb.locate(cb.lower(o.<String>get(field)), substring), 0);
    }
}
