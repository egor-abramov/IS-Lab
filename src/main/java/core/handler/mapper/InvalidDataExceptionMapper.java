package core.handler.mapper;

import core.service.exception.InvalidDataException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidDataExceptionMapper implements ExceptionMapper<InvalidDataException> {
    @Override
    public Response toResponse(InvalidDataException e) {
        return ErrorResponses.of(Response.Status.BAD_REQUEST, e.getErrors());
    }
}
