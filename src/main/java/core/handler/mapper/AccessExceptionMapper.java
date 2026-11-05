package core.handler.mapper;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AccessExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
        Response response = ErrorResponses.of(e.getResponse().getStatusInfo(), e.getMessage());
        return e.getResponse().getStatus() == 401
                ? Response.fromResponse(response).header("WWW-Authenticate", "Bearer").build()
                : response;
    }
}
