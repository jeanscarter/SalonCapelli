package app.exception;

/**
 * Excepción para errores de base de datos
 * Patrón: Exception Hierarchy
 */
public class DatabaseException extends BaseException {
    
    private static final String DEFAULT_CODE = "DB_ERROR";
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    @Override
    protected String getDefaultErrorCode() {
        return DEFAULT_CODE;
    }
    
    // Códigos de error específicos
    public static final String CONNECTION_ERROR = "DB_CONN_001";
    public static final String QUERY_ERROR = "DB_QUERY_002";
    public static final String CONSTRAINT_VIOLATION = "DB_CONSTRAINT_003";
    public static final String INITIALIZATION_ERROR = "DB_INIT_004";
    
    /**
     * Factory Methods para errores comunes
     */
    public static DatabaseException connectionFailed(Throwable cause) {
        return new DatabaseException(
            CONNECTION_ERROR,
            "Error al conectar con la base de datos",
            cause
        );
    }
    
    public static DatabaseException queryFailed(String query, Throwable cause) {
        return new DatabaseException(
            QUERY_ERROR,
            "Error ejecutando consulta: " + query,
            cause
        );
    }
    
    public static DatabaseException constraintViolation(String constraint, Throwable cause) {
        return new DatabaseException(
            CONSTRAINT_VIOLATION,
            "Violación de restricción: " + constraint,
            cause
        );
    }
    
    public static DatabaseException initializationFailed(Throwable cause) {
        return new DatabaseException(
            INITIALIZATION_ERROR,
            "Error al inicializar la base de datos",
            cause
        );
    }
}