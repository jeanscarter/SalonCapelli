package app.exception.cliente;

/**
 * Excepción lanzada cuando no se encuentra un cliente
 * Patrón: Factory Method
 */
public class ClienteNotFoundException extends ClienteException {
    
    private static final String ERROR_CODE = "CLIENTE_NOT_FOUND";
    
    private ClienteNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
    
    @Override
    protected LogLevel getLogLevel() {
        return LogLevel.WARN; // No encontrar algo no es un error crítico
    }
    
    /**
     * Crea una excepción para cliente no encontrado por ID
     * 
     * @param id ID del cliente
     * @return ClienteNotFoundException
     */
    public static ClienteNotFoundException byId(int id) {
        return new ClienteNotFoundException(
            String.format("No se encontró cliente con ID: %d", id)
        );
    }
    
    /**
     * Crea una excepción para cliente no encontrado por cédula
     * 
     * @param cedula Cédula del cliente
     * @return ClienteNotFoundException
     */
    public static ClienteNotFoundException byCedula(String cedula) {
        return new ClienteNotFoundException(
            String.format("No se encontró cliente con cédula: %s", cedula)
        );
    }
}