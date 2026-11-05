package core.security;

import core.model.AppUser;
import core.model.OwnedEntity;
import core.model.Role;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

import java.util.Objects;

@RequestScoped
public class CurrentUser {
    private AppUser user;

    public void set(AppUser user) {
        this.user = user;
    }

    public AppUser get() {
        if (user == null) {
            throw new NotAuthorizedException("Bearer");
        }
        return user;
    }

    public Long id() {
        return get().getId();
    }

    public boolean isAdmin() {
        return get().getRole() == Role.ADMIN;
    }

    public boolean canRead(OwnedEntity entity) {
        return isAdmin() || Objects.equals(id(), entity.getOwnerId());
    }

    public void requireWrite(OwnedEntity entity) {
        if (!Objects.equals(id(), entity.getOwnerId())) {
            throw new ForbiddenException("изменять можно только собственные данные");
        }
    }

    public void requireOwnReference(OwnedEntity entity) {
        if (entity != null) {
            requireWrite(entity);
        }
    }
}
