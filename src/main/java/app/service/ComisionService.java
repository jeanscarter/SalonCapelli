package app.service;

import app.exception.DatabaseException;
import app.model.ReglaComisionDetallada;
import app.model.Servicio;
import app.repository.ReglaComisionDetalladaRepository;
import app.repository.ReglaComisionDetalladaRepositorySQLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motor de cálculo de comisiones.
 * 
 * Reemplaza completamente la lógica hardcoded de LEGACY PayrollService.calculateCommissionForItem().
 * 
 * Flujo de resolución:
 *   1. Busca en reglas_comision_detalladas la regla de mayor prioridad que aplique
 *   2. Si encuentra regla → calcula comisión (PORCENTAJE o MONTO_FIJO)
 *   3. Si no encuentra → retorna 0.0 (la categoría puede no tener comisión)
 */
public class ComisionService {

    private static final Logger logger = LoggerFactory.getLogger(ComisionService.class);
    private final ReglaComisionDetalladaRepository reglasRepo;

    public ComisionService() {
        this.reglasRepo = new ReglaComisionDetalladaRepositorySQLite();
    }

    public ComisionService(ReglaComisionDetalladaRepository reglasRepo) {
        this.reglasRepo = reglasRepo;
    }

    /**
     * Calcula la comisión a pagar para un ítem de venta.
     * 
     * @param trabajadoraId       ID de la trabajadora que realizó el servicio
     * @param servicioId          ID del servicio realizado
     * @param categoriaServicio   Categoría del servicio (Peluqueria, Lavado, Quimico, etc.)
     * @param precioVenta         Precio de venta aplicado
     * @param clienteTraeProducto Si el cliente trajo su propio producto
     * @return Monto de comisión a pagar (en USD)
     */
    public double calcularComision(int trabajadoraId, int servicioId, 
                                    String categoriaServicio, double precioVenta, 
                                    boolean clienteTraeProducto) {

        // Caso especial: PAGO-MANUAL → 100% es comisión/pago directo
        if ("PAGO-MANUAL".equalsIgnoreCase(categoriaServicio)) {
            logger.debug("PAGO-MANUAL detectado: comisión = precio total ${}", precioVenta);
            return precioVenta;
        }

        try {
            ReglaComisionDetallada regla = reglasRepo.findReglaMasEspecifica(
                    trabajadoraId, servicioId, categoriaServicio, 
                    clienteTraeProducto, precioVenta);

            if (regla != null) {
                double comision = regla.calcularComision(precioVenta);
                logger.debug("Regla aplicada [ID={}, prioridad={}, desc='{}']: comisión = ${}",
                        regla.getId(), regla.getPrioridad(), regla.getDescripcion(), 
                        String.format("%.2f", comision));
                return comision;
            }

            logger.debug("No se encontró regla de comisión para trabajadora={}, servicio={}, cat={}", 
                    trabajadoraId, servicioId, categoriaServicio);
            return 0.0;

        } catch (DatabaseException e) {
            logger.error("Error calculando comisión", e);
            return 0.0;
        }
    }

    /**
     * Calcula la comisión para un servicio específico con datos del modelo
     */
    public double calcularComision(int trabajadoraId, Servicio servicio, 
                                    double precioVenta, boolean clienteTraeProducto) {
        String categoria = servicio.getCategoria() != null 
                ? servicio.getCategoria().getLabel() 
                : null;
        return calcularComision(trabajadoraId, servicio.getId(), categoria, precioVenta, clienteTraeProducto);
    }

    /**
     * Calcula la ganancia de la empresa para un ítem (precio - comisión)
     */
    public double calcularGananciaEmpresa(int trabajadoraId, int servicioId, 
                                           String categoriaServicio, double precioVenta, 
                                           boolean clienteTraeProducto) {
        double comision = calcularComision(trabajadoraId, servicioId, 
                categoriaServicio, precioVenta, clienteTraeProducto);
        return precioVenta - comision;
    }
}
