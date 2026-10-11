package core.handler.mapper;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class.getName());

    @Override
    public Response toResponse(PersistenceException e) {
        LOG.log(Level.WARNING, "database error", e);
        return ErrorResponses.of(Response.Status.INTERNAL_SERVER_ERROR, "внутренняя ошибка сервера");
    }
}
