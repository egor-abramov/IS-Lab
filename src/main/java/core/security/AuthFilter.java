package core.security;

import core.dto.ErrorDto;
import core.model.AppUser;
import core.service.AuthService;
import jakarta.annotation.Priority;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Provider
@RequestScoped
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Inject
    AuthService auth;
    @Inject
    CurrentUser currentUser;
    @Context
    ResourceInfo resource;

    @Override
    public void filter(ContainerRequestContext request) {
        if (resource.getResourceMethod().isAnnotationPresent(PermitAll.class)) {
            return;
        }
        String header = request.getHeaderString("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            reject(request, 401, "необходим вход в систему");
            return;
        }
        AppUser user;
        try {
            user = auth.authenticate(header.substring(7).trim());
        } catch (NotAuthorizedException e) {
            reject(request, 401, "недействительный или просроченный токен");
            return;
        }
        currentUser.set(user);
        boolean secure = request.getSecurityContext().isSecure();
        request.setSecurityContext(new SecurityContext() {
            public Principal getUserPrincipal() { return user::getLogin; }
            public boolean isUserInRole(String role) { return user.getRole().name().equals(role); }
            public boolean isSecure() { return secure; }
            public String getAuthenticationScheme() { return "Bearer"; }
        });
        RolesAllowed roles = resource.getResourceMethod().getAnnotation(RolesAllowed.class);
        if (roles == null) {
            roles = resource.getResourceClass().getAnnotation(RolesAllowed.class);
        }
        if (roles != null && Arrays.stream(roles.value()).noneMatch(user.getRole().name()::equals)) {
            reject(request, 403, "недостаточно прав");
        }
    }

    private void reject(ContainerRequestContext request, int status, String message) {
        Response.ResponseBuilder response = Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(List.of(message)));
        if (status == 401) {
            response.header("WWW-Authenticate", "Bearer");
        }
        request.abortWith(response.build());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Cache-Control", "no-store");
    }
}
