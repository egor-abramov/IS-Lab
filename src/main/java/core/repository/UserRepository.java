package core.repository;

import core.model.AppUser;
import core.service.exception.DataConflictException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import java.util.List;

@ApplicationScoped
public class UserRepository {
    @PersistenceContext(unitName = "lab1")
    EntityManager em;

    public AppUser findByLogin(String login) {
        return em.createQuery("select u from AppUser u where u.login = :login", AppUser.class)
                .setParameter("login", login).getResultStream().findFirst().orElse(null);
    }

    public AppUser findById(long id) {
        return em.find(AppUser.class, id);
    }

    public List<AppUser> findAll() {
        return em.createQuery("select u from AppUser u order by u.id", AppUser.class).getResultList();
    }

    public void create(AppUser user) {
        try {
            em.persist(user);
            em.flush();
        } catch (PersistenceException e) {
            throw new DataConflictException("не удалось зарегистрировать пользователя: логин уже занят или данные недоступны");
        }
    }
}
