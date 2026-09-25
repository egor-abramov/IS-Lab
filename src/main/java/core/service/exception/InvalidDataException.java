package core.service.exception;

import java.util.List;

public class InvalidDataException extends AppException {
    public InvalidDataException(List<String> errors) {
        super(errors);
    }

    public InvalidDataException(String error) {
        this(List.of(error));
    }
}
