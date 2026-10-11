package core.repository;

import core.service.exception.DataConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;

public abstract class BaseRepository {
    @PersistenceContext(unitName = "lab1")
    EntityManager em;

    protected <T> List<T> findAll(Class<T> type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(type);
        Root<T> root = query.from(type);
        query.orderBy(cb.asc(root.get("id")));
        return em.createQuery(query).getResultList();
    }

    public <T> T save(T entity) {
        T saved = em.merge(entity);
        flush();
        return saved;
    }

    protected boolean delete(Class<?> type, long id) {
        Object entity = em.find(type, id);
        if (entity == null) {
            return false;
        }
        em.remove(entity);
        flush();
        return true;
    }

    protected long countReferences(Class<?> type, long id, String... references) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<?> root = query.from(type);
        Predicate[] conditions = Arrays.stream(references)
                .map(reference -> cb.equal(root.get(reference).get("id"), id))
                .toArray(Predicate[]::new);
        query.select(cb.count(root))
                .where(cb.or(conditions));
        return em.createQuery(query).getSingleResult();
    }

    protected <T> void replaceReferences(Class<T> type, String reference, long oldId, Object replacement) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(type);
        Root<T> root = update.from(type);
        update.set(root.get(reference), replacement)
                .where(cb.equal(root.get(reference).get("id"), oldId));
        try {
            em.createQuery(update).executeUpdate();
        } catch (PersistenceException e) {
            throw new DataConflictException("данные нарушают ограничения базы данных");
        }
    }

    private void flush() {
        try {
            em.flush();
        } catch (PersistenceException e) {
            throw new DataConflictException("данные нарушают ограничения базы данных");
        }
    }
}
