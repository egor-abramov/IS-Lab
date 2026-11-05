package core.repository;

import core.model.ImportOperation;
import core.model.ImportStatus;
import core.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

@ApplicationScoped
public class ImportOperationRepository {
    @PersistenceContext(unitName = "lab1")
    EntityManager em;
    @Inject
    CurrentUser currentUser;

    public List<ImportOperation> history() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ImportOperation> query = cb.createQuery(ImportOperation.class);
        Root<ImportOperation> operation = query.from(ImportOperation.class);
        operation.fetch("user");
        if (!currentUser.isAdmin()) {
            query.where(cb.equal(operation.get("user").get("id"), currentUser.id()));
        }
        query.orderBy(cb.desc(operation.get("id")));
        return em.createQuery(query).getResultList();
    }

    public ImportOperation find(long id) {
        return em.find(ImportOperation.class, id);
    }

    public void create(ImportOperation operation) {
        em.persist(operation);
        em.flush();
    }

    public void finish(long id, ImportStatus status, Integer count) {
        ImportOperation operation = find(id);
        operation.setStatus(status);
        operation.setAddedCount(count);
    }
}
