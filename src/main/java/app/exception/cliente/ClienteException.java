package app.exception.cliente;

import app.exception.BaseException;

/**
 * Excepción base para errores relacionados con clientes
 * Todas las excepciones de clientes heredan de esta clase
 */
public class ClienteException extends BaseException {
    
    private static final String DEFAULT_CODE = "CLIENTE_ERROR";
    
    public ClienteException(String message) {
        super(message);
    }
    
    public ClienteException(String message, Throwable cause) {
        super(message, cause);
    }
    
    protected ClienteException(String errorCode, String message, Object... params) {
        super(errorCode, message, params);
    }
    
    protected ClienteException(String errorCode, String message, Throwable cause, Object... params) {
        super(errorCode, message, cause, params);
    }
    
    @Override
    protected String getDefaultErrorCode() {
        return DEFAULT_CODE;
    }
}