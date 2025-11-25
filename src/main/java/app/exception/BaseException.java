package app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase base para todas las excepciones del sistema
 * Patrón: Template Method
 * 
 * Proporciona logging automático y estructura común
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
        logException();
    }
    
    /**
     * Constructor con mensaje y causa
     */
    protected BaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = getDefaultErrorCode();
        this.params = new Object[0];
        logException();
    }
    
    /**
     * Constructor completo con código de error y parámetros
     */
    protected BaseException(String errorCode, String message, Object... params) {
        super(String.format(message, params));
        this.errorCode = errorCode;
        this.params = params;
        logException();
    }
    
    /**
     * Constructor completo con causa
     */
    protected BaseException(String errorCode, String message, Throwable cause, Object... params) {
        super(String.format(message, params), cause);
        this.errorCode = errorCode;
        this.params = params;
        logException();
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
     * Hook Method - Log automático de la excepción
     */
    private void logException() {
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