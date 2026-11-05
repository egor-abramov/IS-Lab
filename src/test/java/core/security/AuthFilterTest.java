package core.security;

import core.handler.AuthHandler;
import core.handler.OrganizationHandler;
import core.handler.UserHandler;
import core.dto.CredentialsRequest;
import core.model.AppUser;
import core.model.Role;
import core.service.AuthService;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthFilterTest {
    AuthFilter filter;
    ContainerRequestContext request;
    AppUser user;

    @BeforeEach
    void setup() throws Exception {
        filter = new AuthFilter();
        filter.auth = mock(AuthService.class);
        filter.currentUser = new CurrentUser();
        filter.resource = mock(ResourceInfo.class);
        request = mock(ContainerRequestContext.class);
        when(request.getSecurityContext()).thenReturn(mock(SecurityContext.class));
        when(filter.resource.getResourceClass()).thenReturn((Class) OrganizationHandler.class);
        when(filter.resource.getResourceMethod()).thenReturn(OrganizationHandler.class.getMethod("get", long.class));
        user = new AppUser();
        user.setId(1L);
        user.setLogin("test-user");
    }

    @Test
    void missingOrInvalidTokenIsRejected() {
        filter.filter(request);
        verify(request).abortWith(argThat(r -> r.getStatus() == 401));
        verifyNoInteractions(filter.auth);
        reset(request);
        when(request.getHeaderString("Authorization")).thenReturn("Bearer broken");
        when(filter.auth.authenticate("broken")).thenThrow(new NotAuthorizedException("Bearer"));
        filter.filter(request);
        verify(request).abortWith(argThat(r -> r.getStatus() == 401));
    }

    @Test
    void registrationIsPublic() throws Exception {
        when(filter.resource.getResourceMethod()).thenReturn(AuthHandler.class.getMethod("register", CredentialsRequest.class));
        filter.filter(request);
        verify(request, never()).abortWith(any(Response.class));
        verifyNoInteractions(filter.auth);
    }

    @Test
    void userListRequiresAdministrator() throws Exception {
        when(filter.resource.getResourceClass()).thenReturn((Class) UserHandler.class);
        when(filter.resource.getResourceMethod()).thenReturn(UserHandler.class.getMethod("list"));
        when(request.getHeaderString("Authorization")).thenReturn("Bearer valid");
        when(filter.auth.authenticate("valid")).thenReturn(user);
        filter.filter(request);
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        clearInvocations(request);
        user.setRole(Role.ADMIN);
        filter.filter(request);
        verify(request, never()).abortWith(any(Response.class));
        assertEquals(1L, filter.currentUser.id());
        verify(request).setSecurityContext(argThat(c -> c.isUserInRole("ADMIN") && !c.isUserInRole("USER")));
    }
}
