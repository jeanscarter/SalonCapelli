package app.exception.trabajadora;

/**
 * Excepción lanzada cuando no se encuentra una trabajadora
 * Patrón: Factory Method
 */
public class TrabajadoraNotFoundException extends TrabajadoraException {

    private static final String ERROR_CODE = "TRABAJADORA_NOT_FOUND";

    private TrabajadoraNotFoundException(String message) {
        super(ERROR_CODE, message);
    }

    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN;
    }

    public static TrabajadoraNotFoundException byId(int id) {
        return new TrabajadoraNotFoundException(
            String.format("No se encontró trabajadora con ID: %d", id)
        );
    }

    public static TrabajadoraNotFoundException byCedula(String cedula) {
        return new TrabajadoraNotFoundException(
            String.format("No se encontró trabajadora con cédula: %s", cedula)
        );
    }
}
