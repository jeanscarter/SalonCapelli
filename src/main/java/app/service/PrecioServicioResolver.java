package app.service;

import app.model.Cliente;
import app.model.Servicio;
import app.model.TipoCabello;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de dominio que resuelve el precio final de un servicio
 * según el tipo de cabello del cliente.
 *
 * NUEVA LÓGICA DE NEGOCIO: El precio es dinámico según la característica del cliente.
 */
public class PrecioServicioResolver {

    private static final Logger logger = LoggerFactory.getLogger(PrecioServicioResolver.class);

    private static PrecioServicioResolver instance;

    private PrecioServicioResolver() {}

    public static PrecioServicioResolver getInstance() {
        if (instance == null) {
            synchronized (PrecioServicioResolver.class) {
                if (instance == null) {
                    instance = new PrecioServicioResolver();
                }
            }
        }
        return instance;
    }

    /**
     * Resuelve el precio exacto de un servicio para un cliente específico.
     *
     * @param servicio El servicio a cotizar
     * @param cliente  El cliente que solicita el servicio
     * @return Precio final basado en el TipoCabello del cliente
     */
    public double resolverPrecio(Servicio servicio, Cliente cliente) {
        if (servicio == null || cliente == null) {
            logger.warn("Servicio o cliente nulo al resolver precio");
            return 0.0;
        }

        TipoCabello tipo = cliente.getTipoCabello();
        if (tipo == null) {
            logger.warn("Cliente sin tipo de cabello definido, usando precio CORTO por defecto");
            tipo = TipoCabello.CORTO;
        }

        double precio = servicio.getPrecio(tipo);

        logger.debug("Precio resuelto: Servicio='{}', Cliente='{}', TipoCabello={}, Precio={}",
                servicio.getNombre(), cliente.getNombreCompleto(), tipo, precio);

        return precio;
    }

    /**
     * Resuelve el precio considerando si el cliente trae su propio producto.
     *
     * @param servicio              El servicio a cotizar
     * @param cliente               El cliente que solicita el servicio
     * @param clienteTraeProducto   true si el cliente trae su propio producto
     * @return Precio final ajustado
     */
    public double resolverPrecio(Servicio servicio, Cliente cliente, boolean clienteTraeProducto) {
        if (clienteTraeProducto && servicio.isPermiteClienteProducto()) {
            logger.debug("Cliente trae producto propio. Precio especial: {}", servicio.getPrecioClienteProducto());
            return servicio.getPrecioClienteProducto();
        }
        return resolverPrecio(servicio, cliente);
    }

    /**
     * Resuelve el precio directamente por tipo de cabello (sin objeto Cliente).
     *
     * @param servicio    El servicio
     * @param tipoCabello El tipo de cabello
     * @return Precio correspondiente
     */
    public double resolverPrecio(Servicio servicio, TipoCabello tipoCabello) {
        if (servicio == null || tipoCabello == null) return 0.0;
        return servicio.getPrecio(tipoCabello);
    }
}
