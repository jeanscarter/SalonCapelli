package app.service;

import app.model.Pago;
import app.model.Propina;
import app.model.Venta;
import app.model.VentaItem;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class TicketPDFService {

    private static final Logger logger = LoggerFactory.getLogger(TicketPDFService.class);
    private static final String DIR_TICKETS = "Tickets_Capelli";
    
    // Ancho 80mm en puntos (aprox 226pt). Alto grande para ticket dinámico.
    private static final float TICKET_WIDTH = 226f;
    private static final float TICKET_HEIGHT = 1000f; // Alto dinámico

    public static void generateAndOpenTicket(Venta venta, String nombreCliente) {
        try {
            Files.createDirectories(Paths.get(DIR_TICKETS));
            
            String fileName = String.format("%s/Factura_%s_%s.pdf", 
                DIR_TICKETS, venta.getNumeroCorrelativo(), nombreCliente.replaceAll("[^a-zA-Z0-9.-]", "_"));
                
            File file = new File(fileName);
            
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            
            // Ticket de 80mm
            PageSize pageSize = new PageSize(TICKET_WIDTH, TICKET_HEIGHT);
            Document document = new Document(pdf, pageSize);
            document.setMargins(10, 10, 10, 10);
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            // Cabecera
            document.add(new Paragraph("SALON DE BELLEZA CAPELLI")
                    .setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("J-123456789")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Factura: #" + venta.getNumeroCorrelativo())
                    .setBold().setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Fecha: " + venta.getFechaVenta().format(dtf))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            
            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
                    
            // Datos del cliente
            document.add(new Paragraph("Cliente: " + nombreCliente)
                    .setFontSize(9).setBold());
            
            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
                    
            // Ítems
            Table tableItems = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            for (VentaItem item : venta.getItems()) {
                tableItems.addCell(new Cell().add(new Paragraph(item.getNombreServicio()).setFontSize(8)).setBorder(Border.NO_BORDER));
                tableItems.addCell(new Cell().add(new Paragraph(String.format("$%.2f", item.getPrecioVenta())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
                tableItems.addCell(new Cell(1, 2).add(new Paragraph("  Trab: " + item.getNombreTrabajadora()).setFontSize(7).setItalic()).setBorder(Border.NO_BORDER));
            }
            document.add(tableItems);
            
            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
                    
            // Totales
            Table tableTotales = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            tableTotales.addCell(new Cell().add(new Paragraph("Subtotal:").setFontSize(8)).setBorder(Border.NO_BORDER));
            tableTotales.addCell(new Cell().add(new Paragraph(String.format("$%.2f", venta.getSubtotal())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            
            if (venta.getMontoDescuento() > 0) {
                tableTotales.addCell(new Cell().add(new Paragraph("Descuento:").setFontSize(8)).setBorder(Border.NO_BORDER));
                tableTotales.addCell(new Cell().add(new Paragraph(String.format("-$%.2f", venta.getMontoDescuento())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            }
            
            tableTotales.addCell(new Cell().add(new Paragraph("IVA (16%):").setFontSize(8)).setBorder(Border.NO_BORDER));
            tableTotales.addCell(new Cell().add(new Paragraph(String.format("$%.2f", venta.getMontoIva())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            
            tableTotales.addCell(new Cell().add(new Paragraph("TOTAL USD:").setFontSize(10).setBold()).setBorder(Border.NO_BORDER));
            tableTotales.addCell(new Cell().add(new Paragraph(String.format("$%.2f", venta.getTotal())).setFontSize(10).setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            
            tableTotales.addCell(new Cell().add(new Paragraph("Tasa BCV:").setFontSize(8)).setBorder(Border.NO_BORDER));
            tableTotales.addCell(new Cell().add(new Paragraph(String.format("%.2f", venta.getTasaBcv())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            
            tableTotales.addCell(new Cell().add(new Paragraph("TOTAL BS:").setFontSize(9).setBold()).setBorder(Border.NO_BORDER));
            tableTotales.addCell(new Cell().add(new Paragraph(String.format("Bs %.2f", venta.getTotal() * venta.getTasaBcv())).setFontSize(9).setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            
            document.add(tableTotales);
            
            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            
            // Pagos
            if (!venta.getPagos().isEmpty()) {
                document.add(new Paragraph("PAGOS:").setFontSize(8).setBold());
                Table tablePagos = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
                for (Pago p : venta.getPagos()) {
                    tablePagos.addCell(new Cell().add(new Paragraph(p.getMetodoPago() + " (" + p.getMoneda() + ")").setFontSize(8)).setBorder(Border.NO_BORDER));
                    tablePagos.addCell(new Cell().add(new Paragraph(String.format("%.2f", p.getMonto())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
                }
                document.add(tablePagos);
            }
            
            // Estatus / Saldo
            if ("PENDIENTE".equals(venta.getEstatus()) || "PARCIAL".equals(venta.getEstatus())) {
                document.add(new Paragraph("ESTATUS: DEUDA PENDIENTE")
                    .setFontSize(9).setBold().setTextAlignment(TextAlignment.CENTER));
            } else {
                document.add(new Paragraph("ESTATUS: PAGADA")
                    .setFontSize(9).setBold().setTextAlignment(TextAlignment.CENTER));
            }
            
            // Propinas
            if (!venta.getPropinas().isEmpty()) {
                document.add(new Paragraph("----------------------------------------")
                        .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("PROPINAS:").setFontSize(8).setBold());
                Table tablePropinas = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
                for (Propina p : venta.getPropinas()) {
                    tablePropinas.addCell(new Cell().add(new Paragraph(p.getNombreTrabajadora()).setFontSize(8)).setBorder(Border.NO_BORDER));
                    tablePropinas.addCell(new Cell().add(new Paragraph(String.format("$%.2f", p.getMonto())).setFontSize(8).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
                }
                document.add(tablePropinas);
            }
            
            document.add(new Paragraph("----------------------------------------")
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("¡Gracias por su visita!")
                    .setFontSize(9).setItalic().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Este documento no es válido para efectos fiscales")
                    .setFontSize(6).setTextAlignment(TextAlignment.CENTER));

            document.close();
            
            logger.info("Ticket generado exitosamente: {}", fileName);
            
            // Abrir archivo automáticamente
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            }
            
        } catch (Exception e) {
            logger.error("Error al generar PDF del ticket", e);
        }
    }
}
