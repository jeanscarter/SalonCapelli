package app.view.modals;

import app.component.Modal;
import app.model.ScannedTicketDTO;
import app.model.Trabajadora;
import app.repository.*;
import app.service.BCVService;
import app.service.FuzzyMatcher;
import app.service.GeminiTicketScannerService;
import app.service.LocalTicketProcessorService;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal interactivo avanzado para escaneo, lectura y carga estructurada de tickets.
 * 
 * Recrea la experiencia y opciones de la Ventana de Ventas:
 * - Selección configurable de motor (IA Gemini vs Local Offline).
 * - Nº de Factura/Ticket, Fecha, Tasa (con consulta automática al Euro Oficial histórico de dolarapi.com).
 * - Asociación estricta de colaboradoras con la Base de Datos.
 * - Formas de pago, referencia y cálculo en tiempo real en USD y Bs.
 */
public class TicketScannerModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(TicketScannerModal.class);

    private final GeminiTicketScannerService scannerService;
    private final LocalTicketProcessorService localProcessor;
    private final TicketCorrectionRepository correctionRepo;
    private final TrabajadoraRepository trabajadoraRepo;
    private final TicketScannerCallback callback;

    private List<Trabajadora> listaTrabajadorasDB = new ArrayList<>();

    private byte[] currentImageBytes;
    private String currentMimeType = "image/jpeg";

    // Componentes UI - Izquierda
    private JPanel pnlDropZone;
    private JLabel lblImagePreview;
    private JLabel lblDropText;
    private JLabel lblEngineBadge;
    private JButton btnSelectFile;
    private JButton btnAnalyze;
    private JButton btnApplyToSale;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblConfidenceIndicator;

    // Componentes UI - Derecha (Cabecera de Factura/Ticket)
    private JTextField txtResFactura;
    private JTextField txtResFecha;
    private JTextField txtResTasa;
    private JButton btnConsultarTasa;
    private JLabel lblTasaOrigen;

    // Componentes UI - Datos del Cliente y Estilista
    private JTextField txtResCliente;
    private JTextField txtResCedula;
    private JComboBox<Trabajadora> cbResTrabajadora;

    // Componentes UI - Forma de Pago
    private JComboBox<String> cmbMetodoPago;
    private JTextField txtReferenciaPago;

    // Componentes UI - Tabla de Servicios y Totales
    private JTable tblItems;
    private DefaultTableModel tableModel;
    private JButton btnAddRow;
    private JButton btnDeleteRow;
    private JLabel lblResTotalUSD;
    private JLabel lblResTotalBS;

    // Valores originales para detectar correcciones
    private ScannedTicketDTO currentScannedDTO;
    private String originalCliente;
    private String originalTrabajadora;
    private String[] originalMontos;

    public interface TicketScannerCallback {
        void onTicketScanned(ScannedTicketDTO dto);
    }

    public TicketScannerModal(TicketScannerCallback callback) {
        this.callback = callback;
        this.scannerService = new GeminiTicketScannerService();
        this.localProcessor = new LocalTicketProcessorService();
        this.correctionRepo = new TicketCorrectionRepository();
        this.trabajadoraRepo = new TrabajadoraRepositorySQLite();
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1020, 710));
        putClientProperty(FlatClientProperties.STYLE, "arc:16");

        loadCatalogData();

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 18", "[370::370, fill][grow, fill]", "[][grow, fill][]"));
        mainPanel.setOpaque(false);

        // ======= HEADER =======
        JPanel pnlHeader = new JPanel(new MigLayout("insets 0, fillx", "[]15[]push[]", "[]"));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("📸 Lector y Carga de Tickets");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; foreground:$Component.accentColor");
        pnlHeader.add(lblTitle);

        lblEngineBadge = new JLabel("Cargando motor...");
        lblEngineBadge.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        pnlHeader.add(lblEngineBadge);

        lblConfidenceIndicator = new JLabel("");
        lblConfidenceIndicator.putClientProperty(FlatClientProperties.STYLE, "font:bold -1");
        pnlHeader.add(lblConfidenceIndicator);

        mainPanel.add(pnlHeader, "span 2, growx, wrap, gapbottom 6");

        // ======= PANEL IZQUIERDO: Carga de Imagen =======
        JPanel pnlLeft = new JPanel(new MigLayout("wrap 1, insets 15, fill", "[grow, fill]", "[]8[grow, fill]8[]6[]"));
        pnlLeft.putClientProperty(FlatClientProperties.STYLE, "arc:14; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblLeftTitle = new JLabel("Foto del Ticket / Comanda");
        lblLeftTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
        pnlLeft.add(lblLeftTitle);

        pnlDropZone = new JPanel(new BorderLayout());
        pnlDropZone.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Table.background");
        pnlDropZone.setBorder(new LineBorder(new Color(150, 150, 150, 80), 2, true));
        pnlDropZone.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblDropText = new JLabel("<html><center><b>Haz clic, arrastra una foto<br/>o presiona Ctrl + V para pegar</b><br/><br/><font color='gray'>Formatos: JPG, PNG, WEBP</font></center></html>", SwingConstants.CENTER);
        lblImagePreview = new JLabel("", SwingConstants.CENTER);
        lblImagePreview.setVisible(false);

        pnlDropZone.add(lblDropText, BorderLayout.CENTER);
        pnlDropZone.add(lblImagePreview, BorderLayout.CENTER);

        pnlDropZone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { chooseFile(); }
        });

        setupDragAndDrop();
        pnlLeft.add(pnlDropZone, "grow");

        JPanel pnlFileActions = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[]"));
        pnlFileActions.setOpaque(false);

        btnSelectFile = new JButton("Examinar Archivo...");
        btnSelectFile.addActionListener(e -> chooseFile());
        pnlFileActions.add(btnSelectFile, "growx");

        JButton btnPaste = new JButton("📋 Pegar (Ctrl+V)");
        btnPaste.addActionListener(e -> pasteFromClipboard());
        pnlFileActions.add(btnPaste);

        pnlLeft.add(pnlFileActions);

        btnAnalyze = new JButton("⚡ Procesar Ticket");
        btnAnalyze.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold +2; margin:8,10,8,10; arc:10");
        btnAnalyze.setEnabled(false);
        btnAnalyze.addActionListener(e -> analyzeTicket());
        pnlLeft.add(btnAnalyze, "gapy 4");

        mainPanel.add(pnlLeft, "grow");

        // ======= PANEL DERECHO: Datos estilo Ventana de Ventas =======
        JPanel pnlResults = new JPanel(new MigLayout("wrap 1, insets 15, fill", "[grow, fill]", "[]6[]4[]4[grow, fill]4[]4[]"));
        pnlResults.putClientProperty(FlatClientProperties.STYLE, "arc:14; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        JLabel lblResultsTitle = new JLabel("✏️ Datos de la Venta (Revisa y Edita antes de Aplicar)");
        lblResultsTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        pnlResults.add(lblResultsTitle);

        // --- FILA 1: Factura, Fecha, Tasa y botón dolarapi ---
        JPanel pnlFacturaRow = new JPanel(new MigLayout("insets 0, fillx", "[60::60][grow, fill]10[45::45][grow, fill]10[40::40][grow, fill][]", "[]"));
        pnlFacturaRow.setOpaque(false);

        pnlFacturaRow.add(new JLabel("Factura #:"));
        txtResFactura = new JTextField();
        txtResFactura.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 010398");
        pnlFacturaRow.add(txtResFactura);

        pnlFacturaRow.add(new JLabel("Fecha:"));
        txtResFecha = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        txtResFecha.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "DD/MM/YYYY");
        pnlFacturaRow.add(txtResFecha);

        pnlFacturaRow.add(new JLabel("Tasa:"));
        txtResTasa = new JTextField(String.format("%.2f", BCVService.getCachedRate()));
        txtResTasa.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtResTasa.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { recalculateTotals(); }
        });
        pnlFacturaRow.add(txtResTasa);

        btnConsultarTasa = new JButton("🔄 Euro/BCV");
        btnConsultarTasa.setToolTipText("Consultar tasa oficial de la fecha especificada en dolarapi.com");
        btnConsultarTasa.addActionListener(e -> consultarTasaEuroPorFecha());
        pnlFacturaRow.add(btnConsultarTasa);

        pnlResults.add(pnlFacturaRow);

        // Badge de origen de tasa
        lblTasaOrigen = new JLabel("Tasa Euro Oficial / Referencial");
        lblTasaOrigen.putClientProperty(FlatClientProperties.STYLE, "font:-2; foreground:$Label.disabledForeground");
        pnlResults.add(lblTasaOrigen, "gapbottom 4");

        // --- FILA 2: Cliente y Estilista General de BD ---
        JPanel pnlClientRow = new JPanel(new MigLayout("insets 0, fillx", "[60::60][grow, fill]10[45::45][grow, fill]10[60::60][grow, fill]", "[]5[]"));
        pnlClientRow.setOpaque(false);

        pnlClientRow.add(new JLabel("Cliente:"));
        txtResCliente = new JTextField();
        txtResCliente.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del cliente");
        pnlClientRow.add(txtResCliente);

        pnlClientRow.add(new JLabel("Cédula:"));
        txtResCedula = new JTextField();
        txtResCedula.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "V-12345678");
        pnlClientRow.add(txtResCedula);

        pnlClientRow.add(new JLabel("Estilista:"));
        cbResTrabajadora = new JComboBox<>();
        pnlClientRow.add(cbResTrabajadora, "wrap");

        // --- FILA 3: Forma de Pago y Referencia ---
        pnlClientRow.add(new JLabel("Pago:"));
        cmbMetodoPago = new JComboBox<>(new String[]{"Sin especificar", "PAGO_MOVIL", "EFECTIVO", "TRANSFERENCIA", "TARJETA_DEBITO", "TARJETA_CREDITO", "ZELLE", "DIVISAS"});
        pnlClientRow.add(cmbMetodoPago);

        pnlClientRow.add(new JLabel("Ref:"));
        txtReferenciaPago = new JTextField();
        txtReferenciaPago.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Referencia de pago");
        pnlClientRow.add(txtReferenciaPago, "span 3, growx");

        pnlResults.add(pnlClientRow);

        // --- TABLA DE SERVICIOS EDITABLE ---
        String[] colHeaders = {"Fila", "Servicio (Catálogo)", "Colaboradora (BD)", "Monto ($)", "Monto (Bs)", "Confianza"};
        tableModel = new DefaultTableModel(colHeaders, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 1 && column <= 3; // Servicio, Colaboradora y Monto ($) editables
            }
        };

        tblItems = new JTable(tableModel);
        tblItems.setRowHeight(28);
        tblItems.getTableHeader().setReorderingAllowed(false);

        tblItems.getColumnModel().getColumn(0).setMinWidth(40);
        tblItems.getColumnModel().getColumn(0).setMaxWidth(45);

        // Editor de Colaboradora con JComboBox cargado de la Base de Datos
        JComboBox<String> cbColabEditor = new JComboBox<>();
        for (Trabajadora t : listaTrabajadorasDB) {
            cbColabEditor.addItem(t.getNombreCompleto());
        }
        tblItems.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(cbColabEditor));

        // Renderer de Confianza con Colores
        tblItems.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String val = String.valueOf(value);
                try {
                    int pct = Integer.parseInt(val.replace("%", "").trim());
                    if (pct >= 85) setForeground(new Color(40, 167, 69));
                    else if (pct >= 50) setForeground(new Color(255, 152, 0));
                    else setForeground(new Color(220, 53, 69));
                } catch (NumberFormatException e) {
                    setForeground(UIManager.getColor("Label.disabledForeground"));
                }
                setHorizontalAlignment(CENTER);
                return c;
            }
        });
        tblItems.getColumnModel().getColumn(5).setMaxWidth(75);
        tblItems.getColumnModel().getColumn(5).setMinWidth(65);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 3) {
                recalculateTotals();
            }
        });

        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        pnlResults.add(scrollItems, "grow");

        // --- BOTONES DE TABLA Y TOTALES ---
        JPanel pnlItemActions = new JPanel(new MigLayout("insets 0, fillx", "[]5[]push[][]", "[]"));
        pnlItemActions.setOpaque(false);

        btnAddRow = new JButton("➕ Agregar Fila");
        btnAddRow.putClientProperty(FlatClientProperties.STYLE, "font:-1");
        btnAddRow.addActionListener(e -> addManualRow());
        pnlItemActions.add(btnAddRow);

        btnDeleteRow = new JButton("🗑️ Eliminar");
        btnDeleteRow.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Danger.color");
        btnDeleteRow.addActionListener(e -> deleteSelectedRow());
        pnlItemActions.add(btnDeleteRow);

        lblResTotalUSD = new JLabel("Total $: $ 0.00");
        lblResTotalUSD.putClientProperty(FlatClientProperties.STYLE, "font:bold +4; foreground:$Success.color");
        pnlItemActions.add(lblResTotalUSD, "gapright 15");

        lblResTotalBS = new JLabel("Total Bs: 0.00 Bs");
        lblResTotalBS.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        pnlItemActions.add(lblResTotalBS);

        pnlResults.add(pnlItemActions);

        // Status
        lblStatus = new JLabel("Listo para procesar imagen");
        lblStatus.putClientProperty(FlatClientProperties.STYLE, "font:-1; foreground:$Label.disabledForeground");
        pnlResults.add(lblStatus);

        mainPanel.add(pnlResults, "grow, wrap");

        // ======= BARRA INFERIOR =======
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        mainPanel.add(progressBar, "span 2, growx, wrap");

        JPanel pnlBottom = new JPanel(new MigLayout("insets 0", "[]push[]10[]", "[]"));
        pnlBottom.setOpaque(false);

        int corrections = correctionRepo.countAll();
        JLabel lblLearning = new JLabel(corrections > 0 ?
                String.format("🧠 %d correcciones aprendidas en BD", corrections) : "🧠 Motor listo");
        lblLearning.putClientProperty(FlatClientProperties.STYLE, "font:-2; foreground:$Label.disabledForeground");
        pnlBottom.add(lblLearning);

        JButton btnCancel = new JButton("Cerrar");
        btnCancel.addActionListener(e -> getController().closeModal());
        pnlBottom.add(btnCancel);

        btnApplyToSale = new JButton("✅ Confirmar y Aplicar a la Venta");
        btnApplyToSale.putClientProperty(FlatClientProperties.STYLE, "background:$Success.color; foreground:#fff; font:bold +1; margin:6,12,6,12; arc:8");
        btnApplyToSale.setEnabled(false);
        btnApplyToSale.addActionListener(e -> applyToSaleWithLearning());
        pnlBottom.add(btnApplyToSale);

        mainPanel.add(pnlBottom, "span 2, growx");

        add(mainPanel, BorderLayout.CENTER);
        setupGlobalPasteShortcut(mainPanel);
        updateEngineBadge();
    }

    private void loadCatalogData() {
        try {
            listaTrabajadorasDB = trabajadoraRepo.findAll();
        } catch (Exception e) {
            logger.error("Error al cargar datos del catálogo de BD", e);
        }
    }

    private void updateEngineBadge() {
        String engine = scannerService.getEngineMode();
        if (GeminiTicketScannerService.ENGINE_MODE_LOCAL_OCR.equalsIgnoreCase(engine)) {
            lblEngineBadge.setText("💻 Modo Activo: Motor Local Autónomo (Offline)");
            lblEngineBadge.putClientProperty(FlatClientProperties.STYLE, "font:bold -1; foreground:$Component.accentColor");
        } else {
            lblEngineBadge.setText("🤖 Modo Activo: Inteligencia Artificial (Gemini Vision)");
            lblEngineBadge.putClientProperty(FlatClientProperties.STYLE, "font:bold -1; foreground:$Success.color");
        }

        // Poblar combo de colaboradoras
        if (cbResTrabajadora != null) {
            cbResTrabajadora.removeAllItems();
            for (Trabajadora t : listaTrabajadorasDB) {
                cbResTrabajadora.addItem(t);
            }
            cbResTrabajadora.setRenderer(new ListCellRenderer<>() {
                private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

                @Override
                public Component getListCellRendererComponent(JList<? extends Trabajadora> list, Trabajadora value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null) {
                        label.setText(value.getNombreCompleto());
                    }
                    return label;
                }
            });
        }
    }

    // =========================================================================
    // PROCESAMIENTO SEGÚN CONFIGURACIÓN (IA vs LOCAL)
    // =========================================================================

    private void analyzeTicket() {
        if (currentImageBytes == null || currentImageBytes.length == 0) {
            ToastNotification.showWarning(this, "Sin Imagen", "Por favor carga una foto del ticket primero.");
            return;
        }

        btnAnalyze.setEnabled(false);
        progressBar.setVisible(true);
        lblStatus.setText("⏳ Procesando imagen del ticket...");

        String configuredEngine = scannerService.getEngineMode();

        SwingWorker<ScannedTicketDTO, Void> worker = new SwingWorker<>() {
            @Override
            protected ScannedTicketDTO doInBackground() throws Exception {
                if (GeminiTicketScannerService.ENGINE_MODE_LOCAL_OCR.equalsIgnoreCase(configuredEngine)) {
                    // Modo Local seleccionado en configuración
                    logger.info("Ejecutando procesamiento en Modo Local por configuración...");
                    ScannedTicketDTO localResult = localProcessor.processTicket(currentImageBytes, currentMimeType);
                    localResult.setProcessingMode("LOCAL");
                    return localResult;
                } else {
                    // Modo IA Gemini seleccionado en configuración
                    if (scannerService.isApiKeyConfigured()) {
                        try {
                            logger.info("Ejecutando procesamiento en Modo IA (Gemini) por configuración...");
                            ScannedTicketDTO remoteResult = scannerService.scanTicketImage(currentImageBytes, currentMimeType);
                            remoteResult.setProcessingMode("REMOTE");
                            if (remoteResult.getConfidenceScore() <= 0) {
                                remoteResult.setConfidenceScore(0.92);
                            }
                            return remoteResult;
                        } catch (Exception e) {
                            logger.warn("Motor IA falló ({}), recurriendo a fallback local...", e.getMessage());
                        }
                    }
                    // Fallback a motor local
                    ScannedTicketDTO fallback = localProcessor.processTicket(currentImageBytes, currentMimeType);
                    fallback.setProcessingMode("LOCAL");
                    return fallback;
                }
            }

            @Override
            protected void done() {
                btnAnalyze.setEnabled(true);
                progressBar.setVisible(false);
                try {
                    currentScannedDTO = get();
                    displayResults(currentScannedDTO);
                    btnApplyToSale.setEnabled(true);
                    String mode = "LOCAL".equals(currentScannedDTO.getProcessingMode()) ? "Motor Local" : "Inteligencia Artificial";
                    lblStatus.setText(String.format("✅ Procesado con éxito vía %s. Revisa y edita los datos antes de aplicar.", mode));
                    ToastNotification.showSuccess(TicketScannerModal.this, "Ticket Procesado", "Datos listos para revisar y facturar.");
                } catch (Exception ex) {
                    logger.error("Error al analizar ticket", ex);
                    lblStatus.setText("❌ Error al procesar ticket.");
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    ToastNotification.showError(TicketScannerModal.this, "Error de Procesamiento", msg);
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    // CONSULTA AUTOMÁTICA DE TASA EURO OFICIAL POR FECHA
    // =========================================================================

    private void consultarTasaEuroPorFecha() {
        String fecha = txtResFecha.getText().trim();
        lblStatus.setText("⏳ Consultando tasa oficial para la fecha: " + fecha + "...");

        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() {
                return BCVService.getEuroOficialRate(fecha);
            }

            @Override
            protected void done() {
                try {
                    double rate = get();
                    if (rate > 0) {
                        txtResTasa.setText(String.format("%.2f", rate));
                        lblTasaOrigen.setText("🌐 Euro Oficial por Fecha (ve.dolarapi.com): " + String.format("%.2f", rate) + " Bs");
                        lblTasaOrigen.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Success.color");
                        recalculateTotals();
                        ToastNotification.showSuccess(TicketScannerModal.this, "Tasa Actualizada", "Tasa Euro obtenida: " + String.format("%.2f", rate) + " Bs");
                    }
                } catch (Exception e) {
                    ToastNotification.showError(TicketScannerModal.this, "Error de Consulta", "No se pudo obtener tasa: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    // DISPLAY Y EDICIÓN DE RESULTADOS
    // =========================================================================

    private void displayResults(ScannedTicketDTO dto) {
        if (dto == null) return;

        originalCliente = dto.getClienteNombre();
        originalTrabajadora = dto.getTrabajadoraNombre();

        // 1. Factura, Fecha y Tasa
        if (dto.getNumeroFactura() != null && !dto.getNumeroFactura().isBlank()) {
            txtResFactura.setText(dto.getNumeroFactura());
        }
        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            txtResFecha.setText(dto.getFecha());
        }

        if (dto.getTasa() > 0) {
            txtResTasa.setText(String.format("%.2f", dto.getTasa()));
            String origen = dto.getTasaOrigen();
            if ("DETECTADA_EN_TICKET".equals(origen)) {
                lblTasaOrigen.setText("🔍 Tasa extraída directamente del ticket: " + String.format("%.2f", dto.getTasa()) + " Bs");
                lblTasaOrigen.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Component.accentColor");
            } else if ("EURO_HISTORICO_DOLARAPI".equals(origen)) {
                lblTasaOrigen.setText("🌐 Euro Oficial Histórico por Fecha (ve.dolarapi.com): " + String.format("%.2f", dto.getTasa()) + " Bs");
                lblTasaOrigen.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Success.color");
            } else {
                lblTasaOrigen.setText("🏦 Tasa BCV / Referencial: " + String.format("%.2f", dto.getTasa()) + " Bs");
                lblTasaOrigen.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Label.disabledForeground");
            }
        }

        // 2. Cliente y Cédula
        txtResCliente.setText(dto.getClienteNombre() != null ? dto.getClienteNombre() : "");
        txtResCedula.setText(dto.getClienteCedula() != null ? dto.getClienteCedula() : "");

        // 3. Estilista General (Fuzzy Matching con BD)
        matchAndSelectTrabajadora(dto.getTrabajadoraNombre());

        // 4. Método de Pago y Referencia
        if (dto.getMetodoPago() != null) {
            String mp = dto.getMetodoPago().toUpperCase();
            for (int i = 0; i < cmbMetodoPago.getItemCount(); i++) {
                String item = cmbMetodoPago.getItemAt(i).toUpperCase();
                if (mp.contains(item) || item.contains(mp)) {
                    cmbMetodoPago.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            cmbMetodoPago.setSelectedIndex(0);
        }

        txtReferenciaPago.setText(dto.getReferenciaPago() != null ? dto.getReferenciaPago() : "");

        // 5. Tabla de Servicios / Ítems
        tableModel.setRowCount(0);
        originalMontos = new String[dto.getItems().size()];

        for (int i = 0; i < dto.getItems().size(); i++) {
            ScannedTicketDTO.ScannedItemDTO item = dto.getItems().get(i);
            String montoStr = String.format("%.2f", item.getPrecio());
            originalMontos[i] = montoStr;

            // Resolver trabajadora de la fila con la BD
            String colabFila = item.getTrabajadoraNombre();
            if (colabFila == null || colabFila.isBlank()) {
                Trabajadora gen = (Trabajadora) cbResTrabajadora.getSelectedItem();
                colabFila = gen != null ? gen.getNombreCompleto() : "";
            } else {
                Trabajadora matched = findBestMatchingTrabajadora(colabFila);
                if (matched != null) {
                    colabFila = matched.getNombreCompleto();
                }
            }

            int confPct = (int) (item.getConfidence() * 100);
            if (confPct <= 0) confPct = 85;

            tableModel.addRow(new Object[]{
                    item.getRowNumber() > 0 ? String.valueOf(item.getRowNumber()) : String.valueOf(i + 1),
                    item.getDescripcion(),
                    colabFila,
                    montoStr,
                    "0.00 Bs",
                    confPct + "%"
            });
        }

        recalculateTotals();

        // 6. Indicador de confianza global
        int globalConf = (int) (dto.getConfidenceScore() * 100);
        if (globalConf <= 0) globalConf = 85;

        String confText;
        String confColor;
        if (globalConf >= 80) {
            confText = "🟢 Confianza Alta (" + globalConf + "%)";
            confColor = "foreground:$Success.color";
        } else if (globalConf >= 50) {
            confText = "🟡 Confianza Media (" + globalConf + "%) — Revisa valores";
            confColor = "foreground:#FF9800";
        } else {
            confText = "🔴 Confianza Baja (" + globalConf + "%) — Corrige manualmente";
            confColor = "foreground:$Danger.color";
        }
        lblConfidenceIndicator.setText(confText);
        lblConfidenceIndicator.putClientProperty(FlatClientProperties.STYLE, "font:bold -1; " + confColor);
    }

    private void matchAndSelectTrabajadora(String rawName) {
        if (rawName == null || rawName.isBlank() || listaTrabajadorasDB.isEmpty()) {
            return;
        }

        Trabajadora best = findBestMatchingTrabajadora(rawName);
        if (best != null) {
            cbResTrabajadora.setSelectedItem(best);
        }
    }

    private Trabajadora findBestMatchingTrabajadora(String rawName) {
        if (rawName == null || rawName.isBlank()) return null;

        List<String> names = new ArrayList<>();
        for (Trabajadora t : listaTrabajadorasDB) {
            names.add(t.getNombreCompleto());
            if (t.getNombres() != null) names.add(t.getNombres());
        }

        FuzzyMatcher.MatchResult res = FuzzyMatcher.findBestMatch(rawName, names, 0.40);
        if (res != null) {
            for (Trabajadora t : listaTrabajadorasDB) {
                if (t.getNombreCompleto().equalsIgnoreCase(res.matchedValue) ||
                    (t.getNombres() != null && t.getNombres().equalsIgnoreCase(res.matchedValue))) {
                    return t;
                }
            }
        }
        return null;
    }

    private void recalculateTotals() {
        double totalUSD = 0.0;
        double tasa = 1.0;

        try {
            tasa = Double.parseDouble(txtResTasa.getText().trim().replace(",", "."));
            if (tasa <= 0) tasa = 1.0;
        } catch (Exception ignored) {}

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                String valStr = String.valueOf(tableModel.getValueAt(i, 3)).replace(",", ".").trim();
                double montoItem = Double.parseDouble(valStr);
                totalUSD += montoItem;

                double montoBs = montoItem * tasa;
                tableModel.setValueAt(String.format("%.2f Bs", montoBs), i, 4);
            } catch (Exception ignored) {}
        }

        lblResTotalUSD.setText(String.format("Total $: $ %.2f", totalUSD));
        lblResTotalBS.setText(String.format("Total Bs: %.2f Bs", totalUSD * tasa));
    }

    private void addManualRow() {
        Trabajadora t = (Trabajadora) cbResTrabajadora.getSelectedItem();
        String colabName = t != null ? t.getNombreCompleto() : "";
        int nextNum = tableModel.getRowCount() + 1;
        tableModel.addRow(new Object[]{String.valueOf(nextNum), "Servicio", colabName, "0.00", "0.00 Bs", "100%"});
        recalculateTotals();
    }

    private void deleteSelectedRow() {
        int selected = tblItems.getSelectedRow();
        if (selected >= 0) {
            tableModel.removeRow(selected);
            recalculateTotals();
        }
    }

    // =========================================================================
    // CONFIRMAR Y APLICAR A LA VENTA CON APRENDIZAJE
    // =========================================================================

    private void applyToSaleWithLearning() {
        if (callback == null) return;

        ScannedTicketDTO finalDTO = new ScannedTicketDTO();

        finalDTO.setNumeroFactura(txtResFactura.getText().trim().isEmpty() ? null : txtResFactura.getText().trim());
        finalDTO.setFecha(txtResFecha.getText().trim().isEmpty() ? null : txtResFecha.getText().trim());

        try {
            finalDTO.setTasa(Double.parseDouble(txtResTasa.getText().trim().replace(",", ".")));
        } catch (Exception e) {
            finalDTO.setTasa(BCVService.getCachedRate());
        }
        finalDTO.setTasaOrigen(lblTasaOrigen.getText());

        finalDTO.setClienteNombre(txtResCliente.getText().trim().isEmpty() ? null : txtResCliente.getText().trim());
        finalDTO.setClienteCedula(txtResCedula.getText().trim().isEmpty() ? null : txtResCedula.getText().trim());

        Trabajadora selectedTrab = (Trabajadora) cbResTrabajadora.getSelectedItem();
        finalDTO.setTrabajadoraNombre(selectedTrab != null ? selectedTrab.getNombreCompleto() : null);

        String selectedPago = (String) cmbMetodoPago.getSelectedItem();
        finalDTO.setMetodoPago("Sin especificar".equals(selectedPago) ? null : selectedPago);
        finalDTO.setReferenciaPago(txtReferenciaPago.getText().trim().isEmpty() ? null : txtReferenciaPago.getText().trim());

        double total = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            ScannedTicketDTO.ScannedItemDTO item = new ScannedTicketDTO.ScannedItemDTO();
            String desc = String.valueOf(tableModel.getValueAt(i, 1));
            item.setDescripcion(desc);

            String colab = String.valueOf(tableModel.getValueAt(i, 2));
            item.setTrabajadoraNombre(colab.isBlank() ? null : colab.trim());

            try {
                String montoStr = String.valueOf(tableModel.getValueAt(i, 3)).replace(",", ".").trim();
                item.setPrecio(Double.parseDouble(montoStr));
            } catch (Exception e) {
                item.setPrecio(0.0);
            }

            try {
                String rowStr = String.valueOf(tableModel.getValueAt(i, 0));
                item.setRowNumber(Integer.parseInt(rowStr));
            } catch (Exception ignored) {}

            if (desc.toLowerCase().contains("propina") || (item.getRowNumber() == 22)) {
                item.setEsPropina(true);
            }

            total += item.getPrecio();
            finalDTO.addItem(item);
        }
        finalDTO.setTotalDetectado(total);

        // Guardar correcciones aprendidas
        int correctionsCount = 0;
        String newTrab = selectedTrab != null ? selectedTrab.getNombreCompleto() : "";
        if (originalTrabajadora != null && !originalTrabajadora.isBlank() && !newTrab.isBlank() && !originalTrabajadora.equalsIgnoreCase(newTrab)) {
            correctionRepo.saveCorrection("COLABORADOR", originalTrabajadora, newTrab, 0);
            correctionsCount++;
        }

        String newCli = txtResCliente.getText().trim();
        if (originalCliente != null && !originalCliente.isBlank() && !newCli.isBlank() && !originalCliente.equalsIgnoreCase(newCli)) {
            correctionRepo.saveCorrection("CLIENTE", originalCliente, newCli, 0);
            correctionsCount++;
        }

        if (originalMontos != null) {
            for (int i = 0; i < Math.min(originalMontos.length, tableModel.getRowCount()); i++) {
                String newMonto = String.valueOf(tableModel.getValueAt(i, 3));
                if (!originalMontos[i].equals(newMonto)) {
                    String rowNum = String.valueOf(tableModel.getValueAt(i, 0));
                    correctionRepo.saveCorrection("MONTO", originalMontos[i], newMonto, rowNum.matches("\\d+") ? Integer.parseInt(rowNum) : 0);
                    correctionsCount++;
                }
            }
        }

        if (correctionsCount > 0) {
            logger.info("Se registraron {} correcciones de aprendizaje en SQLite.", correctionsCount);
        }

        callback.onTicketScanned(finalDTO);
        getController().closeModal();
    }

    // =========================================================================
    // UTILIDADES DE IMAGEN Y PORTAPAPELES
    // =========================================================================

    private void setupGlobalPasteShortcut(JComponent component) {
        component.setFocusable(true);
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    pasteFromClipboard();
                }
            }
        });
    }

    private void setupDragAndDrop() {
        new DropTarget(pnlDropZone, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files != null && !files.isEmpty()) {
                            loadImageFromFile(files.get(0));
                        }
                    } else if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image img = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                        if (img instanceof BufferedImage bi) {
                            loadImageFromBufferedImage(bi);
                        } else if (img != null) {
                            BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = bi.createGraphics();
                            g.drawImage(img, 0, 0, null);
                            g.dispose();
                            loadImageFromBufferedImage(bi);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error al recibir archivo drag-and-drop", ex);
                }
            }
        });
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar Foto del Ticket");
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (*.jpg, *.jpeg, *.png, *.webp)", "jpg", "jpeg", "png", "webp"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            loadImageFromFile(chooser.getSelectedFile());
        }
    }

    private void pasteFromClipboard() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null) {
                if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    Image img = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                    if (img != null) {
                        BufferedImage bi;
                        if (img instanceof BufferedImage b) {
                            bi = b;
                        } else {
                            bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = bi.createGraphics();
                            g.drawImage(img, 0, 0, null);
                            g.dispose();
                        }
                        loadImageFromBufferedImage(bi);
                        ToastNotification.showSuccess(this, "Imagen Pegada", "Foto cargada desde el portapapeles.");
                        return;
                    }
                } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        loadImageFromFile(files.get(0));
                        ToastNotification.showSuccess(this, "Archivo Cargado", "Foto cargada exitosamente.");
                        return;
                    }
                }
            }
            ToastNotification.showWarning(this, "Portapapeles", "No se encontró ninguna imagen en el portapapeles.");
        } catch (Exception ex) {
            logger.error("Error al pegar del portapapeles", ex);
            ToastNotification.showError(this, "Error", "No se pudo pegar la imagen: " + ex.getMessage());
        }
    }

    private void loadImageFromFile(File file) {
        try {
            BufferedImage bi = ImageIO.read(file);
            if (bi == null) {
                ToastNotification.showError(this, "Formato Inválido", "El archivo no es una imagen reconocible.");
                return;
            }
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png")) currentMimeType = "image/png";
            else if (name.endsWith(".webp")) currentMimeType = "image/webp";
            else currentMimeType = "image/jpeg";
            loadImageFromBufferedImage(bi);
        } catch (Exception e) {
            logger.error("Error al leer archivo de imagen", e);
            ToastNotification.showError(this, "Error", "Error al leer imagen: " + e.getMessage());
        }
    }

    private void loadImageFromBufferedImage(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formatName = currentMimeType.contains("png") ? "png" : "jpeg";
            ImageIO.write(bi, formatName, baos);
            this.currentImageBytes = baos.toByteArray();

            int targetWidth = 320;
            int targetHeight = 320;
            double ratio = (double) bi.getWidth() / bi.getHeight();
            if (ratio > 1) targetHeight = (int) (targetWidth / ratio);
            else targetWidth = (int) (targetHeight * ratio);

            Image scaled = bi.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            lblImagePreview.setIcon(new ImageIcon(scaled));
            lblImagePreview.setVisible(true);
            lblDropText.setVisible(false);

            btnAnalyze.setEnabled(true);
            lblStatus.setText("Foto lista (" + (currentImageBytes.length / 1024) + " KB). Haz clic en Procesar Ticket.");
        } catch (Exception ex) {
            logger.error("Error al procesar imagen en memoria", ex);
        }
    }
}
