package app.util.validator;

import app.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validador específico para datos venezolanos
 * Patrón: Singleton + Strategy
 */
public class ValidadorVenezolano {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidadorVenezolano.class);
    private static ValidadorVenezolano instance;
    
    // Patrones de validación
    private static final Pattern CEDULA_PATTERN = 
        Pattern.compile("^[VEve]-?\\d{7,8}$");
    
    private static final Pattern TELEFONO_PATTERN = 
        Pattern.compile("^0(412|414|424|416|426|212|243|244|245|246|247|248|249|251|252|253|254|255|256|257|258|259|261|262|263|264|265|266|267|268|269|271|272|273|274|275|276|277|278|279|281|282|283|284|285|286|287|288|289|291|292|293|294|295)[-\\s]?\\d{7}$");
    
    private static final Pattern NOMBRE_PATTERN = 
        Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$");
    
    private ValidadorVenezolano() {
        logger.debug("Inicializando ValidadorVenezolano");
    }
    
    public static ValidadorVenezolano getInstance() {
        if (instance == null) {
            synchronized (ValidadorVenezolano.class) {
                if (instance == null) {
                    instance = new ValidadorVenezolano();
                }
            }
        }
        return instance;
    }
    
    /**
     * Validador de cédula venezolana
     */
    public Validator<Object> cedulaValidator() {
        return ValidatorBuilder.forField("Cédula")
            .notEmpty()
            .matches(CEDULA_PATTERN.pattern(), 
                "Formato inválido. Use: V-12345678 o E-12345678")
            .build();
    }
    
    /**
     * Validador de teléfono venezolano
     */
    public Validator<String> telefonoValidator() {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                return ValidationResult.valid(); // Opcional
            }
            
            if (!TELEFONO_PATTERN.matcher(value).matches()) {
                return ValidationResult.invalid(
                    "Teléfono",
                    "Formato inválido. Use: 0412-1234567 o 0212-1234567"
                );
            }
            
            return ValidationResult.valid();
        };
    }
    
    /**
     * Validador de nombre
     */
    public Validator<Object> nombreValidator() {
        return ValidatorBuilder.forField("Nombre")
            .notEmpty()
            .minLength(3)
            .maxLength(100)
            .matches(NOMBRE_PATTERN.pattern(), 
                "Solo puede contener letras y espacios")
            .build();
    }
    
    /**
     * Normaliza una cédula (quita espacios, convierte a mayúsculas, agrega guion)
     */
    public String normalizarCedula(String cedula) {
        if (cedula == null) return null;
        
        String normalized = cedula.trim().toUpperCase().replace(" ", "");
        
        // Si no tiene guion, agregarlo
        if (!normalized.contains("-")) {
            if (normalized.length() > 1) {
                normalized = normalized.charAt(0) + "-" + normalized.substring(1);
            }
        }
        
        logger.debug("Cédula normalizada: {} -> {}", cedula, normalized);
        return normalized;
    }
    
    /**
     * Normaliza un teléfono (elimina espacios extras, formato consistente)
     */
    public String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) return null;
        
        String normalized = telefono.trim().replaceAll("\\s+", "");
        
        // Agregar guion si no lo tiene
        if (!normalized.contains("-") && normalized.length() == 11) {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(4);
        }
        
        logger.debug("Teléfono normalizado: {} -> {}", telefono, normalized);
        return normalized;
    }
    
    /**
     * Valida y normaliza una cédula
     */
    public String validarYNormalizarCedula(String cedula) throws ValidationException {
        logger.debug("Validando cédula: {}", cedula);
        
        String normalized = normalizarCedula(cedula);
        ValidationResult result = cedulaValidator().validate(normalized);
        result.throwIfInvalid();
        
        logger.info("Cédula válida: {}", normalized);
        return normalized;
    }
    
    /**
     * Valida y normaliza un teléfono
     */
    public String validarYNormalizarTelefono(String telefono) throws ValidationException {
        if (telefono == null || telefono.trim().isEmpty()) {
            return null; // Opcional
        }
        
        logger.debug("Validando teléfono: {}", telefono);
        
        String normalized = normalizarTelefono(telefono);
        ValidationResult result = telefonoValidator().validate(normalized);
        result.throwIfInvalid();
        
        logger.info("Teléfono válido: {}", normalized);
        return normalized;
    }
    
    /**
     * Valida un nombre
     */
    public void validarNombre(String nombre) throws ValidationException {
        logger.debug("Validando nombre: {}", nombre);
        
        ValidationResult result = nombreValidator().validate(nombre);
        result.throwIfInvalid();
        
        logger.info("Nombre válido: {}", nombre);
    }
}