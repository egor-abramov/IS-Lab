package core.repository;

import core.model.Organization;
import core.model.OrganizationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class OrganizationRepository extends BaseRepository {

    public Organization findById(long id) {
        return read(em -> em.find(Organization.class, id));
    }

    public Organization save(Organization organization) {
        return inTransaction(em -> em.merge(organization));
    }

    public boolean delete(long id) {
        return inTransaction(em -> {
            Organization o = em.find(Organization.class, id);
            if (o == null) {
                return false;
            }
            em.remove(o);
            return true;
        });
    }

    public List<Organization> findPage(String filterField, String filterValue,
                                       String sortField, boolean asc, int page, int size) {
        return read(em -> {
            String jpql = "select o from Organization o" + where(filterField, filterValue)
                    + " order by o." + sortField + (asc ? " asc" : " desc") + ", o.id";
            TypedQuery<Organization> q = em.createQuery(jpql, Organization.class);
            setFilter(q, filterField, filterValue);
            q.setFirstResult(page * size);
            q.setMaxResults(size);
            return q.getResultList();
        });
    }

    public long count(String filterField, String filterValue) {
        return read(em -> {
            TypedQuery<Long> q = em.createQuery(
                    "select count(o) from Organization o" + where(filterField, filterValue), Long.class);
            setFilter(q, filterField, filterValue);
            return q.getSingleResult();
        });
    }

    private String containsPattern(String value) {
        String escaped = value.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private String where(String field, String value) {
        if (field == null || value == null || value.isEmpty()) {
            return "";
        }
        if (field.equals("type")) {
            return matchingTypes(value).isEmpty() ? " where 1 = 0" : " where o.type in :types";
        }
        return " where lower(o." + field + ") like :value escape '!'";
    }

    private void setFilter(TypedQuery<?> q, String field, String value) {
        if (field == null || value == null || value.isEmpty()) {
            return;
        }
        if (field.equals("type")) {
            List<OrganizationType> types = matchingTypes(value);
            if (!types.isEmpty()) {
                q.setParameter("types", types);
            }
        } else {
            q.setParameter("value", containsPattern(value));
        }
    }

    private List<OrganizationType> matchingTypes(String value) {
        return Arrays.stream(OrganizationType.values())
                .filter(t -> t.name().toLowerCase().contains(value.toLowerCase()))
                .toList();
    }
}
