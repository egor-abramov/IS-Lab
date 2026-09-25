package core.handler.mapper;

import core.service.exception.EntityInUseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EntityInUseExceptionMapper implements ExceptionMapper<EntityInUseException> {
    @Override
    public Response toResponse(EntityInUseException e) {
        return ErrorResponses.of(Response.Status.CONFLICT, e.getErrors());
    }
}
