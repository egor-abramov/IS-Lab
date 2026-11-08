package core.service.exception;

import java.util.List;

public class DataConflictException extends AppException {
    public DataConflictException(String error) {
        super(List.of(error));
    }

    public DataConflictException(String error, Throwable cause) {
        super(List.of(error), cause);
    }
}
