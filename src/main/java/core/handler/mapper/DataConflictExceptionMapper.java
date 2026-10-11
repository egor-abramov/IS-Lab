package core.handler.mapper;

import core.service.exception.DataConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DataConflictExceptionMapper implements ExceptionMapper<DataConflictException> {
    @Override
    public Response toResponse(DataConflictException e) {
        return ErrorResponses.of(Response.Status.CONFLICT, e.getErrors());
    }
}
