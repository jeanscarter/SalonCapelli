package app.view;

import app.service.ReporteService;
import app.service.ReporteService.DailyRow;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Vista de Reporte Semanal / Rango de Fechas.
 * Migrado desde: WeeklyReportWindow.java (CapelliSalesWindow)
 * Adaptado al look & feel nativo de SalonCapelli.
 *
 * Estructura:
 * ┌──────────────────────────────────────────────────────┐
 * │  [Desde] [Hasta] [Generar] [Exportar CSV]           │
 * ├──────────────────────────────────────────────────────┤
 * │  Tabla (fecha | efectivo | pdv | zelle | cxc | rosa │
 * │        | otros | TOTAL)                             │
 * ├────────────────────────┬─────────────────────────────┤
 * │ Resumen por Cuenta     │  Resumen por Método Pago    │
 * └────────────────────────┴─────────────────────────────┘
 */
public class ReporteSemanalView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ReporteSemanalView.class);
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReporteService reporteService;

    // Controles
    private JSpinner spinnerDesde;
    private JSpinner spinnerHasta;

    // Tabla
    private DefaultTableModel tblModel;
    private JTable reportTable;

    // Totales
    private JLabel lblTotalPeriodo;

    // Resúmenes
    private JPanel panelResumenCuenta;
    private JPanel panelResumenMetodo;

    public ReporteSemanalView() {
        this.reporteService = new ReporteService();
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20, gap 15", "[grow, fill]", "[][grow, fill][]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // === Título + Controles ===
        add(createControlPanel(), "growx, wrap");

        // === Tabla Principal ===
        add(createTablePanel(), "grow, wrap");

        // === Panel inferior: Resúmenes + Total ===
        add(createBottomPanel(), "growx");
    }

    // ======================================================
    // PANEL: Controles superiores
    // ======================================================

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 10 15 10 15, fillx", "[]5[]15[]5[]20[]15[]push[]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background");

        JLabel title = new JLabel("Reporte Semanal");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Component.accentColor");
        panel.add(title, "span, wrap, gapy 0 10");

        // Fechas: por defecto últimos 7 días
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Date sevenDaysAgo = cal.getTime();

        panel.add(new JLabel("Desde:"));
        spinnerDesde = new JSpinner(new SpinnerDateModel(sevenDaysAgo, null, null, Calendar.DAY_OF_MONTH));
        spinnerDesde.setEditor(new JSpinner.DateEditor(spinnerDesde, "dd/MM/yyyy"));
        panel.add(spinnerDesde, "w 130!");

        panel.add(new JLabel("Hasta:"));
        spinnerHasta = new JSpinner(new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH));
        spinnerHasta.setEditor(new JSpinner.DateEditor(spinnerHasta, "dd/MM/yyyy"));
        panel.add(spinnerHasta, "w 130!");

        JButton btnGenerar = new JButton("Generar Reporte");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE,
            "arc:10; background:$Component.accentColor; foreground:#fff; font:bold");
        btnGenerar.addActionListener(e -> generarReporte());
        panel.add(btnGenerar);

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.putClientProperty(FlatClientProperties.STYLE, "arc:10; font:bold");
        btnExportar.addActionListener(e -> exportarCSV());
        panel.add(btnExportar);

        lblTotalPeriodo = new JLabel("Total: $ 0.00");
        lblTotalPeriodo.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Success.color");
        panel.add(lblTotalPeriodo);

        return panel;
    }

    // ======================================================
    // PANEL: Tabla principal
    // ======================================================

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[grow, fill]", "[grow, fill]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        String[] columns = {
            "Fecha",
            "Efectivo ($)",
            "PdV/PM Capelli ($)",
            "Zelle ($)",
            "Ctas por Cobrar ($)",
            "Cta. Rosa ($)",
            "Otros ($)",
            "TOTAL DÍA ($)"
        };

        tblModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        reportTable = new JTable(tblModel);
        reportTable.setRowHeight(30);
        reportTable.putClientProperty(FlatClientProperties.STYLE,
            "showHorizontalLines:true; intercellSpacing:0,1");

        // Alinear números a la derecha
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 1; i < columns.length; i++) {
            reportTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        // Columna TOTAL en negrita
        DefaultTableCellRenderer boldRightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(JLabel.RIGHT);
                return c;
            }
        };
        reportTable.getColumnModel().getColumn(columns.length - 1).setCellRenderer(boldRightRenderer);

        JScrollPane scroll = new JScrollPane(reportTable);
        panel.add(scroll, "grow");

        return panel;
    }

    // ======================================================
    // PANEL: Resúmenes inferiores
    // ======================================================

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0, gap 15", "[grow, fill][grow, fill]", "[fill]"));
        panel.setOpaque(false);

        // Resumen por Cuenta
        panelResumenCuenta = new JPanel(new MigLayout("fillx, insets 15, wrap 1", "[grow, fill]", "[]"));
        panelResumenCuenta.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");
        JLabel lblCuenta = new JLabel("Resumen por Cuenta Receptora");
        lblCuenta.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panelResumenCuenta.add(lblCuenta);
        panel.add(panelResumenCuenta);

        // Resumen por Método
        panelResumenMetodo = new JPanel(new MigLayout("fillx, insets 15, wrap 1", "[grow, fill]", "[]"));
        panelResumenMetodo.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");
        JLabel lblMetodo = new JLabel("Resumen por Método de Pago");
        lblMetodo.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panelResumenMetodo.add(lblMetodo);
        panel.add(panelResumenMetodo);

        return panel;
    }

    // ======================================================
    // LÓGICA: Generar Reporte
    // ======================================================

    private void generarReporte() {
        Date startDate = (Date) spinnerDesde.getValue();
        Date endDate = (Date) spinnerHasta.getValue();

        if (startDate.after(endDate)) {
            ToastNotification.showError(this, "Error", "La fecha inicio no puede ser posterior a la fecha fin.");
            return;
        }

        LocalDate desde = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate hasta = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        logger.info("Generando reporte semanal: {} → {}", desde, hasta);

        tblModel.setRowCount(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<DailyRow> rows;
            Map<String, Double> porCuenta;
            Map<String, Double> porMetodo;

            @Override
            protected Void doInBackground() throws Exception {
                rows = reporteService.calcularReporteRango(desde, hasta);
                porCuenta = reporteService.getIngresoPorCuentaReceptora(desde, hasta);
                porMetodo = reporteService.getIngresoPorMetodoPago(desde, hasta);
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();

                    // Acumuladores de totales por rubro
                    double sumEfectivo = 0, sumCapelli = 0, sumZelle = 0;
                    double sumCxc = 0, sumRosa = 0, sumOtros = 0, sumTotal = 0;

                    for (DailyRow row : rows) {
                        if (row.getTotalDia() < 0.01) continue; // Omitir días sin movimiento

                        tblModel.addRow(new Object[]{
                            row.fecha().format(FMT_DISPLAY),
                            DF.format(row.efectivoUsd()),
                            DF.format(row.capelliConvertidoUsd()),
                            DF.format(row.zelleUsd()),
                            DF.format(row.cxcUsd()),
                            DF.format(row.rosaConvertidoUsd()),
                            DF.format(row.otrosUsd()),
                            DF.format(row.getTotalDia())
                        });

                        sumEfectivo += row.efectivoUsd();
                        sumCapelli += row.capelliConvertidoUsd();
                        sumZelle += row.zelleUsd();
                        sumCxc += row.cxcUsd();
                        sumRosa += row.rosaConvertidoUsd();
                        sumOtros += row.otrosUsd();
                        sumTotal += row.getTotalDia();
                    }

                    // Fila de TOTALES
                    tblModel.addRow(new Object[]{"", "", "", "", "", "", "", ""});
                    tblModel.addRow(new Object[]{
                        "TOTALES:",
                        DF.format(sumEfectivo),
                        DF.format(sumCapelli),
                        DF.format(sumZelle),
                        DF.format(sumCxc),
                        DF.format(sumRosa),
                        DF.format(sumOtros),
                        DF.format(sumTotal)
                    });

                    lblTotalPeriodo.setText("Total: $ " + DF.format(sumTotal));

                    // Resúmenes
                    llenarPanelResumen(panelResumenCuenta, "Resumen por Cuenta Receptora", porCuenta);
                    llenarPanelResumen(panelResumenMetodo, "Resumen por Método de Pago", porMetodo);

                    revalidate();
                    repaint();

                    ToastNotification.showSuccess(ReporteSemanalView.this, "Reporte Generado",
                        "Periodo: " + desde.format(FMT_DISPLAY) + " al " + hasta.format(FMT_DISPLAY));

                } catch (Exception e) {
                    logger.error("Error generando reporte semanal", e);
                    ToastNotification.showError(ReporteSemanalView.this, "Error",
                        "No se pudo generar el reporte: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ======================================================
    // EXPORTAR CSV
    // ======================================================

    private void exportarCSV() {
        if (tblModel.getRowCount() == 0) {
            ToastNotification.showWarning(this, "Genere un reporte primero.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte Semanal");
        fileChooser.setSelectedFile(new File("Reporte_Semanal_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
                // Headers
                for (int i = 0; i < tblModel.getColumnCount(); i++) {
                    fw.write(tblModel.getColumnName(i));
                    if (i < tblModel.getColumnCount() - 1) fw.write(",");
                }
                fw.write("\n");

                // Data
                for (int i = 0; i < tblModel.getRowCount(); i++) {
                    for (int j = 0; j < tblModel.getColumnCount(); j++) {
                        Object val = tblModel.getValueAt(i, j);
                        String str = (val == null) ? "" : val.toString().replace(",", ".");
                        fw.write(str);
                        if (j < tblModel.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                }

                ToastNotification.showSuccess(this, "Exportado", "Archivo CSV guardado exitosamente.");
            } catch (IOException ex) {
                ToastNotification.showError(this, "Error de Exportación", ex.getMessage());
            }
        }
    }

    // ======================================================
    // UTILIDAD: Llenar panel de resumen
    // ======================================================

    private void llenarPanelResumen(JPanel panel, String titulo, Map<String, Double> datos) {
        panel.removeAll();

        JLabel lblTitle = new JLabel(titulo);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panel.add(lblTitle);

        if (datos == null || datos.isEmpty()) {
            JLabel lbl = new JLabel("Sin datos para este periodo");
            lbl.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
            panel.add(lbl);
            return;
        }

        double total = 0;
        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            JPanel row = new JPanel(new MigLayout("insets 3 0 3 0, fillx", "[grow][]", "[]"));
            row.setOpaque(false);

            JLabel lblName = new JLabel(entry.getKey());
            row.add(lblName);

            JLabel lblValue = new JLabel("$ " + DF.format(entry.getValue()));
            lblValue.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
            row.add(lblValue);

            panel.add(row);
            total += entry.getValue();
        }

        // Separador y total
        panel.add(new JSeparator(), "growx, gapy 5");
        JPanel rowTotal = new JPanel(new MigLayout("insets 3 0 3 0, fillx", "[grow][]", "[]"));
        rowTotal.setOpaque(false);
        JLabel lblTotLabel = new JLabel("Total");
        lblTotLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        rowTotal.add(lblTotLabel);
        JLabel lblTotVal = new JLabel("$ " + DF.format(total));
        lblTotVal.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Success.color");
        rowTotal.add(lblTotVal);
        panel.add(rowTotal);
    }
}
