package app.exception; // Trigger rebuild

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase base para todas las excepciones del sistema
 * Patrón: Template Method
 * 
 * Proporciona logging automático y estructura común.
 *
 * CORRECCIÓN #10: logException() movido fuera de los constructores de BaseException.
 * Las subclases deben llamar logCreation() explícitamente al final de su propio
 * constructor, después de inicializar sus campos, para que el log incluya todos
 * los datos relevantes (ej: cedula en ClienteDuplicadoException).
 */
public abstract class BaseException extends Exception {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseException.class);
    private final String errorCode;
    private final transient Object[] params;
    
    /**
     * Constructor con mensaje simple
     */
    protected BaseException(String message) {
        super(message);
        this.errorCode = getDefaultErrorCode();
        this.params = new Object[0];
        logCreation(); // Safe: no subclass fields to wait for in this constructor chain
    }
    
    /**
     * Constructor con mensaje y causa
     */
    protected BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = getDefaultErrorCode();
        this.params = new Object[0];
        logCreation(); // Safe: no subclass fields to wait for in this constructor chain
    }
    
    /**
     * Constructor completo con código de error y parámetros.
     * NOTA: NO llama logCreation() — las subclases deben llamarlo
     * explícitamente después de inicializar sus campos propios.
     */
    protected BaseException(String errorCode, String message, Object... params) {
        super(String.format(message, params));
        this.errorCode = errorCode;
        this.params = params;
        // CORRECCIÓN #10: NO llamar logCreation() aquí — la subclase lo hará
    }
    
    /**
     * Constructor completo con causa.
     * NOTA: NO llama logCreation() — las subclases deben llamarlo
     * explícitamente después de inicializar sus campos propios.
     */
    protected BaseException(String errorCode, String message, Throwable cause, Object... params) {
        super(String.format(message, params), cause);
        this.errorCode = errorCode;
        this.params = params;
        // CORRECCIÓN #10: NO llamar logCreation() aquí — la subclase lo hará
    }
    
    /**
     * Template Method - Las subclases deben implementar
     */
    protected abstract String getDefaultErrorCode();
    
    /**
     * Template Method - Nivel de logging (por defecto ERROR)
     */
    protected LogLevel getLogLevel() {
        return LogLevel.ERROR;
    }
    
    /**
     * CORRECCIÓN #10: Método público para que las subclases llamen
     * después de inicializar sus propios campos.
     * Reemplaza el antiguo logException() privado que se llamaba
     * desde el constructor de BaseException (antes de que la subclase
     * asignara sus campos).
     */
    protected void logCreation() {
        String logMessage = String.format("[%s] %s", errorCode, getMessage());
        
        switch (getLogLevel()) {
            case ERROR:
                if (getCause() != null) {
                    logger.error(logMessage, getCause());
                } else {
                    logger.error(logMessage);
                }
                break;
            case WARN:
                logger.warn(logMessage);
                break;
            case INFO:
                logger.info(logMessage);
                break;
            case DEBUG:
                logger.debug(logMessage);
                break;
        }
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getParams() {
        return params;
    }
    
    /**
     * Enum para niveles de logging
     */
    protected enum LogLevel {
        ERROR, WARN, INFO, DEBUG
    }
}