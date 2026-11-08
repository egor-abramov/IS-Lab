package core.repository;

import core.model.Organization;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SpecialOpsRepository extends BaseRepository {
    private Long ownerId() {
        return currentUser.isAdmin() ? null : currentUser.id();
    }

    public long countFullNameGreater(String fullName) {
        return ((Number) em.createNativeQuery("select count_full_name_greater(?1, cast(?2 as bigint))")
                .setParameter(1, fullName).setParameter(2, ownerId()).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Organization> findNameContains(String sub) {
        return em.createNativeQuery("select * from find_name_contains(?1, cast(?2 as bigint))", Organization.class)
                .setParameter(1, sub).setParameter(2, ownerId()).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> uniqueRatings() {
        List<Number> ratings = em.createNativeQuery("select * from unique_ratings(cast(?1 as bigint))")
                .setParameter(1, ownerId()).getResultList();
        return ratings.stream().map(Number::longValue).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Organization> top5ByTurnover() {
        return em.createNativeQuery("select * from top5_by_turnover(cast(?1 as bigint))", Organization.class)
                .setParameter(1, ownerId()).getResultList();
    }

    public double avgEmployeesTop10() {
        return ((Number) em.createNativeQuery("select avg_employees_top10(cast(?1 as bigint))")
                .setParameter(1, ownerId()).getSingleResult()).doubleValue();
    }
}
