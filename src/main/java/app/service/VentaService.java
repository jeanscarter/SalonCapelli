package app.service;

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import app.exception.ValidationException;
import app.model.Producto;
import app.model.Venta;
import app.model.VentaItem;
import app.repository.ProductoRepository;
import app.repository.ProductoRepositorySQLite;
import app.repository.VentaRepository;
import app.repository.VentaRepositorySQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Servicio principal de Ventas.
 * Orquesta la validación de inventario, generación de correlativos 
 * y delegación a la capa de persistencia transaccional.
 */
public class VentaService {

    private static final Logger logger = LoggerFactory.getLogger(VentaService.class);
    
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public VentaService() {
        this.ventaRepository = new VentaRepositorySQLite();
        this.productoRepository = new ProductoRepositorySQLite();
    }

    /**
     * Procesa una venta completa. Valida stock antes de intentar persistir.
     * Si todo es válido, genera el correlativo y delega a saveCompleteVenta.
     */
    public void procesarVenta(Venta venta) throws DatabaseException, ValidationException {
        logger.info("Iniciando procesamiento de venta...");

        // 1. Validaciones de Negocio previas a la BD
        validarVenta(venta);

        // 2. Validación de Stock Físico
        validarStockFisico(venta);

        // 3. Generar Correlativo
        String correlativo = generarProximoCorrelativo();
        venta.setNumeroCorrelativo(correlativo);
        logger.debug("Correlativo generado: {}", correlativo);

        // 4. Asegurar Tasa BCV
        if (venta.getTasaBcv() <= 0) {
            venta.setTasaBcv(BCVService.getCachedRate());
        }

        // 5. Persistencia Transaccional (todo o nada)
        ventaRepository.saveCompleteVenta(venta);
        
        logger.info("✓ Venta procesada y guardada con éxito. Correlativo: {}", correlativo);
    }

    private void validarVenta(Venta venta) throws ValidationException {
        java.util.List<ValidationException.ValidationError> errors = new java.util.ArrayList<>();

        if (venta.getItems() == null || venta.getItems().isEmpty()) {
            errors.add(new ValidationException.ValidationError("items", "La venta debe contener al menos un servicio (ítem)."));
        }

        if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
            errors.add(new ValidationException.ValidationError("pagos", "Debe registrar al menos un pago."));
        } else if (!venta.isPagada()) {
            errors.add(new ValidationException.ValidationError("monto", String.format("El monto pagado (%.2f) no cubre el total de la venta (%.2f).", 
                                            venta.getTotalPagado(), venta.getTotal())));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Verifica que los productos asociados a los ítems de venta tengan stock suficiente.
     * Como una venta puede usar el mismo producto múltiples veces, agrupamos la demanda.
     */
    private void validarStockFisico(Venta venta) throws DatabaseException, ValidationException {
        java.util.List<ValidationException.ValidationError> errors = new java.util.ArrayList<>();

        for (VentaItem item : venta.getItems()) {
            if (item.getProductoId() != null) {
                Producto p = productoRepository.findById(item.getProductoId());
                if (p == null) {
                    errors.add(new ValidationException.ValidationError("producto_" + item.getProductoId(), "El producto con ID " + item.getProductoId() + " no existe."));
                } else if (p.getStockActual() < 1) { // 1 es la cantidad fija actual por servicio
                    errors.add(new ValidationException.ValidationError("stock", String.format("Stock insuficiente para el producto '%s'. Stock actual: %d.", 
                                                    p.getNombre(), p.getStockActual())));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Genera un número correlativo consecutivo formateado a 6 dígitos (ej. 000125).
     * Usa la tabla app_settings para persistir el contador.
     */
    private synchronized String generarProximoCorrelativo() throws DatabaseException {
        String sqlSelect = "SELECT setting_value FROM app_settings WHERE setting_key = 'correlativo'";
        String sqlUpdate = "UPDATE app_settings SET setting_value = ? WHERE setting_key = 'correlativo'";
        
        try (Connection conn = DatabaseConnection.connect()) {
            int actual = 1;
            
            try (PreparedStatement pstmtSel = conn.prepareStatement(sqlSelect);
                 ResultSet rs = pstmtSel.executeQuery()) {
                if (rs.next()) {
                    actual = Integer.parseInt(rs.getString("setting_value"));
                }
            }
            
            int siguiente = actual + 1;
            
            try (PreparedStatement pstmtUpd = conn.prepareStatement(sqlUpdate)) {
                pstmtUpd.setString(1, String.valueOf(siguiente));
                pstmtUpd.executeUpdate();
            }
            
            return String.format("%06d", actual);
            
        } catch (SQLException | NumberFormatException e) {
            logger.error("Error generando correlativo", e);
            throw DatabaseException.queryFailed("GENERAR_CORRELATIVO", e);
        }
    }
}
