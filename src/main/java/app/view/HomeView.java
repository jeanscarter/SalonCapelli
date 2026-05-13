package app.view;

import app.exception.DatabaseException;
import app.service.DashboardService;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Map;

public class HomeView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(HomeView.class);
    private final DashboardService dashboardService;

    // Etiquetas de la UI
    private JLabel lblBcvRate;
    private JLabel lblTotalIngreso;
    private JPanel topServiciosPanel;
    private JPanel produccionPanel;

    public HomeView() {
        this.dashboardService = new DashboardService();
        init();
        loadData();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow, fill]", "[][][grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // --- Título ---
        JLabel title = new JLabel("Dashboard Principal");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +15");
        add(title, "growx, wrap");

        // --- Tarjetas Superiores (KPIs) ---
        JPanel pnlKpis = new JPanel(new MigLayout("insets 0, gap 20", "[grow, fill][grow, fill]", "[]"));
        pnlKpis.setOpaque(false);

        lblBcvRate = new JLabel("Cargando...");
        lblBcvRate.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Component.accentColor");
        pnlKpis.add(createCard("Tasa BCV Actual (USD/Bs)", lblBcvRate, "$Component.accentColor"));

        lblTotalIngreso = new JLabel("Cargando...");
        lblTotalIngreso.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Success.color");
        pnlKpis.add(createCard("Ingresos Hoy (USD)", lblTotalIngreso, "$Success.color"));

        add(pnlKpis, "growx, wrap, gapy 20");

        // --- Paneles de Listas ---
        JPanel pnlListas = new JPanel(new MigLayout("insets 0, gap 20", "[grow, fill][grow, fill]", "[grow, fill]"));
        pnlListas.setOpaque(false);

        // Top Servicios
        topServiciosPanel = new JPanel(new MigLayout("fillx, insets 15", "[grow][right]", "[]10[]"));
        topServiciosPanel.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");
        JLabel lblTopServicios = new JLabel("Top 5 Servicios (Este Mes)");
        lblTopServicios.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        topServiciosPanel.add(lblTopServicios, "span, wrap");
        pnlListas.add(topServiciosPanel);

        // Producción por Trabajadora
        produccionPanel = new JPanel(new MigLayout("fillx, insets 15", "[grow][right]", "[]10[]"));
        produccionPanel.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");
        JLabel lblProduccion = new JLabel("Producción por Trabajadora (Hoy)");
        lblProduccion.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        produccionPanel.add(lblProduccion, "span, wrap");
        pnlListas.add(produccionPanel);

        add(pnlListas, "grow");
    }

    private JPanel createCard(String title, JLabel valueLabel, String borderColor) {
        JPanel card = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[]10[]"));
        card.putClientProperty(FlatClientProperties.STYLE, 
                "arc:15; background:$Panel.background; border:0,0,3,0," + borderColor);
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        
        card.add(lblTitle, "wrap");
        card.add(valueLabel);
        
        return card;
    }

    private void loadData() {
        logger.info("Cargando datos del dashboard...");
        
        // Cargar Tasa BCV (En un hilo separado para no bloquear la UI si la API es lenta)
        SwingWorker<Double, Void> workerBcv = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                return dashboardService.getTasaBCV();
            }
            @Override
            protected void done() {
                try {
                    lblBcvRate.setText(String.format("Bs. %.2f", get()));
                } catch (Exception e) {
                    lblBcvRate.setText("Error");
                }
            }
        };
        workerBcv.execute();

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        try {
            // Ingresos Hoy
            double ingresosHoy = dashboardService.getIngresoTotal(hoy, hoy);
            lblTotalIngreso.setText(String.format("$ %.2f", ingresosHoy));

            // Top Servicios
            Map<String, Integer> topServicios = dashboardService.getTopServicios(inicioMes, hoy);
            for (Map.Entry<String, Integer> entry : topServicios.entrySet()) {
                topServiciosPanel.add(new JLabel(entry.getKey()));
                JLabel lblCant = new JLabel(entry.getValue() + " unds");
                lblCant.putClientProperty(FlatClientProperties.STYLE, "font:bold");
                topServiciosPanel.add(lblCant, "wrap");
            }
            if (topServicios.isEmpty()) {
                topServiciosPanel.add(new JLabel("Sin datos registrados en el mes."), "span");
            }

            // Producción Hoy
            Map<String, Double> produccion = dashboardService.getProduccionPorTrabajadora(hoy, hoy);
            for (Map.Entry<String, Double> entry : produccion.entrySet()) {
                produccionPanel.add(new JLabel(entry.getKey()));
                JLabel lblProd = new JLabel(String.format("$ %.2f", entry.getValue()));
                lblProd.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
                produccionPanel.add(lblProd, "wrap");
            }
            if (produccion.isEmpty()) {
                produccionPanel.add(new JLabel("Sin producción registrada hoy."), "span");
            }

        } catch (DatabaseException e) {
            logger.error("Error cargando métricas", e);
            ToastNotification.showError(this, "Error de Datos", "No se pudieron cargar algunas estadísticas.");
        }
    }
}
