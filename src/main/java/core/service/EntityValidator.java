package core.service;

import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class EntityValidator {
    @Inject
    Validator validator;

    public void check(Object object) {
        if (object == null) {
            throw new InvalidDataException("request body is empty");
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .toList();
            throw new InvalidDataException(errors);
        }
    }
}
