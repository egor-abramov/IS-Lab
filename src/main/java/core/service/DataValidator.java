package core.service;

import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DataValidator {
    @Inject
    Validator validator;

    public void check(Object object) {
        check(object, new ArrayList<>());
    }

    public void check(Object object, List<String> errors) {
        if (object == null) {
            throw new InvalidDataException("пустое тело запроса");
        }
        validator.validate(object).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .forEach(errors::add);
        if (!errors.isEmpty()) {
            throw new InvalidDataException(errors);
        }
    }
}
