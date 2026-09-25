package core.repository;

import core.model.Organization;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SpecialOpsRepository extends BaseRepository {

    public long countFullNameGreater(String fullName) {
        return read(em -> ((Number) em.createNativeQuery("select count_full_name_greater(?1)")
                .setParameter(1, fullName)
                .getSingleResult()).longValue());
    }

    @SuppressWarnings("unchecked")
    public List<Organization> findNameContains(String sub) {
        return read(em -> em.createNativeQuery("select * from find_name_contains(?1)", Organization.class)
                .setParameter(1, sub)
                .getResultList());
    }

    @SuppressWarnings("unchecked")
    public List<Long> uniqueRatings() {
        return read(em -> ((List<Number>) em.createNativeQuery("select * from unique_ratings()")
                .getResultList())
                .stream().map(Number::longValue).toList());
    }

    @SuppressWarnings("unchecked")
    public List<Organization> top5ByTurnover() {
        return read(em -> em.createNativeQuery("select * from top5_by_turnover()", Organization.class)
                .getResultList());
    }

    public double avgEmployeesTop10() {
        return read(em -> ((Number) em.createNativeQuery("select avg_employees_top10()")
                .getSingleResult()).doubleValue());
    }
}
