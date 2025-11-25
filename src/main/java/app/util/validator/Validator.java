package app.util.validator;

import app.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interfaz para validadores
 * Patrón: Strategy
 */
@FunctionalInterface
public interface Validator<T> {
    ValidationResult validate(T value);
    
    /**
     * Combina este validador con otro (Chain of Responsibility)
     */
    default Validator<T> and(Validator<T> other) {
        return value -> {
            ValidationResult first = this.validate(value);
            if (!first.isValid()) {
                return first;
            }
            return other.validate(value);
        };
    }
}

/**
 * Resultado de una validación
 * Patrón: Value Object
 */
class ValidationResult {
    private final boolean valid;
    private final List<ValidationException.ValidationError> errors;
    
    private ValidationResult(boolean valid, List<ValidationException.ValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, new ArrayList<>());
    }
    
    public static ValidationResult invalid(String field, String message) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationException.ValidationError(field, message));
        return new ValidationResult(false, errors);
    }
    
    public static ValidationResult invalid(List<ValidationException.ValidationError> errors) {
        return new ValidationResult(false, errors);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationException.ValidationError> getErrors() {
        return errors;
    }
    
    public void throwIfInvalid() throws ValidationException {
        if (!valid) {
            throw new ValidationException(errors);
        }
    }
}

/**
 * Builder para crear validadores complejos
 * Patrón: Builder + Fluent Interface
 */
class ValidatorBuilder<T> {
    private final String fieldName;
    private final List<Validator<T>> validators = new ArrayList<>();
    
    private ValidatorBuilder(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public static <T> ValidatorBuilder<T> forField(String fieldName) {
        return new ValidatorBuilder<>(fieldName);
    }
    
    public ValidatorBuilder<T> notNull() {
        validators.add(value -> {
            if (value == null) {
                return ValidationResult.invalid(fieldName, "Este campo es obligatorio");
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public ValidatorBuilder<T> notEmpty() {
        validators.add(value -> {
            if (value == null || value.toString().trim().isEmpty()) {
                return ValidationResult.invalid(fieldName, "Este campo no puede estar vacío");
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public ValidatorBuilder<T> matches(String regex, String errorMessage) {
        validators.add(value -> {
            if (value != null && !value.toString().matches(regex)) {
                return ValidationResult.invalid(fieldName, errorMessage);
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public ValidatorBuilder<T> minLength(int min) {
        validators.add(value -> {
            if (value != null && value.toString().length() < min) {
                return ValidationResult.invalid(
                    fieldName, 
                    String.format("Debe tener al menos %d caracteres", min)
                );
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public ValidatorBuilder<T> maxLength(int max) {
        validators.add(value -> {
            if (value != null && value.toString().length() > max) {
                return ValidationResult.invalid(
                    fieldName, 
                    String.format("No puede exceder %d caracteres", max)
                );
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public ValidatorBuilder<T> custom(Predicate<T> condition, String errorMessage) {
        validators.add(value -> {
            if (!condition.test(value)) {
                return ValidationResult.invalid(fieldName, errorMessage);
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    public Validator<T> build() {
        return value -> {
            List<ValidationException.ValidationError> allErrors = new ArrayList<>();
            
            for (Validator<T> validator : validators) {
                ValidationResult result = validator.validate(value);
                if (!result.isValid()) {
                    allErrors.addAll(result.getErrors());
                }
            }
            
            return allErrors.isEmpty() ? 
                ValidationResult.valid() : 
                ValidationResult.invalid(allErrors);
        };
    }
}