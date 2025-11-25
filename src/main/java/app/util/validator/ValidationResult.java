package app.util.validator;

import app.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una validación
 * Patrón: Value Object
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<ValidationException.ValidationError> errors;
    
    private ValidationResult(boolean valid, List<ValidationException.ValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }
    
    /**
     * Crea un resultado válido
     * 
     * @return ValidationResult válido
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, new ArrayList<>());
    }
    
    /**
     * Crea un resultado inválido con un error
     * 
     * @param field Campo que falló la validación
     * @param message Mensaje de error
     * @return ValidationResult inválido
     */
    public static ValidationResult invalid(String field, String message) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationException.ValidationError(field, message));
        return new ValidationResult(false, errors);
    }
    
    /**
     * Crea un resultado inválido con múltiples errores
     * 
     * @param errors Lista de errores
     * @return ValidationResult inválido
     */
    public static ValidationResult invalid(List<ValidationException.ValidationError> errors) {
        return new ValidationResult(false, errors);
    }
    
    /**
     * Verifica si la validación fue exitosa
     * 
     * @return true si es válido, false en caso contrario
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Obtiene la lista de errores
     * 
     * @return Lista de errores de validación
     */
    public List<ValidationException.ValidationError> getErrors() {
        return errors;
    }
    
    /**
     * Lanza una excepción si la validación falló
     * 
     * @throws ValidationException si la validación es inválida
     */
    public void throwIfInvalid() throws ValidationException {
        if (!valid) {
            throw new ValidationException(errors);
        }
    }
}