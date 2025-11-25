package app.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excepción para errores de validación
 * Patrón: Composite (puede contener múltiples errores)
 */
public class ValidationException extends BaseException {
    
    private static final String DEFAULT_CODE = "VALIDATION_ERROR";
    private final List<ValidationError> errors;
    
    public ValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
        this.errors.add(new ValidationError("general", message));
    }
    
    public ValidationException(String field, String message) {
        super(String.format("Campo '%s': %s", field, message));
        this.errors = new ArrayList<>();
        this.errors.add(new ValidationError(field, message));
    }
    
    public ValidationException(List<ValidationError> errors) {
        super(buildMessage(errors));
        this.errors = new ArrayList<>(errors);
    }
    
    @Override
    protected String getDefaultErrorCode() {
        return DEFAULT_CODE;
    }
    
    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN; // Las validaciones son warnings, no errors
    }
    
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    private static String buildMessage(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Error de validación";
        }
        if (errors.size() == 1) {
            ValidationError error = errors.get(0);
            return String.format("Campo '%s': %s", error.getField(), error.getMessage());
        }
        StringBuilder sb = new StringBuilder("Errores de validación:\n");
        for (ValidationError error : errors) {
            sb.append(String.format("  - %s: %s\n", error.getField(), error.getMessage()));
        }
        return sb.toString();
    }
    
    /**
     * Clase interna para representar un error de validación individual
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s", field, message);
        }
    }
    
    // Códigos de error específicos
    public static final String REQUIRED_FIELD = "VAL_REQUIRED_001";
    public static final String INVALID_FORMAT = "VAL_FORMAT_002";
    public static final String OUT_OF_RANGE = "VAL_RANGE_003";
    public static final String INVALID_LENGTH = "VAL_LENGTH_004";
    
    /**
     * Factory Methods para errores comunes
     */
    public static ValidationException requiredField(String field) {
        return new ValidationException(field, "Este campo es obligatorio");
    }
    
    public static ValidationException invalidFormat(String field, String expectedFormat) {
        return new ValidationException(
            field,
            String.format("Formato inválido. Se espera: %s", expectedFormat)
        );
    }
    
    public static ValidationException invalidLength(String field, int min, int max) {
        return new ValidationException(
            field,
            String.format("Debe tener entre %d y %d caracteres", min, max)
        );
    }
}