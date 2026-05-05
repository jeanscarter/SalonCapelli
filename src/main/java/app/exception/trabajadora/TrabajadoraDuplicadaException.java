package app.exception.trabajadora;

/**
 * Excepción lanzada cuando se intenta crear una trabajadora con cédula duplicada
 */
public class TrabajadoraDuplicadaException extends TrabajadoraException {

    private static final String ERROR_CODE = "TRABAJADORA_DUPLICADA";
    private final String cedula;

    public TrabajadoraDuplicadaException(String cedula) {
        super(ERROR_CODE, "Ya existe una trabajadora con la cédula: %s", cedula);
        this.cedula = cedula;
    }

    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN;
    }

    public String getCedula() {
        return cedula;
    }
}
