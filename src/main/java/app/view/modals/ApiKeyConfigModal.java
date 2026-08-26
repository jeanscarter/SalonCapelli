package app.view.modals;

import app.component.Modal;
import app.model.AIKey;
import app.model.AIStatsDTO;
import app.model.AIUsageLog;
import app.service.GeminiTicketScannerService;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modal avanzado para la configuración del Pool de API Keys (Failover),
 * selección dinámica de modelos y visualización de tiempos de uso y métricas de IA.
 */
public class ApiKeyConfigModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyConfigModal.class);

    private final GeminiTicketScannerService scannerService;
    private final ApiKeyCallback callback;

    // Componentes generales
    private JTabbedPane tabbedPane;
    private JLabel lblCapacitySummary;

    // Tab 1: Claves
    private JTable tblKeys;
    private DefaultTableModel tableModelKeys;
    private JButton btnAddKey;
    private JButton btnTestSelected;
    private JButton btnTestAll;
    private JButton btnToggleActive;
    private JButton btnDeleteKey;
    private JLabel lblKeyStatusMsg;

    // Tab 2: Modo de Motor y Modelos
    private JRadioButton rbModeAI;
    private JRadioButton rbModeLocal;
    private JPanel pnlGeminiConfig;
    private JComboBox<String> cmbModels;
    private JButton btnRefreshModels;
    private JButton btnSaveModel;
    private JLabel lblModelInfo;

    // Tab 3: Métricas
    private JLabel lblKpiToday;
    private JProgressBar progressDailyQuota;
    private JLabel lblKpiAvgTime;
    private JLabel lblKpiSuccessRate;
    private JLabel lblKpiLastTime;
    private JTable tblLogs;
    private DefaultTableModel tableModelLogs;
    private JButton btnRefreshStats;

    public interface ApiKeyCallback {
        void onSaved();
    }

    public ApiKeyConfigModal(ApiKeyCallback callback) {
        this.callback = callback;
        this.scannerService = new GeminiTicketScannerService();
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(760, 580));
        putClientProperty(FlatClientProperties.STYLE, "arc:16");

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20", "[grow, fill]", "[][grow, fill][]"));
        mainPanel.setOpaque(false);

        // Header Principal
        JPanel pnlHeader = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[]"));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("🤖 Centro de Control de Inteligencia Artificial y Motor");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:$Component.accentColor");
        pnlHeader.add(lblTitle);

        lblCapacitySummary = new JLabel("Cargando capacidad...");
        lblCapacitySummary.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
        pnlHeader.add(lblCapacitySummary);

        mainPanel.add(pnlHeader, "wrap, gapbottom 10");

        // TabbedPane con estilo moderno
        tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabHeight:36; tabInsets:0,16,0,16; font:bold");

        tabbedPane.addTab("🔑 Pool de API Keys", createKeysTab());
        tabbedPane.addTab("🧠 Motor y Modelos", createModelsTab());
        tabbedPane.addTab("📊 Tiempos de Uso y Métricas", createStatsTab());

        mainPanel.add(tabbedPane, "grow, wrap, gapbottom 12");

        // Botón Cerrar
        JPanel pnlBottom = new JPanel(new MigLayout("insets 0", "push[]", "[]"));
        pnlBottom.setOpaque(false);

        JButton btnClose = new JButton("Cerrar");
        btnClose.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold");
        btnClose.addActionListener(e -> {
            if (callback != null) {
                callback.onSaved();
            }
            getController().closeModal();
        });
        pnlBottom.add(btnClose);

        mainPanel.add(pnlBottom);

        add(mainPanel, BorderLayout.CENTER);

        // Cargar datos iniciales
        loadKeysTable();
        loadModelsData();
        loadStatsData();
    }

    // =========================================================================
    // PESTAÑA 1: POOL DE API KEYS (FAILOVER MULTICUENTA)
    // =========================================================================
    private JPanel createKeysTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[grow, fill]", "[][grow, fill][]10[]"));
        panel.setOpaque(false);

        // Descripción explicativa
        JLabel lblDesc = new JLabel("<html><b>Gestión Multicuenta con Salto Automático (Failover):</b> Puedes agregar claves de distintas cuentas de Google. Si una clave llega a su límite diario (1,500 tickets) o de tasa por minuto, el sistema salta automáticamente a la siguiente sin interrupciones.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        panel.add(lblDesc, "wrap, gapbottom 8");

        // Tabla de Claves
        String[] columns = {"ID", "Cuenta / Etiqueta", "API Key (Enmascarada)", "Uso Hoy", "Total", "Estado", "Activa"};
        tableModelKeys = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblKeys = new JTable(tableModelKeys);
        tblKeys.setRowHeight(28);
        tblKeys.getTableHeader().setReorderingAllowed(false);
        tblKeys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Ocultar columna ID
        tblKeys.getColumnModel().getColumn(0).setMinWidth(0);
        tblKeys.getColumnModel().getColumn(0).setMaxWidth(0);
        tblKeys.getColumnModel().getColumn(0).setWidth(0);

        // Renderers
        tblKeys.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = String.valueOf(value);
                if (val.contains("OK") || val.contains("EXITO")) {
                    setForeground(new Color(40, 167, 69));
                } else if (val.contains("429") || val.contains("LIMITE")) {
                    setForeground(new Color(255, 152, 0));
                } else if (val.contains("ERROR") || val.contains("INVALID")) {
                    setForeground(new Color(220, 53, 69));
                } else {
                    setForeground(UIManager.getColor("Label.disabledForeground"));
                }
                return c;
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblKeys);
        scrollTable.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        panel.add(scrollTable, "grow, wrap, gapbottom 8");

        // Barra de Botones de Acción
        JPanel pnlActions = new JPanel(new MigLayout("insets 0", "[]8[]8[]8[]push[]", "[]"));
        pnlActions.setOpaque(false);

        btnAddKey = new JButton("➕ Agregar API Key");
        btnAddKey.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        btnAddKey.addActionListener(e -> showAddKeyDialog());
        pnlActions.add(btnAddKey);

        btnTestSelected = new JButton("🧪 Probar Seleccionada");
        btnTestSelected.addActionListener(e -> testSelectedKey());
        pnlActions.add(btnTestSelected);

        btnTestAll = new JButton("🔄 Probar Todas");
        btnTestAll.addActionListener(e -> testAllKeys());
        pnlActions.add(btnTestAll);

        btnToggleActive = new JButton("⚡ Activar/Desactivar");
        btnToggleActive.addActionListener(e -> toggleSelectedKey());
        pnlActions.add(btnToggleActive);

        btnDeleteKey = new JButton("🗑️ Eliminar");
        btnDeleteKey.putClientProperty(FlatClientProperties.STYLE, "foreground:$Danger.color");
        btnDeleteKey.addActionListener(e -> deleteSelectedKey());
        pnlActions.add(btnDeleteKey);

        panel.add(pnlActions, "wrap");

        lblKeyStatusMsg = new JLabel(" ");
        lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        panel.add(lblKeyStatusMsg);

        return panel;
    }

    private void loadKeysTable() {
        tableModelKeys.setRowCount(0);
        try {
            List<AIKey> keys = scannerService.getKeyRepository().findAll();
            int activeCount = 0;
            int totalToday = 0;

            for (AIKey k : keys) {
                if (k.isActive()) activeCount++;
                totalToday += k.getRequestsToday();

                String statusBadge = switch (k.getLastStatus()) {
                    case "OK" -> "🟢 Conectada";
                    case "RATE_LIMIT_429" -> "🟡 Límite 429";
                    case "ERROR" -> "🔴 Error";
                    default -> "⚪ " + k.getLastStatus();
                };

                tableModelKeys.addRow(new Object[]{
                        k.getId(),
                        k.getLabel(),
                        k.getMaskedKey(),
                        k.getRequestsToday() + " / 1,500",
                        k.getTotalRequests(),
                        statusBadge,
                        k.isActive() ? "✅ Sí" : "❌ No"
                });
            }

            int capacity = activeCount * 1500;
            lblCapacitySummary.setText(String.format("⚡ Capacidad Diaria: %d / %d tickets (%d cuentas activas)", totalToday, capacity, activeCount));
        } catch (Exception e) {
            logger.error("Error al cargar tabla de claves", e);
        }
    }

    private void showAddKeyDialog() {
        JTextField txtLabel = new JTextField("Cuenta " + (tableModelKeys.getRowCount() + 1));
        JPasswordField txtKey = new JPasswordField();
        txtKey.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Pega tu clave AIzaSy... aquí");

        JPanel pnl = new JPanel(new MigLayout("wrap 1, fillx, insets 10", "[grow, fill]", "[]5[]10[]5[]"));
        pnl.add(new JLabel("Nombre o Identificador de la Cuenta:"));
        pnl.add(txtLabel);
        pnl.add(new JLabel("API Key de Google Gemini:"));
        pnl.add(txtKey);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Agregar Nueva API Key", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String label = txtLabel.getText().trim();
            String rawKey = new String(txtKey.getPassword()).trim();

            if (rawKey.isEmpty()) {
                ToastNotification.showWarning(this, "Campo Vacío", "Debes ingresar una API Key válida.");
                return;
            }

            try {
                AIKey newKey = new AIKey(label.isEmpty() ? "Cuenta Adicional" : label, GeminiTicketScannerService.sanitizeApiKey(rawKey));
                scannerService.getKeyRepository().save(newKey);
                ToastNotification.showSuccess(this, "Clave Agregada", "La nueva API Key ha sido guardada en el pool.");
                loadKeysTable();
                loadModelsData();
            } catch (Exception ex) {
                logger.error("Error al guardar nueva clave", ex);
                ToastNotification.showError(this, "Error", "No se pudo guardar la clave: " + ex.getMessage());
            }
        }
    }

    private void testSelectedKey() {
        int selectedRow = tblKeys.getSelectedRow();
        if (selectedRow < 0) {
            ToastNotification.showWarning(this, "Selección requerida", "Por favor selecciona una clave de la tabla.");
            return;
        }

        int keyId = (int) tableModelKeys.getValueAt(selectedRow, 0);
        lblKeyStatusMsg.setText("⏳ Probando clave seleccionada...");
        lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        SwingWorker<GeminiTicketScannerService.ConnectionTestResult, Void> worker = new SwingWorker<>() {
            @Override
            protected GeminiTicketScannerService.ConnectionTestResult doInBackground() {
                try {
                    List<AIKey> all = scannerService.getKeyRepository().findAll();
                    for (AIKey k : all) {
                        if (k.getId() == keyId) {
                            GeminiTicketScannerService.ConnectionTestResult res = scannerService.testConnectionDetailed(k.getApiKey());
                            scannerService.getKeyRepository().updateKeyStatus(keyId, res.success ? "OK" : "ERROR", res.message);
                            return res;
                        }
                    }
                } catch (Exception e) {
                    return new GeminiTicketScannerService.ConnectionTestResult(false, e.getMessage(), null);
                }
                return new GeminiTicketScannerService.ConnectionTestResult(false, "Clave no encontrada", null);
            }

            @Override
            protected void done() {
                try {
                    GeminiTicketScannerService.ConnectionTestResult res = get();
                    if (res.success) {
                        lblKeyStatusMsg.setText("✅ " + res.message);
                        lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "foreground:$Success.color");
                    } else {
                        lblKeyStatusMsg.setText("❌ Error: " + res.message);
                        lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "foreground:$Danger.color");
                    }
                } catch (Exception ex) {
                    lblKeyStatusMsg.setText("❌ Error al probar: " + ex.getMessage());
                }
                loadKeysTable();
            }
        };
        worker.execute();
    }

    private void testAllKeys() {
        lblKeyStatusMsg.setText("⏳ Probando todas las claves configuradas...");
        lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    List<AIKey> all = scannerService.getKeyRepository().findAll();
                    for (AIKey k : all) {
                        GeminiTicketScannerService.ConnectionTestResult res = scannerService.testConnectionDetailed(k.getApiKey());
                        scannerService.getKeyRepository().updateKeyStatus(k.getId(), res.success ? "OK" : "ERROR", res.message);
                    }
                } catch (Exception e) {
                    logger.error("Error al probar todas las claves", e);
                }
                return null;
            }

            @Override
            protected void done() {
                lblKeyStatusMsg.setText("✅ Prueba masiva completada.");
                lblKeyStatusMsg.putClientProperty(FlatClientProperties.STYLE, "foreground:$Success.color");
                loadKeysTable();
            }
        };
        worker.execute();
    }

    private void toggleSelectedKey() {
        int selectedRow = tblKeys.getSelectedRow();
        if (selectedRow < 0) return;

        int keyId = (int) tableModelKeys.getValueAt(selectedRow, 0);
        try {
            List<AIKey> all = scannerService.getKeyRepository().findAll();
            for (AIKey k : all) {
                if (k.getId() == keyId) {
                    scannerService.getKeyRepository().toggleActive(keyId, !k.isActive());
                    break;
                }
            }
            loadKeysTable();
        } catch (Exception ex) {
            ToastNotification.showError(this, "Error", "No se pudo cambiar estado: " + ex.getMessage());
        }
    }

    private void deleteSelectedKey() {
        int selectedRow = tblKeys.getSelectedRow();
        if (selectedRow < 0) return;

        int keyId = (int) tableModelKeys.getValueAt(selectedRow, 0);
        String label = String.valueOf(tableModelKeys.getValueAt(selectedRow, 1));

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de eliminar la clave '" + label + "' del pool?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                scannerService.getKeyRepository().delete(keyId);
                ToastNotification.showSuccess(this, "Clave Eliminada", "La clave fue removida del sistema.");
                loadKeysTable();
            } catch (Exception ex) {
                ToastNotification.showError(this, "Error", "No se pudo eliminar: " + ex.getMessage());
            }
        }
    }

    // =========================================================================
    // PESTAÑA 2: SELECCIÓN DINÁMICA DE MODELOS Y MODO DE MOTOR
    // =========================================================================
    private JPanel createModelsTab() {
        JPanel panel = new JPanel(new MigLayout("fillx, wrap 1, insets 15", "[grow, fill]", "[]10[]15[]15[]15[grow]"));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel("Configuración del Motor de Visión y OCR");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panel.add(lblTitle);

        JLabel lblDesc = new JLabel("<html>Selecciona si procesarás tickets con <b>Inteligencia Artificial (Gemini)</b> o con el <b>Motor Local (Offline)</b>. Al encender una opción, la otra se apaga automáticamente.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        panel.add(lblDesc);

        // Selector de Modo (IA vs Local)
        JPanel pnlModeCard = new JPanel(new MigLayout("fillx, insets 10", "[grow, fill][grow, fill]", "[]"));
        pnlModeCard.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        rbModeAI = new JRadioButton("🤖 Modo IA (Gemini)");
        rbModeAI.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        rbModeLocal = new JRadioButton("💻 Modo Local (Offline)");
        rbModeLocal.putClientProperty(FlatClientProperties.STYLE, "font:bold");

        ButtonGroup bgMode = new ButtonGroup();
        bgMode.add(rbModeAI);
        bgMode.add(rbModeLocal);

        String currentEngine = scannerService.getEngineMode();
        if (GeminiTicketScannerService.ENGINE_MODE_LOCAL_OCR.equalsIgnoreCase(currentEngine)) {
            rbModeLocal.setSelected(true);
        } else {
            rbModeAI.setSelected(true);
        }

        rbModeAI.addActionListener(e -> {
            try {
                scannerService.saveEngineMode(GeminiTicketScannerService.ENGINE_MODE_GEMINI_AI);
                updateEngineUiState();
                ToastNotification.showSuccess(this, "Modo Actualizado", "Se activó el Motor de Inteligencia Artificial (Gemini).");
            } catch (Exception ex) {
                ToastNotification.showError(this, "Error", ex.getMessage());
            }
        });

        rbModeLocal.addActionListener(e -> {
            try {
                scannerService.saveEngineMode(GeminiTicketScannerService.ENGINE_MODE_LOCAL_OCR);
                updateEngineUiState();
                ToastNotification.showSuccess(this, "Modo Actualizado", "Se activó el Motor Local Autónomo (Offline).");
            } catch (Exception ex) {
                ToastNotification.showError(this, "Error", ex.getMessage());
            }
        });

        pnlModeCard.add(rbModeAI);
        pnlModeCard.add(rbModeLocal);
        panel.add(pnlModeCard);

        // Sub-panel de opciones de Gemini
        pnlGeminiConfig = new JPanel(new MigLayout("fillx, wrap 1, insets 10", "[grow, fill]", "[]8[]"));
        pnlGeminiConfig.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:lighten($Panel.background,3%); border:1,1,1,1,$Component.borderColor");

        JLabel lblSubTitle = new JLabel("Modelo Gemini Activo:");
        lblSubTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        pnlGeminiConfig.add(lblSubTitle);

        // Selector y botones
        JPanel pnlSelect = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill][]8[]", "[]"));
        pnlSelect.setOpaque(false);

        cmbModels = new JComboBox<>();
        cmbModels.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        pnlSelect.add(cmbModels, "growx");

        btnRefreshModels = new JButton("🔄 Consultar Google");
        btnRefreshModels.addActionListener(e -> refreshModelsFromGoogle());
        pnlSelect.add(btnRefreshModels);

        btnSaveModel = new JButton("Guardar Preferencia");
        btnSaveModel.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold");
        btnSaveModel.addActionListener(e -> saveSelectedModel());
        pnlSelect.add(btnSaveModel);

        pnlGeminiConfig.add(pnlSelect);
        panel.add(pnlGeminiConfig);

        // Caja de información sobre modelos
        JPanel pnlCards = new JPanel(new MigLayout("fillx, insets 12", "[grow, fill]", "[]6[]6[]"));
        pnlCards.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblM1 = new JLabel("⭐ gemini-3.6-flash / 2.5-flash: Recomendados para SalonCapelli (Lectura de letra cursiva, zero-shot OCR y rapidez).");
        lblM1.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
        pnlCards.add(lblM1, "wrap");

        JLabel lblM2 = new JLabel("⚡ gemini-2.5-flash-lite / 3.1-flash-lite: Menor tiempo de procesamiento, ideal para tickets impresos limpios.");
        lblM2.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.foreground");
        pnlCards.add(lblM2, "wrap");

        JLabel lblM3 = new JLabel("💡 Nota: En el Free Tier de Google AI Studio, todos los modelos Flash son 100% gratuitos hasta 1,500 peticiones diarias por cuenta.");
        lblM3.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        pnlCards.add(lblM3);

        panel.add(pnlCards);

        lblModelInfo = new JLabel(" ");
        lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        panel.add(lblModelInfo);

        updateEngineUiState();
        return panel;
    }

    private void updateEngineUiState() {
        boolean isAi = rbModeAI != null && rbModeAI.isSelected();
        if (pnlGeminiConfig != null) {
            pnlGeminiConfig.setEnabled(isAi);
            cmbModels.setEnabled(isAi);
            btnRefreshModels.setEnabled(isAi);
            btnSaveModel.setEnabled(isAi);
        }
    }

    private void loadModelsData() {
        if (cmbModels == null) return;
        cmbModels.removeAllItems();
        cmbModels.addItem("Automático (Recomendado)");

        String apiKey = scannerService.getApiKey();
        List<String> models = scannerService.getAvailableModels(apiKey);
        for (String m : models) {
            cmbModels.addItem(m);
        }

        String savedModel = scannerService.getSelectedModel();
        if (savedModel != null && !savedModel.isBlank() && !savedModel.equalsIgnoreCase("gemini-3.6-flash")) {
            cmbModels.setSelectedItem(savedModel);
        } else {
            cmbModels.setSelectedIndex(0);
        }
    }

    private void refreshModelsFromGoogle() {
        btnRefreshModels.setEnabled(false);
        lblModelInfo.setText("⏳ Consultando modelos activos a la API de Google...");
        lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                String apiKey = scannerService.getApiKey();
                return scannerService.getAvailableModels(apiKey);
            }

            @Override
            protected void done() {
                btnRefreshModels.setEnabled(true);
                try {
                    List<String> models = get();
                    cmbModels.removeAllItems();
                    cmbModels.addItem("Automático (Recomendado)");
                    for (String m : models) {
                        cmbModels.addItem(m);
                    }
                    lblModelInfo.setText(String.format("✅ Se encontraron %d modelos compatibles en Google AI Studio.", models.size()));
                    lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "foreground:$Success.color");
                } catch (Exception ex) {
                    lblModelInfo.setText("❌ Error al listar modelos: " + ex.getMessage());
                    lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "foreground:$Danger.color");
                }
            }
        };
        worker.execute();
    }

    private void saveSelectedModel() {
        String selected = (String) cmbModels.getSelectedItem();
        try {
            if (selected == null || selected.startsWith("Automático")) {
                scannerService.saveSelectedModel("");
                ToastNotification.showSuccess(this, "Modelo Actualizado", "Se usará la selección automática óptima (Flash).");
            } else {
                scannerService.saveSelectedModel(selected);
                ToastNotification.showSuccess(this, "Modelo Guardado", "Se seleccionó '" + selected + "' como modelo preferido.");
            }
        } catch (Exception e) {
            ToastNotification.showError(this, "Error", "No se pudo guardar: " + e.getMessage());
        }
    }

    // =========================================================================
    // PESTAÑA 3: TIEMPOS DE USO Y MÉTRICAS EN TIEMPO REAL
    // =========================================================================
    private JPanel createStatsTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[grow, fill]", "[][grow, fill][]"));
        panel.setOpaque(false);

        // Fila Superior: KPI Cards
        JPanel pnlKpis = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        pnlKpis.setOpaque(false);

        pnlKpis.add(createKpiCard("📅 Peticiones Hoy", lblKpiToday = new JLabel("0 / 1500"), progressDailyQuota = new JProgressBar()));
        pnlKpis.add(createKpiCard("⏱️ Tiempo Promedio", lblKpiAvgTime = new JLabel("0 ms"), null));
        pnlKpis.add(createKpiCard("✅ Tasa de Éxito", lblKpiSuccessRate = new JLabel("100%"), null));
        pnlKpis.add(createKpiCard("🕒 Último Escaneo", lblKpiLastTime = new JLabel("Ninguno"), null));

        panel.add(pnlKpis, "wrap, gapbottom 10");

        // Tabla de Historial Reciente
        JLabel lblHistTitle = new JLabel("Historial de los últimos escaneos realizados:");
        lblHistTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        panel.add(lblHistTitle, "wrap, gapbottom 4");

        String[] cols = {"Fecha/Hora", "Cuenta Usada", "Modelo", "Latencia (ms)", "Resultado", "Detalle/Error"};
        tableModelLogs = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblLogs = new JTable(tableModelLogs);
        tblLogs.setRowHeight(24);
        tblLogs.getTableHeader().setReorderingAllowed(false);

        tblLogs.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = String.valueOf(value);
                if ("SUCCESS".equalsIgnoreCase(val)) {
                    setForeground(new Color(40, 167, 69));
                } else if ("RATE_LIMIT_429".equalsIgnoreCase(val)) {
                    setForeground(new Color(255, 152, 0));
                } else {
                    setForeground(new Color(220, 53, 69));
                }
                return c;
            }
        });

        JScrollPane scrollLogs = new JScrollPane(tblLogs);
        scrollLogs.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        panel.add(scrollLogs, "grow, wrap, gapbottom 8");

        // Botón Refrescar Estadísticas
        JPanel pnlRefresh = new JPanel(new MigLayout("insets 0", "push[]", "[]"));
        pnlRefresh.setOpaque(false);

        btnRefreshStats = new JButton("🔄 Actualizar Métricas");
        btnRefreshStats.addActionListener(e -> loadStatsData());
        pnlRefresh.add(btnRefreshStats);

        panel.add(pnlRefresh);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, JProgressBar bar) {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 10, fillx", "[grow, fill]", "[]4[]4[]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:-2; foreground:$Label.disabledForeground");
        card.add(lblTitle);

        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +3; foreground:$Component.accentColor");
        card.add(valueLabel);

        if (bar != null) {
            bar.setStringPainted(true);
            bar.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            card.add(bar);
        }

        return card;
    }

    private void loadStatsData() {
        AIStatsDTO stats = scannerService.getStats();

        // 1. Peticiones hoy vs capacidad diaria
        int today = stats.getRequestsToday();
        int cap = stats.getDailyCapacity();
        lblKpiToday.setText(String.format("%d / %d", today, cap > 0 ? cap : 1500));

        if (progressDailyQuota != null) {
            progressDailyQuota.setMaximum(cap > 0 ? cap : 1500);
            progressDailyQuota.setValue(today);
            int percent = cap > 0 ? (int) (((double) today / cap) * 100) : 0;
            progressDailyQuota.setString(percent + "% utilizado");
        }

        // 2. Tiempo promedio
        double avg = stats.getAvgDurationMs();
        if (avg >= 1000) {
            lblKpiAvgTime.setText(String.format("%.2f s", avg / 1000.0));
        } else {
            lblKpiAvgTime.setText(String.format("%.0f ms", avg));
        }

        // 3. Tasa de éxito
        lblKpiSuccessRate.setText(String.format("%.1f%%", stats.getSuccessRate()));
        if (stats.getSuccessRate() >= 90) {
            lblKpiSuccessRate.putClientProperty(FlatClientProperties.STYLE, "font:bold +3; foreground:$Success.color");
        } else {
            lblKpiSuccessRate.putClientProperty(FlatClientProperties.STYLE, "font:bold +3; foreground:$Warning.color");
        }

        // 4. Último escaneo
        lblKpiLastTime.setText(stats.getLastUsedTimestamp());

        // 5. Historial reciente
        tableModelLogs.setRowCount(0);
        if (stats.getRecentLogs() != null) {
            for (AIUsageLog log : stats.getRecentLogs()) {
                String timeStr = log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ") : "N/A";
                tableModelLogs.addRow(new Object[]{
                        timeStr,
                        log.getKeyLabel() != null ? log.getKeyLabel() : "Clave Default",
                        log.getModelUsed(),
                        log.getDurationMs() + " ms",
                        log.getStatus(),
                        log.getErrorMessage() != null ? log.getErrorMessage() : "OK"
                });
            }
        }
    }
}
