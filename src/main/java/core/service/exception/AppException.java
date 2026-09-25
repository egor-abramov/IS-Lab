package core.service.exception;

import java.util.List;

public abstract class AppException extends RuntimeException {
    private final List<String> errors;

    protected AppException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() { return errors; }
}
