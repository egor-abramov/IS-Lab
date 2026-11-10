package core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PessimisticLockException;
import jakarta.transaction.RollbackException;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import static org.postgresql.util.PSQLState.SERIALIZATION_FAILURE;

@ApplicationScoped
public class SerializationRetry {
    private static final int MAX_ATTEMPTS = 3;

    public <T> T run(Callable<T> op) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return op.call();
            } catch (RollbackException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("транзакция не завершена", e);
                }
            } catch (PessimisticLockException e) {
                if (!isSerializationFailure(e)) {
                    throw e;
                }
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("транзакция не завершена", e);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException();
    }

    private boolean isSerializationFailure(Throwable error) {
        while (error != null) {
            if (error instanceof SQLException sql && SERIALIZATION_FAILURE.getState().equals(sql.getSQLState())) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }
}
