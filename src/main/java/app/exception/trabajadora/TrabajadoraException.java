package app.exception.trabajadora;

import app.exception.BaseException;

/**
 * Excepción base para errores relacionados con trabajadoras
 * Todas las excepciones de trabajadoras heredan de esta clase
 */
public class TrabajadoraException extends BaseException {

    private static final String DEFAULT_CODE = "TRABAJADORA_ERROR";

    public TrabajadoraException(String message) {
        super(message);
    }

    public TrabajadoraException(String message, Throwable cause) {
        super(message, cause);
    }

    protected TrabajadoraException(String errorCode, String message, Object... params) {
        super(errorCode, message, params);
    }

    protected TrabajadoraException(String errorCode, String message, Throwable cause, Object... params) {
        super(errorCode, message, cause, params);
    }

    @Override
    protected String getDefaultErrorCode() {
        return DEFAULT_CODE;
    }
}
