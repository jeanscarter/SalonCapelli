package app.exception.cliente;

/**
 * Excepción lanzada cuando se intenta crear un cliente duplicado
 * (cliente con cédula que ya existe)
 */
public class ClienteDuplicadoException extends ClienteException {
    
    private static final String ERROR_CODE = "CLIENTE_DUPLICADO";
    private final String cedula;
    
    /**
     * Constructor
     * 
     * @param cedula Cédula del cliente duplicado
     */
    public ClienteDuplicadoException(String cedula) {
        super(ERROR_CODE, "Ya existe un cliente con la cédula: %s", cedula);
        this.cedula = cedula;
    }
    
    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN; // Es un error de validación de negocio, no crítico
    }
    
    /**
     * Obtiene la cédula que causó el error
     * 
     * @return Cédula duplicada
     */
    public String getCedula() {
        return cedula;
    }
}