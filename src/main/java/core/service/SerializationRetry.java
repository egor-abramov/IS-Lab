package core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.RollbackException;

import java.util.concurrent.Callable;

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
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException();
    }
}
