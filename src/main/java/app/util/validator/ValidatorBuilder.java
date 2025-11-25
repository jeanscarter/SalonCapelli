package app.util.validator;

import app.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Builder para crear validadores complejos
 * Patrón: Builder + Fluent Interface
 */
public class ValidatorBuilder<T> {
    
    private final String fieldName;
    private final List<Validator<T>> validators = new ArrayList<>();
    
    private ValidatorBuilder(String fieldName) {
        this.fieldName = fieldName;
    }
    
    /**
     * Crea un nuevo builder para un campo
     * 
     * @param fieldName Nombre del campo a validar
     * @param <T> Tipo del valor a validar
     * @return Builder configurado
     */
    public static <T> ValidatorBuilder<T> forField(String fieldName) {
        return new ValidatorBuilder<>(fieldName);
    }
    
    /**
     * Valida que el valor no sea null
     * 
     * @return Este builder para encadenamiento
     */
    public ValidatorBuilder<T> notNull() {
        validators.add(value -> {
            if (value == null) {
                return ValidationResult.invalid(fieldName, "Este campo es obligatorio");
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    /**
     * Valida que el valor no esté vacío
     * 
     * @return Este builder para encadenamiento
     */
    public ValidatorBuilder<T> notEmpty() {
        validators.add(value -> {
            if (value == null || value.toString().trim().isEmpty()) {
                return ValidationResult.invalid(fieldName, "Este campo no puede estar vacío");
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    /**
     * Valida que el valor coincida con una expresión regular
     * 
     * @param regex Expresión regular
     * @param errorMessage Mensaje de error personalizado
     * @return Este builder para encadenamiento
     */
    public ValidatorBuilder<T> matches(String regex, String errorMessage) {
        validators.add(value -> {
            if (value != null && !value.toString().matches(regex)) {
                return ValidationResult.invalid(fieldName, errorMessage);
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    /**
     * Valida longitud mínima
     * 
     * @param min Longitud mínima
     * @return Este builder para encadenamiento
     */
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
    
    /**
     * Valida longitud máxima
     * 
     * @param max Longitud máxima
     * @return Este builder para encadenamiento
     */
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
    
    /**
     * Valida con una condición personalizada
     * 
     * @param condition Condición a evaluar
     * @param errorMessage Mensaje de error si falla
     * @return Este builder para encadenamiento
     */
    public ValidatorBuilder<T> custom(Predicate<T> condition, String errorMessage) {
        validators.add(value -> {
            if (!condition.test(value)) {
                return ValidationResult.invalid(fieldName, errorMessage);
            }
            return ValidationResult.valid();
        });
        return this;
    }
    
    /**
     * Construye el validador final
     * 
     * @return Validador configurado
     */
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