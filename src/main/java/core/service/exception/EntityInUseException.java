package core.service.exception;

import java.util.List;

public class EntityInUseException extends AppException {
    public EntityInUseException(String error) {
        super(List.of(error));
    }
}
