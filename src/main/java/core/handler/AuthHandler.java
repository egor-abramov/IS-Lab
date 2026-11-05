package core.handler;

import core.dto.AuthDto;
import core.dto.CredentialsRequest;
import core.dto.UserDto;
import core.security.CurrentUser;
import core.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthHandler {
    @Inject
    AuthService service;
    @Inject
    CurrentUser currentUser;

    @POST
    @Path("/register")
    @PermitAll
    public Response register(CredentialsRequest request) {
        return Response.status(Response.Status.CREATED).entity(service.register(request)).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public AuthDto login(CredentialsRequest request) {
        return service.login(request);
    }

    @GET
    @Path("/me")
    public UserDto me() {
        return UserDto.from(currentUser.get());
    }
}
