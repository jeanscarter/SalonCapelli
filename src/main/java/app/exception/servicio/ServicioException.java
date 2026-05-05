package app.exception.servicio;

import app.exception.BaseException;

/**
 * Excepción base para errores relacionados con servicios
 */
public class ServicioException extends BaseException {

    private static final String DEFAULT_CODE = "SERVICIO_ERROR";

    public ServicioException(String message) {
        super(message);
    }

    public ServicioException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ServicioException(String errorCode, String message, Object... params) {
        super(errorCode, message, params);
    }

    protected ServicioException(String errorCode, String message, Throwable cause, Object... params) {
        super(errorCode, message, cause, params);
    }

    @Override
    protected String getDefaultErrorCode() {
        return DEFAULT_CODE;
    }
}
