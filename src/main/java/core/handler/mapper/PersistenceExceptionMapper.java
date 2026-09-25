package core.handler.mapper;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class.getName());

    @Override
    public Response toResponse(PersistenceException e) {
        LOG.log(Level.WARNING, "database error", e);
        if (isConstraintViolation(e)) {
            return ErrorResponses.of(Response.Status.BAD_REQUEST,
                    "data violates database constraints (it may have just been changed by another user)");
        }
        return ErrorResponses.of(Response.Status.INTERNAL_SERVER_ERROR, "database error");
    }

    private static boolean isConstraintViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState().startsWith("23");
            }
        }
        return false;
    }
}
