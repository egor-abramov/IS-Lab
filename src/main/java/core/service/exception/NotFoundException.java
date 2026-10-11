package core.service.exception;

import java.util.List;

public class NotFoundException extends AppException {
    public NotFoundException(String what, long id) {
        super(List.of(message(what, id)));
    }

    public static String message(String what, long id) {
        return "не найдено: " + what + " с id " + id;
    }
}
