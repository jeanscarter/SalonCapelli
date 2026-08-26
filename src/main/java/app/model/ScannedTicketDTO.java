package app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que representa la información extraída de un ticket de servicio
 * mediante procesamiento de visión local o servicio remoto.
 */
public class ScannedTicketDTO {

    private String clienteNombre;
    private String clienteCedula;
    private String clienteTelefono;
    private String trabajadoraNombre;
    private List<ScannedItemDTO> items;
    private String metodoPago;
    private String referenciaPago;
    private double totalDetectado;
    private String notas;

    // Campos de cabecera de comanda / factura
    private String numeroFactura;
    private String fecha;
    private double tasa;
    private String tasaOrigen;

    // Campos de metadatos del procesamiento
    private String processingMode;    // "LOCAL", "REMOTE", "HYBRID"
    private double confidenceScore;   // 0.0 - 1.0 confianza global

    public ScannedTicketDTO() {
        this.items = new ArrayList<>();
        this.processingMode = "LOCAL";
        this.confidenceScore = 0.0;
    }

    public static class ScannedItemDTO {
        private String descripcion;
        private double precio;
        private String tipoCabello; // "CORTO", "MEDIANO", "LARGO", "EXTENSIONES", etc.
        private boolean esProducto;
        private boolean esPropina;

        // Campos de metadatos por ítem
        private double confidence;         // 0.0 - 1.0 confianza de este ítem
        private int rowNumber;             // Fila del ticket (1-22)
        private String trabajadoraNombre;  // Colaboradora para este ítem específico
        private int matchedServicioId;     // ID del servicio coincidido en BD
        private int matchedTrabajadoraId;  // ID de la trabajadora coincidida en BD

        public ScannedItemDTO() {
        }

        public ScannedItemDTO(String descripcion, double precio, String tipoCabello, boolean esProducto) {
            this.descripcion = descripcion;
            this.precio = precio;
            this.tipoCabello = tipoCabello;
            this.esProducto = esProducto;
        }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public double getPrecio() { return precio; }
        public void setPrecio(double precio) { this.precio = precio; }

        public String getTipoCabello() { return tipoCabello; }
        public void setTipoCabello(String tipoCabello) { this.tipoCabello = tipoCabello; }

        public boolean isEsProducto() { return esProducto; }
        public void setEsProducto(boolean esProducto) { this.esProducto = esProducto; }

        public boolean isEsPropina() { return esPropina; }
        public void setEsPropina(boolean esPropina) { this.esPropina = esPropina; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

        public String getTrabajadoraNombre() { return trabajadoraNombre; }
        public void setTrabajadoraNombre(String trabajadoraNombre) { this.trabajadoraNombre = trabajadoraNombre; }

        public int getMatchedServicioId() { return matchedServicioId; }
        public void setMatchedServicioId(int matchedServicioId) { this.matchedServicioId = matchedServicioId; }

        public int getMatchedTrabajadoraId() { return matchedTrabajadoraId; }
        public void setMatchedTrabajadoraId(int matchedTrabajadoraId) { this.matchedTrabajadoraId = matchedTrabajadoraId; }

        @Override
        public String toString() {
            return descripcion + " - $" + String.format("%.2f", precio);
        }
    }

    // Getters y Setters
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteCedula() { return clienteCedula; }
    public void setClienteCedula(String clienteCedula) { this.clienteCedula = clienteCedula; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public String getTrabajadoraNombre() { return trabajadoraNombre; }
    public void setTrabajadoraNombre(String trabajadoraNombre) { this.trabajadoraNombre = trabajadoraNombre; }

    public List<ScannedItemDTO> getItems() { return items; }
    public void setItems(List<ScannedItemDTO> items) { this.items = items != null ? items : new ArrayList<>(); }
    public void addItem(ScannedItemDTO item) { if (this.items == null) this.items = new ArrayList<>(); this.items.add(item); }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public double getTotalDetectado() { return totalDetectado; }
    public void setTotalDetectado(double totalDetectado) { this.totalDetectado = totalDetectado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getTasa() { return tasa; }
    public void setTasa(double tasa) { this.tasa = tasa; }

    public String getTasaOrigen() { return tasaOrigen; }
    public void setTasaOrigen(String tasaOrigen) { this.tasaOrigen = tasaOrigen; }

    public String getProcessingMode() { return processingMode; }
    public void setProcessingMode(String processingMode) { this.processingMode = processingMode; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
}
