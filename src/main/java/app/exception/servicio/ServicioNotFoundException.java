package app.exception.servicio;

/**
 * Excepción lanzada cuando no se encuentra un servicio
 * Patrón: Factory Method
 */
public class ServicioNotFoundException extends ServicioException {

    private static final String ERROR_CODE = "SERVICIO_NOT_FOUND";

    private ServicioNotFoundException(String message) {
        super(ERROR_CODE, message);
        logCreation(); // CORRECCIÓN #10
    }

    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN;
    }

    public static ServicioNotFoundException byId(int id) {
        return new ServicioNotFoundException(
            String.format("No se encontró servicio con ID: %d", id)
        );
    }

    public static ServicioNotFoundException byNombre(String nombre) {
        return new ServicioNotFoundException(
            String.format("No se encontró servicio: %s", nombre)
        );
    }
}
