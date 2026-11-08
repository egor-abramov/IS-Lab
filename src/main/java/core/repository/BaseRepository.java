package core.repository;

import core.service.exception.DataConflictException;
import core.model.OwnedEntity;
import core.security.CurrentUser;
import jakarta.inject.Inject;
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
    @Inject
    protected CurrentUser currentUser;

    @PersistenceContext(unitName = "lab1")
    EntityManager em;

    protected <T extends OwnedEntity> T findById(Class<T> type, long id) {
        T entity = em.find(type, id);
        return entity != null && currentUser.canRead(entity) ? entity : null;
    }

    protected Predicate visible(CriteriaBuilder cb, Root<?> root) {
        return currentUser.isAdmin() ? cb.conjunction() : cb.equal(root.get("ownerId"), currentUser.id());
    }

    protected <T extends OwnedEntity> List<T> findAll(Class<T> type) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(type);
        Root<T> root = query.from(type);
        query.where(visible(cb, root)).orderBy(cb.asc(root.get("id")));
        return em.createQuery(query).getResultList();
    }

    public <T extends OwnedEntity> T save(T entity) {
        if (entity.getId() == null) {
            entity.setOwnerId(currentUser.id());
        } else {
            currentUser.requireWrite(entity);
        }
        T saved = em.merge(entity);
        flush();
        return saved;
    }

    public void useSerializableIsolation() {
        em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE")
                .executeUpdate();
    }

    protected <T extends OwnedEntity> boolean delete(Class<T> type, long id) {
        T entity = findById(type, id);
        if (entity == null) {
            return false;
        }
        currentUser.requireWrite(entity);
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
                .where(cb.and(cb.equal(root.get("ownerId"), currentUser.id()), cb.or(conditions)));
        return em.createQuery(query).getSingleResult();
    }

    protected <T> void replaceReferences(Class<T> type, String reference, long oldId, Object replacement) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<T> update = cb.createCriteriaUpdate(type);
        Root<T> root = update.from(type);
        update.set(root.get(reference), replacement)
                .where(cb.and(cb.equal(root.get("ownerId"), currentUser.id()),
                        cb.equal(root.get(reference).get("id"), oldId)));
        try {
            em.createQuery(update).executeUpdate();
        } catch (PersistenceException e) {
            throw new DataConflictException("данные нарушают ограничения базы данных", e);
        }
    }

    private void flush() {
        try {
            em.flush();
        } catch (PersistenceException e) {
            throw new DataConflictException("данные нарушают ограничения базы данных", e);
        }
    }
}
