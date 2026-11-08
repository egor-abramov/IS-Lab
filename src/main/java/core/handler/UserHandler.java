package core.handler;

import core.dto.ItemsDto;
import core.dto.UserDto;
import core.repository.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class UserHandler {
    @Inject
    UserRepository repository;

    @GET
    public ItemsDto<UserDto> list() {
        return new ItemsDto<>(repository.findAll().stream().map(UserDto::from).toList());
    }
}
