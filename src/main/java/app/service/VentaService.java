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
import app.repository.CuentaPorCobrarRepository;
import app.repository.CuentaPorCobrarRepositorySQLite;
import app.model.CuentaPorCobrar;
import app.model.Pago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Servicio principal de Ventas.
 * Orquesta la validación de inventario, generación de correlativos 
 * y delegación a la capa de persistencia transaccional.
 *
 * Fase 2: Soporta modo histórico (Ctrl+F4) donde el correlativo,
 * fecha y tasa BCV son proporcionados externamente.
 */
public class VentaService {

    private static final Logger logger = LoggerFactory.getLogger(VentaService.class);
    
    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final CuentaPorCobrarRepository cxcRepository;

    public VentaService() {
        this.ventaRepository = new VentaRepositorySQLite();
        this.productoRepository = new ProductoRepositorySQLite();
        this.cxcRepository = new CuentaPorCobrarRepositorySQLite();
    }

    /**
     * Procesa una venta completa. Valida stock antes de intentar persistir.
     * Si todo es válido, genera el correlativo y delega a saveCompleteVenta.
     *
     * Fase 2: Si la venta ya tiene correlativo pre-asignado (modo histórico),
     * no genera uno nuevo ni consulta la tasa BCV.
     */
    public void procesarVenta(Venta venta) throws DatabaseException, ValidationException {
        logger.info("Iniciando procesamiento de venta...");

        // 1. Validaciones de Negocio previas a la BD
        validarVenta(venta);

        // 2. Validación de Stock Físico
        validarStockFisico(venta);

        // 3. Generar Correlativo (solo si no viene pre-asignado — modo histórico)
        if (venta.getNumeroCorrelativo() == null || venta.getNumeroCorrelativo().isBlank()) {
            String correlativo = generarProximoCorrelativo();
            venta.setNumeroCorrelativo(correlativo);
            logger.debug("Correlativo auto-generado: {}", correlativo);
        } else {
            logger.debug("Usando correlativo pre-asignado (modo histórico): {}", venta.getNumeroCorrelativo());
        }

        // 4. Asegurar Tasa BCV (solo si no viene pre-asignada)
        if (venta.getTasaBcv() <= 0) {
            venta.setTasaBcv(BCVService.getCachedRate());
        }

        // 5. Persistencia Transaccional (todo o nada)
        ventaRepository.saveCompleteVenta(venta);
        
        // 6. Fase 4.5: Registrar CxC si la venta no está PAGADA
        if (!"PAGADA".equals(venta.getEstatus()) && venta.getClienteId() != null) {
            double totalPagado = 0.0;
            for (Pago p : venta.getPagos()) {
                if ("Bs".equals(p.getMoneda()) && p.getTasaBcvAlPago() > 0) {
                    totalPagado += p.getMonto() / p.getTasaBcvAlPago();
                } else {
                    totalPagado += p.getMonto();
                }
            }
            double pendiente = venta.getTotal() - totalPagado;
            if (pendiente > 0.01) {
                CuentaPorCobrar cxc = new CuentaPorCobrar();
                cxc.setClienteId(venta.getClienteId());
                cxc.setVentaId(venta.getId());
                cxc.setMontoOriginal(venta.getTotal());
                cxc.setMontoPendiente(pendiente);
                cxc.setEstatus(venta.getEstatus());
                cxc.setFechaCreacion(venta.getFechaVenta());
                cxcRepository.save(cxc);
                logger.info("Cuenta por Cobrar registrada: Cliente={}, Monto=${}", venta.getClienteId(), pendiente);
            }
        }
        
        logger.info("✓ Venta procesada y guardada con éxito. Correlativo: {}", venta.getNumeroCorrelativo());
    }

    /**
     * Obtiene el valor actual del correlativo (sin incrementar).
     * Usado para mostrar el próximo número de factura en la UI.
     *
     * @return Correlativo formateado a 6 dígitos (ej. "000042")
     */
    public String obtenerCorrelativoActual() throws DatabaseException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'correlativo'";
        
        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                int valor = Integer.parseInt(rs.getString("setting_value"));
                return String.format("%06d", valor);
            }
            
            return "000001";
            
        } catch (SQLException | NumberFormatException e) {
            logger.error("Error leyendo correlativo actual", e);
            throw DatabaseException.queryFailed("LEER_CORRELATIVO", e);
        }
    }

    /**
     * Busca la tasa BCV utilizada en ventas de una fecha específica.
     * Delegación directa al repositorio para uso desde la UI.
     */
    public Double buscarTasaBcvPorFecha(LocalDate fecha) throws DatabaseException {
        return ventaRepository.findTasaBcvByFecha(fecha);
    }

    /* CORRECCIÓN #1: Ventas a crédito — validación condicional de pagos según estatus */
    private void validarVenta(Venta venta) throws ValidationException {
        java.util.List<ValidationException.ValidationError> errors = new java.util.ArrayList<>();

        if (venta.getItems() == null || venta.getItems().isEmpty()) {
            errors.add(new ValidationException.ValidationError("items", "La venta debe contener al menos un servicio (ítem)."));
        }

        String estatus = venta.getEstatus();
        boolean esCreditoOParcial = "PENDIENTE".equals(estatus) || "PARCIAL".equals(estatus);

        if (esCreditoOParcial) {
            // Ventas a crédito: pagos opcionales (puede haber 0 pagos o pago parcial)
            // La UI ya validó con el usuario que acepta registrar como CxC
            logger.debug("Venta con estatus '{}' — validación de pagos flexible", estatus);
        } else {
            // Ventas PAGADA (o sin estatus definido): pagos obligatorios y cobertura total
            if (venta.getPagos() == null || venta.getPagos().isEmpty()) {
                errors.add(new ValidationException.ValidationError("pagos", "Debe registrar al menos un pago."));
            } else if (!venta.isPagada()) {
                errors.add(new ValidationException.ValidationError("monto", String.format(
                    "El monto pagado (%.2f) no cubre el total de la venta (%.2f).",
                    venta.getTotalPagado(), venta.getTotal())));
            }
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
