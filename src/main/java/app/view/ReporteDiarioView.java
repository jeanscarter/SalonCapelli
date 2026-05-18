package app.view;

import app.service.ReporteService;
import app.service.ReporteService.DailyStats;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Vista de Reporte Diario de Operaciones.
 * Migrado desde: DailyReportWindow.java (CapelliSalesWindow)
 * Adaptado al look & feel nativo de SalonCapelli.
 *
 * Estructura:
 * ┌──────────────────────────────────────────────┐
 * │  Selector de fecha  │  Botón consultar       │
 * ├──────────────────────────────────────────────┤
 * │  Tarjetas KPI (Efectivo, PdV, Zelle, etc.)  │
 * ├──────────────────────────────────────────────┤
 * │  Tabla detalle facturas │  Panel Resúmenes   │
 * └──────────────────────────────────────────────┘
 */
public class ReporteDiarioView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ReporteDiarioView.class);
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    private final ReporteService reporteService;

    // Controles
    private JSpinner dateSpinner;

    // KPI Labels
    private JLabel lblTasaUsada;
    private JLabel lblEfectivoUsd;
    private JLabel lblPtoVentaBs;
    private JLabel lblPagosRosa;
    private JLabel lblZelleUsd;
    private JLabel lblCxcUsd;
    private JLabel lblOtrosUsd;
    private JLabel lblTotalIva;
    private JLabel lblTotalGeneral;

    // Tabla detalle
    private javax.swing.table.DefaultTableModel tblModel;

    // Resúmenes
    private JPanel panelResumenCuenta;
    private JPanel panelResumenMetodo;

    public ReporteDiarioView() {
        this.reporteService = new ReporteService();
        init();
        cargarReporte();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20, gap 15", "[grow, fill]", "[][][][grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // === Título ===
        JLabel title = new JLabel("Reporte Diario de Operaciones");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +12; foreground:$Component.accentColor");
        add(title, "wrap");

        // === Panel Selector Fecha ===
        add(createDateSelectorPanel(), "growx, wrap");

        // === Panel KPIs ===
        add(createKpiPanel(), "growx, wrap");

        // === Panel inferior: Tabla + Resúmenes ===
        JPanel pnlBottom = new JPanel(new MigLayout("fill, insets 0, gap 15", "[60%, grow, fill][40%, grow, fill]", "[grow, fill]"));
        pnlBottom.setOpaque(false);
        pnlBottom.add(createDetallePanel());
        pnlBottom.add(createResumenPanel());
        add(pnlBottom, "grow");
    }

    // ======================================================
    // PANEL: Selector de Fecha
    // ======================================================

    private JPanel createDateSelectorPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 10 15 10 15, fillx", "[]10[150!]20[]20[]push[]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background");

        panel.add(new JLabel("Fecha:"));

        dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy");
        dateSpinner.setEditor(dateEditor);
        panel.add(dateSpinner);

        JButton btnConsultar = new JButton("Consultar");
        btnConsultar.putClientProperty(FlatClientProperties.STYLE,
            "arc:10; background:$Component.accentColor; foreground:#fff; font:bold");
        btnConsultar.addActionListener(e -> cargarReporte());
        panel.add(btnConsultar);

        lblTasaUsada = new JLabel("Tasa: --");
        lblTasaUsada.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Label.disabledForeground");
        panel.add(lblTasaUsada);

        // Total general grande
        lblTotalGeneral = new JLabel("$ 0.00");
        lblTotalGeneral.putClientProperty(FlatClientProperties.STYLE, "font:bold +14; foreground:$Success.color");
        panel.add(lblTotalGeneral);

        return panel;
    }

    // ======================================================
    // PANEL: KPIs (Tarjetas de métricas)
    // ======================================================

    private JPanel createKpiPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, gap 10", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        panel.setOpaque(false);

        lblEfectivoUsd = new JLabel("$ 0.00");
        panel.add(createKpiCard("Efectivo ($)", lblEfectivoUsd, "$Component.accentColor"));

        lblPtoVentaBs = new JLabel("Bs 0.00");
        panel.add(createKpiCard("PdV / PM Capelli", lblPtoVentaBs, "$Component.accentColor"));

        lblPagosRosa = new JLabel("Bs 0.00");
        panel.add(createKpiCard("Pagos Cta. Rosa", lblPagosRosa, "$Warning.color"));

        lblZelleUsd = new JLabel("$ 0.00");
        panel.add(createKpiCard("Zelle ($)", lblZelleUsd, "$Success.color"));

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String accentVar) {
        JPanel card = new JPanel(new MigLayout("fill, insets 12 15 12 15", "[grow]", "[]5[]"));
        card.putClientProperty(FlatClientProperties.STYLE,
            "arc:12; background:$Panel.background; border:0,0,3,0," + accentVar);

        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground; font:-1");
        card.add(lblTitle, "wrap");

        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:" + accentVar);
        card.add(valueLabel);

        return card;
    }

    // ======================================================
    // PANEL: Tabla detalle de facturas
    // ======================================================

    private JPanel createDetallePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[grow, fill]", "[]10[grow, fill]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblTitle = new JLabel("Facturas del Día");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");
        panel.add(lblTitle, "wrap");

        tblModel = new javax.swing.table.DefaultTableModel(
            new String[]{"#", "Cliente", "Total ($)", "Método(s)", "Estatus"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(tblModel);
        table.setRowHeight(28);
        table.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines:true; intercellSpacing:0,1");

        // Alinear montos a la derecha
        javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, "grow");

        return panel;
    }

    // ======================================================
    // PANEL: Resúmenes laterales
    // ======================================================

    private JPanel createResumenPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0, wrap 1, gap 10", "[grow, fill]", "[][grow, fill][]"));
        panel.setOpaque(false);

        // Resumen por Cuenta Receptora
        panelResumenCuenta = new JPanel(new MigLayout("fillx, insets 15, wrap 1", "[grow, fill]", "[]10[]"));
        panelResumenCuenta.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblCuentaTitle = new JLabel("Desglose por Cuenta");
        lblCuentaTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panelResumenCuenta.add(lblCuentaTitle);
        panel.add(panelResumenCuenta);

        // Resumen por Método de Pago
        panelResumenMetodo = new JPanel(new MigLayout("fillx, insets 15, wrap 1", "[grow, fill]", "[]10[]"));
        panelResumenMetodo.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblMetodoTitle = new JLabel("Desglose por Método de Pago");
        lblMetodoTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panelResumenMetodo.add(lblMetodoTitle);
        panel.add(panelResumenMetodo);

        // Extras: CxC, IVA, Otros
        JPanel panelExtras = new JPanel(new MigLayout("fillx, insets 15, wrap 1", "[grow, fill]", "[]"));
        panelExtras.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblExtrasTitle = new JLabel("Otros Indicadores");
        lblExtrasTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panelExtras.add(lblExtrasTitle);

        lblCxcUsd = new JLabel("Cuentas por Cobrar: $ 0.00");
        lblCxcUsd.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        panelExtras.add(lblCxcUsd);

        lblOtrosUsd = new JLabel("Otros: $ 0.00");
        panelExtras.add(lblOtrosUsd);

        lblTotalIva = new JLabel("IVA Recaudado: $ 0.00");
        lblTotalIva.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        panelExtras.add(lblTotalIva);
        panel.add(panelExtras);

        return panel;
    }

    // ======================================================
    // CARGA DE DATOS
    // ======================================================

    private void cargarReporte() {
        Date selectedDate = (Date) dateSpinner.getValue();
        LocalDate fecha = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        logger.info("Cargando reporte diario para: {}", fecha);

        // Limpiar
        tblModel.setRowCount(0);
        setLabelsLoading();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            DailyStats stats;
            Map<String, Double> porCuenta;
            Map<String, Double> porMetodo;
            List<Map<String, Object>> detalle;

            @Override
            protected Void doInBackground() throws Exception {
                stats = reporteService.calcularEstadisticasDia(fecha);
                porCuenta = reporteService.getIngresoPorCuentaReceptora(fecha, fecha);
                porMetodo = reporteService.getIngresoPorMetodoPago(fecha, fecha);
                detalle = reporteService.getDetalleVentasRango(fecha, fecha);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions

                    // KPIs
                    lblTasaUsada.setText("Tasa: " + DF.format(stats.tasaUsada()) + " Bs/$");
                    lblEfectivoUsd.setText("$ " + DF.format(stats.efectivoUsd()));
                    lblPtoVentaBs.setText("Bs " + DF.format(stats.totalBsCapelli()) +
                        "  ➤  ($ " + DF.format(stats.getCapelliConvertidoUsd()) + ")");
                    lblPagosRosa.setText("Bs " + DF.format(stats.totalBsRosa()) +
                        "  ➤  ($ " + DF.format(stats.getRosaConvertidoUsd()) + ")");
                    lblZelleUsd.setText("$ " + DF.format(stats.zelleUsd()));

                    lblCxcUsd.setText("Cuentas por Cobrar: $ " + DF.format(stats.cuentasPorCobrar()));
                    boolean isDark = FlatLaf.isLafDark();
                    if (stats.cuentasPorCobrar() > 0) {
                        lblCxcUsd.setForeground(isDark ? new Color(255, 82, 82) : new Color(211, 47, 47));
                    } else {
                        lblCxcUsd.setForeground(UIManager.getColor("Label.foreground"));
                    }

                    lblOtrosUsd.setText("Otros: $ " + DF.format(stats.otrosUsd()));

                    double totalIvaBs = stats.totalIva() * stats.tasaUsada();
                    lblTotalIva.setText("IVA: Bs " + DF.format(totalIvaBs) + " ($ " + DF.format(stats.totalIva()) + ")");

                    lblTotalGeneral.setText("$ " + DF.format(stats.getTotalDiaUsd()));

                    // Tabla detalle
                    for (Map<String, Object> row : detalle) {
                        tblModel.addRow(new Object[]{
                            row.get("correlativo"),
                            row.get("cliente"),
                            "$ " + DF.format((Double) row.get("total")),
                            row.get("metodos_pago"),
                            row.get("estatus")
                        });
                    }

                    // Resumen por Cuenta
                    llenarPanelResumen(panelResumenCuenta, "Desglose por Cuenta", porCuenta);
                    // Resumen por Método
                    llenarPanelResumen(panelResumenMetodo, "Desglose por Método de Pago", porMetodo);

                    revalidate();
                    repaint();

                } catch (Exception e) {
                    logger.error("Error al cargar reporte diario", e);
                    ToastNotification.showError(ReporteDiarioView.this, "Error",
                        "No se pudo cargar el reporte: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void llenarPanelResumen(JPanel panel, String titulo, Map<String, Double> datos) {
        panel.removeAll();

        JLabel lblTitle = new JLabel(titulo);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panel.add(lblTitle);

        if (datos.isEmpty()) {
            JLabel lbl = new JLabel("Sin datos");
            lbl.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
            panel.add(lbl);
            return;
        }

        for (Map.Entry<String, Double> entry : datos.entrySet()) {
            JPanel row = new JPanel(new MigLayout("insets 3 0 3 0, fillx", "[grow][]", "[]"));
            row.setOpaque(false);

            JLabel lblName = new JLabel(entry.getKey());
            row.add(lblName);

            JLabel lblValue = new JLabel("$ " + DF.format(entry.getValue()));
            lblValue.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
            row.add(lblValue);

            panel.add(row);
        }
    }

    private void setLabelsLoading() {
        String loading = "...";
        lblTasaUsada.setText("Consultando...");
        lblEfectivoUsd.setText(loading);
        lblPtoVentaBs.setText(loading);
        lblPagosRosa.setText(loading);
        lblZelleUsd.setText(loading);
        lblCxcUsd.setText(loading);
        lblOtrosUsd.setText(loading);
        lblTotalIva.setText(loading);
        lblTotalGeneral.setText("$ --");
    }
}
