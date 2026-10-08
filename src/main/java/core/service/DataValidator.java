package core.service;

import core.service.exception.InvalidDataException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class DataValidator {
    @Inject
    Validator validator;

    public void check(Object object) {
        if (object == null) {
            throw new InvalidDataException("request body is empty");
        }
        Set<ConstraintViolation<Object>> errs = validator.validate(object);
        if (!errs.isEmpty()) {
            List<String> errors = errs.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .toList();
            throw new InvalidDataException(errors);
        }
    }
}
