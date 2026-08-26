package app.view;

import app.service.BCVService;
import app.service.DashboardService;
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
        pnlKpis.add(createCard("Tasa Euro Oficial (EUR/Bs)", lblBcvRate, "$Component.accentColor"));

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

    private record DashboardData(
            double tasaBcv,
            double ingresosHoy,
            Map<String, Integer> topServicios,
            Map<String, Double> produccion
    ) {}

    private void loadData() {
        logger.info("Iniciando carga asíncrona de datos del dashboard...");
        
        lblBcvRate.setText("Cargando...");
        lblTotalIngreso.setText("Calculando...");

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() {
                double tasa = BCVService.getCachedRate();
                try {
                    double tasaLive = dashboardService.getTasaBCV();
                    if (tasaLive > 0) tasa = tasaLive;
                } catch (Exception ignored) {}

                double ingresos = 0.0;
                Map<String, Integer> topSrv = Map.of();
                Map<String, Double> prod = Map.of();

                try {
                    ingresos = dashboardService.getIngresoTotal(hoy, hoy);
                    topSrv = dashboardService.getTopServicios(inicioMes, hoy);
                    prod = dashboardService.getProduccionPorTrabajadora(hoy, hoy);
                } catch (Exception e) {
                    logger.error("Error al calcular métricas del dashboard en background", e);
                }

                return new DashboardData(tasa, ingresos, topSrv, prod);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();

                    // 1. Tasa BCV
                    lblBcvRate.setText(String.format("Bs. %.2f", data.tasaBcv()));

                    // 2. Ingresos Hoy
                    lblTotalIngreso.setText(String.format("$ %.2f", data.ingresosHoy()));

                    // 3. Top Servicios
                    topServiciosPanel.removeAll();
                    JLabel lblTopServicios = new JLabel("Top 5 Servicios (Este Mes)");
                    lblTopServicios.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
                    topServiciosPanel.add(lblTopServicios, "span, wrap");

                    for (Map.Entry<String, Integer> entry : data.topServicios().entrySet()) {
                        topServiciosPanel.add(new JLabel(entry.getKey()));
                        JLabel lblCant = new JLabel(entry.getValue() + " unds");
                        lblCant.putClientProperty(FlatClientProperties.STYLE, "font:bold");
                        topServiciosPanel.add(lblCant, "wrap");
                    }
                    if (data.topServicios().isEmpty()) {
                        topServiciosPanel.add(new JLabel("Sin datos registrados en el mes."), "span");
                    }

                    // 4. Producción Hoy
                    produccionPanel.removeAll();
                    JLabel lblProduccion = new JLabel("Producción por Trabajadora (Hoy)");
                    lblProduccion.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
                    produccionPanel.add(lblProduccion, "span, wrap");

                    for (Map.Entry<String, Double> entry : data.produccion().entrySet()) {
                        produccionPanel.add(new JLabel(entry.getKey()));
                        JLabel lblProd = new JLabel(String.format("$ %.2f", entry.getValue()));
                        lblProd.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
                        produccionPanel.add(lblProd, "wrap");
                    }
                    if (data.produccion().isEmpty()) {
                        produccionPanel.add(new JLabel("Sin producción registrada hoy."), "span");
                    }

                    topServiciosPanel.revalidate();
                    topServiciosPanel.repaint();
                    produccionPanel.revalidate();
                    produccionPanel.repaint();

                } catch (Exception ex) {
                    logger.error("Error al renderizar datos del dashboard", ex);
                }
            }
        };

        worker.execute();
    }
}
