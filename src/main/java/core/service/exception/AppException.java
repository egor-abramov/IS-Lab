package core.service.exception;

import lombok.Getter;

import java.util.List;

@Getter
public abstract class AppException extends RuntimeException {
    private final List<String> errors;

    protected AppException(List<String> errors, Throwable cause) {
        super(String.join("; ", errors), cause);
        this.errors = errors;
    }

    protected AppException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }
}
