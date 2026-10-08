package core.repository;

import core.model.Organization;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.ParameterMode;

import java.util.List;

@ApplicationScoped
public class SpecialOpsRepository extends BaseRepository {

    public long countFullNameGreater(String fullName) {
        return firstColumn(em.createStoredProcedureQuery("count_full_name_greater")
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                .setParameter(1, fullName)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Organization> findNameContains(String sub) {
        return em.createStoredProcedureQuery("find_name_contains", Organization.class)
                .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                .setParameter(1, sub)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> uniqueRatings() {
        return ((List<Object>) em.createStoredProcedureQuery("unique_ratings")
                .getResultList())
                .stream().map(row -> firstColumn(row).longValue()).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Organization> top5ByTurnover() {
        return em.createStoredProcedureQuery("top5_by_turnover", Organization.class)
                .getResultList();
    }

    public double avgEmployeesTop10() {
        return firstColumn(em.createStoredProcedureQuery("avg_employees_top10")
                .getSingleResult()).doubleValue();
    }

    private Number firstColumn(Object row) {
        return (Number) ((Object[]) row)[0];
    }
}
