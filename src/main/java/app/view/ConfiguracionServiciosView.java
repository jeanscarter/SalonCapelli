package app.view;

import app.model.AIKey;
import app.model.AIStatsDTO;
import app.model.AIUsageLog;
import app.model.Usuario;
import app.service.AuthService;
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
 * Vista de Configuración Avanzada y Gestión del Pool de Conexiones.
 * 
 * Acceso restringido exclusivamente para el usuario 'jeanscarter'.
 */
public class ConfiguracionServiciosView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionServiciosView.class);
    private static final String ALLOWED_USER = "jeanscarter";

    private final GeminiTicketScannerService scannerService;

    // Componentes principales
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
    private JPanel pnlGeminiSettings;
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

    public ConfiguracionServiciosView() {
        this.scannerService = new GeminiTicketScannerService();
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 25, gap 15", "[grow, fill]", "[][grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        Usuario currentUser = AuthService.getCurrentUser();
        boolean isAuthorized = currentUser != null && ALLOWED_USER.equalsIgnoreCase(currentUser.getUsername());

        if (!isAuthorized) {
            add(createAccessDeniedPanel(), "grow, center");
            return;
        }

        // Header
        JPanel pnlHeader = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[]"));
        pnlHeader.setOpaque(false);

        JPanel pnlTitles = new JPanel(new MigLayout("insets 0, wrap 1", "[grow, fill]", "[]2[]"));
        pnlTitles.setOpaque(false);

        JLabel lblTitle = new JLabel("⚙️ Configuración del Motor de Procesamiento y Claves");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; foreground:$Component.accentColor");
        pnlTitles.add(lblTitle);

        JLabel lblSub = new JLabel("Gestión multicuenta con cifrado AES-256 GCM, selección de motor y monitoreo de latencia.");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        pnlTitles.add(lblSub);

        pnlHeader.add(pnlTitles);

        lblCapacitySummary = new JLabel("Cargando capacidad...");
        lblCapacitySummary.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:$Success.color");
        pnlHeader.add(lblCapacitySummary);

        add(pnlHeader, "growx, wrap");

        // TabbedPane con estilo moderno
        tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabHeight:38; tabInsets:0,20,0,20; font:bold");

        tabbedPane.addTab("🔑 Pool de Claves (Cifrado AES-256)", createKeysTab());
        tabbedPane.addTab("🧠 Motor y Modelos", createModelsTab());
        tabbedPane.addTab("📊 Tiempos de Uso y Monitoreo", createStatsTab());

        add(tabbedPane, "grow");

        // Cargar datos
        loadKeysTable();
        loadModelsData();
        loadStatsData();
    }

    private JPanel createAccessDeniedPanel() {
        JPanel pnl = new JPanel(new MigLayout("wrap 1, insets 40, align center center", "[center]", "[]15[]10[]"));
        pnl.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblIcon = new JLabel("🔒");
        lblIcon.putClientProperty(FlatClientProperties.STYLE, "font:bold +24");
        pnl.add(lblIcon);

        JLabel lblMsg = new JLabel("Acceso Restringido");
        lblMsg.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; foreground:$Danger.color");
        pnl.add(lblMsg);

        JLabel lblDetail = new JLabel("<html><center>Esta sección contiene configuraciones de infraestructura criptográfica y está<br/>reservada exclusivamente para el usuario <b>" + ALLOWED_USER + "</b>.</center></html>");
        lblDetail.putClientProperty(FlatClientProperties.STYLE, "font:+1; foreground:$Label.disabledForeground");
        pnl.add(lblDetail);

        return pnl;
    }

    // =========================================================================
    // PESTAÑA 1: POOL DE CLAVES
    // =========================================================================
    private JPanel createKeysTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow, fill]", "[][grow, fill][]10[]"));
        panel.setOpaque(false);

        JLabel lblDesc = new JLabel("<html><b>Tolerancia a Fallos y Rotación Automática:</b> Si una cuenta llega al límite diario gratuito (1,500 tickets) o de tasa por minuto, el sistema salta automáticamente a la siguiente cuenta activa sin interrumpir la operación. Todas las claves se resguardan cifradas con <b>AES-256 GCM</b>.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        panel.add(lblDesc, "wrap, gapbottom 10");

        String[] columns = {"ID", "Cuenta / Etiqueta", "Clave (Cifrada en BD)", "Uso Hoy", "Total Histórico", "Estado", "Activa"};
        tableModelKeys = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblKeys = new JTable(tableModelKeys);
        tblKeys.setRowHeight(30);
        tblKeys.getTableHeader().setReorderingAllowed(false);
        tblKeys.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Ocultar ID
        tblKeys.getColumnModel().getColumn(0).setMinWidth(0);
        tblKeys.getColumnModel().getColumn(0).setMaxWidth(0);
        tblKeys.getColumnModel().getColumn(0).setWidth(0);

        tblKeys.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = String.valueOf(value);
                if (val.contains("OK") || val.contains("Conectada")) {
                    setForeground(new Color(40, 167, 69));
                } else if (val.contains("429") || val.contains("Límite")) {
                    setForeground(new Color(255, 152, 0));
                } else if (val.contains("Error") || val.contains("Inválida")) {
                    setForeground(new Color(220, 53, 69));
                } else {
                    setForeground(UIManager.getColor("Label.disabledForeground"));
                }
                return c;
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblKeys);
        scrollTable.putClientProperty(FlatClientProperties.STYLE, "arc:12");
        panel.add(scrollTable, "grow, wrap, gapbottom 10");

        // Botones de acción
        JPanel pnlActions = new JPanel(new MigLayout("insets 0", "[]10[]10[]10[]push[]", "[]"));
        pnlActions.setOpaque(false);

        btnAddKey = new JButton("➕ Agregar Cuenta / Clave");
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
        txtKey.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Pega la clave AIzaSy... aquí");

        JPanel pnl = new JPanel(new MigLayout("wrap 1, fillx, insets 10", "[grow, fill]", "[]5[]10[]5[]"));
        pnl.add(new JLabel("Nombre / Identificador de la Cuenta:"));
        pnl.add(txtLabel);
        pnl.add(new JLabel("API Key de Conexión:"));
        pnl.add(txtKey);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Agregar Cuenta al Pool", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String label = txtLabel.getText().trim();
            String rawKey = new String(txtKey.getPassword()).trim();

            if (rawKey.isEmpty()) {
                ToastNotification.showWarning(this, "Campo Vacío", "Debes ingresar una clave válida.");
                return;
            }

            try {
                AIKey newKey = new AIKey(label.isEmpty() ? "Cuenta Adicional" : label, GeminiTicketScannerService.sanitizeApiKey(rawKey));
                scannerService.getKeyRepository().save(newKey);
                ToastNotification.showSuccess(this, "Clave Guardada Cifrada", "La nueva clave ha sido asegurada con AES-256 en la base de datos.");
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
            ToastNotification.showWarning(this, "Selección requerida", "Por favor selecciona una cuenta de la tabla.");
            return;
        }

        int keyId = (int) tableModelKeys.getValueAt(selectedRow, 0);
        lblKeyStatusMsg.setText("⏳ Probando conexión...");
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
        lblKeyStatusMsg.setText("⏳ Probando todas las cuentas configuradas...");
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
                lblKeyStatusMsg.setText("✅ Diagnóstico completado.");
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
                "¿Estás seguro de eliminar la cuenta '" + label + "' del pool?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                scannerService.getKeyRepository().delete(keyId);
                ToastNotification.showSuccess(this, "Cuenta Eliminada", "La clave fue removida.");
                loadKeysTable();
            } catch (Exception ex) {
                ToastNotification.showError(this, "Error", "No se pudo eliminar: " + ex.getMessage());
            }
        }
    }

    // =========================================================================
    // PESTAÑA 2: MOTOR Y MODELOS
    // =========================================================================
    private JPanel createModelsTab() {
        JPanel panel = new JPanel(new MigLayout("fillx, wrap 1, insets 25", "[grow, fill]", "[]15[]15[]20[grow]"));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel("Selección del Motor de Procesamiento");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +3; foreground:$Component.accentColor");
        panel.add(lblTitle);

        JLabel lblDesc = new JLabel("<html>Elige si el sistema procesará los tickets utilizando <b>Inteligencia Artificial en la Nube (Gemini)</b> o el <b>Motor Local Autónomo (Offline)</b>. Al encender una opción, la otra se desactiva automáticamente.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        panel.add(lblDesc);

        // Card de Selección de Modo (IA vs Local)
        JPanel pnlModeCard = new JPanel(new MigLayout("fillx, insets 15", "[grow, fill][grow, fill]", "[]10[]"));
        pnlModeCard.putClientProperty(FlatClientProperties.STYLE, "arc:14; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        rbModeAI = new JRadioButton("🤖 Modo Inteligencia Artificial (Google Gemini)");
        rbModeAI.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

        rbModeLocal = new JRadioButton("💻 Modo Local Autónomo (Offline / Sin Conexión)");
        rbModeLocal.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");

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
        pnlModeCard.add(rbModeLocal, "wrap");

        JLabel lblDescAI = new JLabel("<html><font color='gray'>Lectura multimodal avanzada, OCR manuscrito complejo y correlación contextual automática.</font></html>");
        JLabel lblDescLocal = new JLabel("<html><font color='gray'>Procesamiento rápido 100% en tu equipo basado en la plantilla litográfica fija. No consume API.</font></html>");
        pnlModeCard.add(lblDescAI);
        pnlModeCard.add(lblDescLocal);

        panel.add(pnlModeCard);

        // Panel de configuración de Gemini (Modelos)
        pnlGeminiSettings = new JPanel(new MigLayout("fillx, wrap 1, insets 15", "[grow, fill]", "[]10[]15[]"));
        pnlGeminiSettings.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:lighten($Panel.background,2%); border:1,1,1,1,$Component.borderColor");

        JLabel lblGeminiTitle = new JLabel("Configuración del Modelo Gemini (Cuando el modo IA está activo):");
        lblGeminiTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        pnlGeminiSettings.add(lblGeminiTitle);

        JPanel pnlSelect = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill][]10[]", "[]"));
        pnlSelect.setOpaque(false);

        cmbModels = new JComboBox<>();
        cmbModels.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        pnlSelect.add(cmbModels, "growx");

        btnRefreshModels = new JButton("🔄 Consultar Modelos Activos");
        btnRefreshModels.addActionListener(e -> refreshModelsFromGoogle());
        pnlSelect.add(btnRefreshModels);

        btnSaveModel = new JButton("Guardar Preferencia");
        btnSaveModel.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold");
        btnSaveModel.addActionListener(e -> saveSelectedModel());
        pnlSelect.add(btnSaveModel);

        pnlGeminiSettings.add(pnlSelect);

        JPanel pnlCards = new JPanel(new MigLayout("fillx, insets 15", "[grow, fill]", "[]8[]8[]"));
        pnlCards.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Panel.background");

        JLabel lblM1 = new JLabel("⭐ Serie Flash (3.6 / 2.5): Óptima para lectura de comandas manuscritas, cursiva y números difíciles.");
        lblM1.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Success.color");
        pnlCards.add(lblM1, "wrap");

        JLabel lblM2 = new JLabel("⚡ Serie Flash-Lite (3.1 / 2.5): Mayor velocidad de respuesta para comprobantes estándar.");
        lblM2.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.foreground");
        pnlCards.add(lblM2, "wrap");

        JLabel lblM3 = new JLabel("🔒 Salto Automático (Failover): Si una clave agota su cupo, el sistema salta a la siguiente sin interrumpir la operación.");
        lblM3.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        pnlCards.add(lblM3);

        pnlGeminiSettings.add(pnlCards);

        panel.add(pnlGeminiSettings);

        lblModelInfo = new JLabel(" ");
        lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        panel.add(lblModelInfo);

        updateEngineUiState();
        return panel;
    }

    private void updateEngineUiState() {
        boolean isAi = rbModeAI != null && rbModeAI.isSelected();
        if (pnlGeminiSettings != null) {
            pnlGeminiSettings.setEnabled(isAi);
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
        lblModelInfo.setText("⏳ Consultando modelos activos...");
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
                    lblModelInfo.setText(String.format("✅ Se detectaron %d modelos compatibles y activos.", models.size()));
                    lblModelInfo.putClientProperty(FlatClientProperties.STYLE, "foreground:$Success.color");
                } catch (Exception ex) {
                    lblModelInfo.setText("❌ Error al listar: " + ex.getMessage());
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
                ToastNotification.showSuccess(this, "Configuración Guardada", "Se usará selección automática óptima.");
            } else {
                scannerService.saveSelectedModel(selected);
                ToastNotification.showSuccess(this, "Modelo Guardado", "Se seleccionó '" + selected + "'.");
            }
        } catch (Exception e) {
            ToastNotification.showError(this, "Error", "No se pudo guardar: " + e.getMessage());
        }
    }

    // =========================================================================
    // PESTAÑA 3: MÉTRICAS Y TIEMPOS DE USO
    // =========================================================================
    private JPanel createStatsTab() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 20", "[grow, fill]", "[][grow, fill][]"));
        panel.setOpaque(false);

        // KPI Cards
        JPanel pnlKpis = new JPanel(new MigLayout("fillx, insets 0, gap 15", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        pnlKpis.setOpaque(false);

        pnlKpis.add(createKpiCard("📅 Peticiones Hoy", lblKpiToday = new JLabel("0 / 1500"), progressDailyQuota = new JProgressBar()));
        pnlKpis.add(createKpiCard("⏱️ Tiempo Promedio", lblKpiAvgTime = new JLabel("0 ms"), null));
        pnlKpis.add(createKpiCard("✅ Tasa de Éxito", lblKpiSuccessRate = new JLabel("100%"), null));
        pnlKpis.add(createKpiCard("🕒 Último Procesamiento", lblKpiLastTime = new JLabel("Ninguno"), null));

        panel.add(pnlKpis, "wrap, gapbottom 15");

        JLabel lblHistTitle = new JLabel("Registro de actividad y tiempos de respuesta recientes:");
        lblHistTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        panel.add(lblHistTitle, "wrap, gapbottom 6");

        String[] cols = {"Fecha/Hora", "Cuenta Usada", "Modelo", "Latencia (ms)", "Resultado", "Detalle / Diagnóstico"};
        tableModelLogs = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblLogs = new JTable(tableModelLogs);
        tblLogs.setRowHeight(26);
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
        scrollLogs.putClientProperty(FlatClientProperties.STYLE, "arc:12");
        panel.add(scrollLogs, "grow, wrap, gapbottom 10");

        JPanel pnlRefresh = new JPanel(new MigLayout("insets 0", "push[]", "[]"));
        pnlRefresh.setOpaque(false);

        btnRefreshStats = new JButton("🔄 Actualizar Métricas");
        btnRefreshStats.addActionListener(e -> loadStatsData());
        pnlRefresh.add(btnRefreshStats);

        panel.add(pnlRefresh);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, JProgressBar bar) {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 15, fillx", "[grow, fill]", "[]6[]6[]"));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:14; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        card.add(lblTitle);

        valueLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:$Component.accentColor");
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

        int today = stats.getRequestsToday();
        int cap = stats.getDailyCapacity();
        lblKpiToday.setText(String.format("%d / %d", today, cap > 0 ? cap : 1500));

        if (progressDailyQuota != null) {
            progressDailyQuota.setMaximum(cap > 0 ? cap : 1500);
            progressDailyQuota.setValue(today);
            int percent = cap > 0 ? (int) (((double) today / cap) * 100) : 0;
            progressDailyQuota.setString(percent + "% utilizado");
        }

        double avg = stats.getAvgDurationMs();
        if (avg >= 1000) {
            lblKpiAvgTime.setText(String.format("%.2f s", avg / 1000.0));
        } else {
            lblKpiAvgTime.setText(String.format("%.0f ms", avg));
        }

        lblKpiSuccessRate.setText(String.format("%.1f%%", stats.getSuccessRate()));
        if (stats.getSuccessRate() >= 90) {
            lblKpiSuccessRate.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:$Success.color");
        } else {
            lblKpiSuccessRate.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:$Warning.color");
        }

        lblKpiLastTime.setText(stats.getLastUsedTimestamp());

        tableModelLogs.setRowCount(0);
        if (stats.getRecentLogs() != null) {
            for (AIUsageLog log : stats.getRecentLogs()) {
                String timeStr = log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ") : "N/A";
                tableModelLogs.addRow(new Object[]{
                        timeStr,
                        log.getKeyLabel() != null ? log.getKeyLabel() : "Clave Principal",
                        log.getModelUsed(),
                        log.getDurationMs() + " ms",
                        log.getStatus(),
                        log.getErrorMessage() != null ? log.getErrorMessage() : "OK"
                });
            }
        }
    }
}
