package app.view;

import app.exception.DatabaseException;
import app.exception.ValidationException;
import app.model.*;
import app.option.ModalOption;
import app.repository.*;
import app.service.BCVService;
import app.service.TicketPDFService;
import app.service.VentaService;
import app.service.AuthService;
import app.system.ModalManager;
import app.util.ToastNotification;
import app.view.modals.ClienteModal;
import app.view.modals.ProductoSelectorModal;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Vista principal de Facturación / Punto de Venta.
 *
 * Fase 1: Búsqueda de clientes por cédula.
 * Fase 2: Correlativo 6 dígitos, modo histórico (Ctrl+F4), IVA 16% con Ctrl+I.
 */
public class VentaView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(VentaView.class);

    // Prefijos de documento soportados
    private static final String[] PREFIJOS_DOCUMENTO = {"V", "J", "G", "P", "E"};

    // Repositorios y Servicios
    private final TrabajadoraRepository trabajadoraRepo;
    private final ServicioRepository servicioRepo;
    private final ClienteRepository clienteRepo;
    private final VentaService ventaService;

    // === FASE 1: Búsqueda de Cliente ===
    private JComboBox<String> cbTipoDocumento;
    private JTextField txtNumeroDocumento;
    private JButton btnBuscarCliente;
    private JTextField txtNombreClienteSeleccionado;
    private Cliente clienteSeleccionado;

    // === Formulario de Servicios ===
    private JComboBox<Trabajadora> cbTrabajadora;
    private JComboBox<Servicio> cbServicio;
    private JComboBox<TipoCabello> cbTipoCabello;
    private JCheckBox chkClienteTraeProducto;
    private JButton btnAddService;

    // === Tabla de Pre-Visualización ===
    private JTable tableItems;
    private DefaultTableModel tableModel;
    private JLabel lblSubtotal;
    private JLabel lblIva;
    private JLabel lblTotal, lblTotalBS;
    private JButton btnProcesarVenta;
    private JLabel lblDescuento;

    // === FASE 4.5: Descuentos ===
    private JComboBox<String> cbTipoDescuento;
    private JTextField txtMontoDescuento;
    private JButton btnAplicarDescuento;

    // === FASE 4: Pagos Múltiples ===
    private JComboBox<String> cbMetodoPago;
    private JComboBox<String> cbMonedaPago;
    private JTextField txtMontoPago;
    private JTextField txtReferenciaPago;
    private JPanel panelPagoMovil;
    private JComboBox<String> cbDestinoPM;
    private JComboBox<String> cbBancoRosa;
    private JTable tblPagos;
    private DefaultTableModel tblPagosModel;
    private JLabel lblSaldoRestante;

    // === FASE 2: Correlativo y Header ===
    private JLabel lblNumeroFactura;
    private JLabel lblTasaBcvActual;

    // === FASE 2: Panel Histórico (Ctrl+F4) ===
    private JPanel panelHistorico;
    private JSpinner spinnerFechaHistorica;
    private JTextField txtCorrelativoHistorico;
    private JTextField txtTasaHistorica;
    private boolean modoHistorico = false;

    // === FASE 2: IVA ===
    private static final double IVA_RATE = 0.16;
    private boolean ivaExento = false;

    // === FASE 3: Propinas ===
    private JComboBox<Trabajadora> cbTrabajadoraPropina;
    private JTextField txtMontoPropina;
    private JTable tblPropinas;
    private DefaultTableModel tblPropinaModel;
    private JLabel lblTotalPropinas;

    // Estado actual
    private final Venta ventaActual;

    public VentaView() {
        this.trabajadoraRepo = new TrabajadoraRepositorySQLite();
        this.servicioRepo = new ServicioRepositorySQLite();
        this.clienteRepo = new ClienteRepositorySQLite();
        this.ventaService = new VentaService();
        this.ventaActual = new Venta();

        init();
        loadCombos();
        cargarCorrelativoActual();
        updateTotals();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 15", "[350::350, fill][grow, fill]", "[]5[]5[grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // =============================================
        // FASE 2: Header con Correlativo y Tasa BCV
        // =============================================
        JPanel pnlHeader = new JPanel(new MigLayout("insets 10 15 10 15, fillx", "[]push[]", "[]"));
        pnlHeader.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background");

        lblNumeroFactura = new JLabel("Factura #------");
        lblNumeroFactura.putClientProperty(FlatClientProperties.STYLE, "font:bold +8; foreground:$Component.accentColor");
        pnlHeader.add(lblNumeroFactura);

        lblTasaBcvActual = new JLabel("Tasa BCV: " + String.format("%.2f", BCVService.getCachedRate()));
        lblTasaBcvActual.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        pnlHeader.add(lblTasaBcvActual);

        add(pnlHeader, "span 2, growx, wrap");

        // =============================================
        // FASE 2: Panel Histórico (Ctrl+F4) — oculto
        // =============================================
        panelHistorico = createHistoricoPanel();
        panelHistorico.setVisible(false);
        add(panelHistorico, "span 2, growx, wrap");

        // --- Panel Izquierdo: Formulario de Ítems ---
        JPanel pnlForm = new JPanel(new MigLayout("wrap 1, insets 20, fillx", "[grow, fill]", "[]10[]"));
        pnlForm.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblFormTitle = new JLabel("Agregar Servicio");
        lblFormTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +5; foreground:$Component.accentColor");
        pnlForm.add(lblFormTitle);

        pnlForm.add(createClienteSearchPanel());
        pnlForm.add(new JSeparator(), "growx, gapy 5");

        pnlForm.add(new JLabel("Trabajadora:"));
        cbTrabajadora = new JComboBox<>();
        pnlForm.add(cbTrabajadora);

        pnlForm.add(new JLabel("Servicio:"));
        cbServicio = new JComboBox<>();
        cbServicio.addActionListener(e -> updateServiceOptions());
        pnlForm.add(cbServicio);

        pnlForm.add(new JLabel("Longitud / Tipo de Cabello:"));
        cbTipoCabello = new JComboBox<>(TipoCabello.values());
        pnlForm.add(cbTipoCabello);

        chkClienteTraeProducto = new JCheckBox("El cliente trae su propio producto");
        chkClienteTraeProducto.setVisible(false);
        pnlForm.add(chkClienteTraeProducto);

        btnAddService = new JButton("Agregar a la Venta", new FlatSVGIcon("icons/add.svg", 16, 16));
        btnAddService.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold");
        btnAddService.addActionListener(e -> tryAddService());
        pnlForm.add(btnAddService, "gapy 20");

        // =============================================
        // FASE 3: Panel de Propinas
        // =============================================
        pnlForm.add(new JSeparator(), "growx, gapy 10");
        pnlForm.add(createPropinasPanel());

        add(pnlForm);

        // --- Panel Derecho: Resumen y Pago ---
        JPanel pnlResumen = new JPanel(new MigLayout("wrap 1, insets 20, fillx", "[grow, fill]", "[][grow, fill][]"));
        pnlResumen.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblResumenTitle = new JLabel("Resumen de Venta");
        lblResumenTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        pnlResumen.add(lblResumenTitle);

        tableModel = new DefaultTableModel(new String[]{"Trabajadora", "Servicio", "Producto", "Precio"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tableItems = new JTable(tableModel);
        tableItems.setRowHeight(30);
        JScrollPane scrollTable = new JScrollPane(tableItems);
        pnlResumen.add(scrollTable, "grow");

        // Totales (Fase 2)
        JPanel pnlTotales = new JPanel(new MigLayout("fillx, insets 10", "[][grow, right]", "[]5[]5[]"));
        pnlTotales.setOpaque(false);
        lblSubtotal = new JLabel("Subtotal: $0.00");
        lblSubtotal.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        pnlTotales.add(lblSubtotal, "span, right, wrap");

        // Fila descuento interactiva
        JPanel pnlDesc = new JPanel(new MigLayout("insets 0", "[][][grow, right]", "[]"));
        pnlDesc.setOpaque(false);
        cbTipoDescuento = new JComboBox<>(new String[]{"Sin descuento", "% Porcentaje", "$ Monto Fijo"});
        cbTipoDescuento.addActionListener(e -> lblDescuento.setText("Descuento: $0.00"));
        pnlDesc.add(cbTipoDescuento);
        txtMontoDescuento = new JTextField(5);
        pnlDesc.add(txtMontoDescuento);
        btnAplicarDescuento = new JButton("Aplicar");
        btnAplicarDescuento.addActionListener(e -> aplicarDescuento());
        pnlDesc.add(btnAplicarDescuento);
        pnlTotales.add(pnlDesc, "span, right, wrap");

        lblDescuento = new JLabel("Descuento: $0.00");
        lblDescuento.putClientProperty(FlatClientProperties.STYLE, "font:+1; foreground:$Warning.color");
        pnlTotales.add(lblDescuento, "span, right, wrap");

        lblIva = new JLabel("IVA (16%): $0.00");
        lblIva.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        pnlTotales.add(lblIva, "span, right, wrap");

        lblTotal = new JLabel("Total USD: $0.00");
        lblTotal.putClientProperty(FlatClientProperties.STYLE, "font:bold +8; foreground:$Success.color");
        pnlTotales.add(lblTotal, "span, right, wrap");

        lblTotalBS = new JLabel("Total Bs: 0.00");
        lblTotalBS.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");
        pnlTotales.add(lblTotalBS, "span, right, wrap");
        pnlResumen.add(pnlTotales);

        // =============================================
        // FASE 4: Panel de Pagos Múltiples
        // =============================================
        pnlResumen.add(new JSeparator(), "growx");
        pnlResumen.add(createPagosPanel());

        // Botón Procesar
        btnProcesarVenta = new JButton("Procesar y Facturar Venta");
        btnProcesarVenta.putClientProperty(FlatClientProperties.STYLE, "background:$Success.color; foreground:#fff; font:bold +2; margin:10,10,10,10");
        btnProcesarVenta.addActionListener(e -> submitVenta());
        pnlResumen.add(btnProcesarVenta, "growx");

        add(pnlResumen);

        // =============================================
        // FASE 2: Keybindings
        // =============================================
        registerKeyBindings();
    }

    // =============================================
    // FASE 1: Panel de Búsqueda de Cliente por Cédula
    // =============================================

    /**
     * Crea el panel de búsqueda de cliente por documento de identidad.
     * Reemplaza el JComboBox que cargaba TODOS los clientes.
     *
     * Layout:
     * ┌─────────────────────────────────────────┐
     * │ Buscar Cliente:                         │
     * │ [V ▼] [__número__________] [🔍 Buscar]  │
     * │ Cliente: Juan Pérez (V-12345678)        │
     * └─────────────────────────────────────────┘
     */
    private JPanel createClienteSearchPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", "[]5[]5[]"));
        panel.setOpaque(false);

        JLabel lblBuscar = new JLabel("Buscar Cliente:");
        lblBuscar.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        panel.add(lblBuscar);

        // Fila de búsqueda: [Prefijo] [Número] [Botón]
        JPanel rowSearch = new JPanel(new MigLayout("insets 0, fillx", "[65!]5[grow, fill]5[]", "[]"));
        rowSearch.setOpaque(false);

        cbTipoDocumento = new JComboBox<>(PREFIJOS_DOCUMENTO);
        cbTipoDocumento.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        rowSearch.add(cbTipoDocumento);

        txtNumeroDocumento = new JTextField();
        txtNumeroDocumento.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "N° de Documento");
        txtNumeroDocumento.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        // Filtro: solo dígitos
        txtNumeroDocumento.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });
        // Enter → buscar
        txtNumeroDocumento.addActionListener(e -> buscarCliente());
        rowSearch.add(txtNumeroDocumento);

        btnBuscarCliente = new JButton("Buscar");
        btnBuscarCliente.setIcon(new FlatSVGIcon("icons/search.svg", 14, 14));
        btnBuscarCliente.putClientProperty(FlatClientProperties.STYLE, "arc:8; background:$Component.accentColor; foreground:#fff");
        btnBuscarCliente.addActionListener(e -> buscarCliente());
        rowSearch.add(btnBuscarCliente);

        panel.add(rowSearch);

        // Campo de confirmación visual (read-only)
        txtNombreClienteSeleccionado = new JTextField();
        txtNombreClienteSeleccionado.setEditable(false);
        txtNombreClienteSeleccionado.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "-- Ningún cliente seleccionado --");
        txtNombreClienteSeleccionado.putClientProperty(FlatClientProperties.STYLE, 
            "arc:8; background:lighten($Panel.background,5%); foreground:$Success.color; font:bold");
        panel.add(txtNombreClienteSeleccionado);

        return panel;
    }

    /**
     * Ejecuta la búsqueda de cliente por documento de identidad.
     *
     * Escenario A (encontrado): Asigna el cliente y muestra su nombre.
     * Escenario B (no encontrado): Toast de aviso + abre ClienteModal con cédula pre-poblada.
     *                              Al guardar en el modal, auto-selecciona el cliente creado.
     */
    private void buscarCliente() {
        String numero = txtNumeroDocumento.getText().trim();

        if (numero.isEmpty()) {
            ToastNotification.showWarning(this, "Ingrese el número de documento.");
            txtNumeroDocumento.requestFocusInWindow();
            return;
        }

        String prefijo = (String) cbTipoDocumento.getSelectedItem();
        String cedulaCompleta = prefijo + "-" + numero;
        logger.info("Buscando cliente con cédula: {}", cedulaCompleta);

        try {
            Cliente encontrado = clienteRepo.findByCedula(cedulaCompleta);

            if (encontrado != null) {
                // === Escenario A: Cliente encontrado ===
                seleccionarCliente(encontrado);
                ToastNotification.showSuccess(this, "Cliente Encontrado",
                    encontrado.getNombreCompleto());
                logger.info("✓ Cliente encontrado: ID={} - {}", encontrado.getId(), encontrado.getNombreCompleto());
            } else {
                // === Escenario B: No encontrado → abrir modal de registro ===
                logger.info("Cliente no encontrado para cédula: {}. Abriendo formulario de registro.", cedulaCompleta);
                
                ToastNotification.showInfo(this, "Cliente No Registrado",
                    "Abriendo formulario de registro con cédula " + cedulaCompleta);

                // Abrir ClienteModal con cédula pre-poblada y callback de auto-selección
                ModalOption opt = ModalOption.getDefault()
                    .setCloseOnClickOutside(false)
                    .setAnimationEnabled(true);

                ClienteModal modal = new ClienteModal(cedulaCompleta, clienteCreado -> {
                    // Callback: auto-seleccionar el cliente recién creado en la factura
                    seleccionarCliente(clienteCreado);
                    logger.info("✓ Cliente registrado y auto-seleccionado: {}", clienteCreado.getNombreCompleto());
                });

                ModalManager.showModal(this, modal, opt, "cliente_registro_rapido");
            }

        } catch (DatabaseException e) {
            logger.error("Error al buscar cliente: {}", e.getMessage(), e);
            ToastNotification.showError(this, "Error de Base de Datos",
                "No se pudo buscar el cliente: " + e.getMessage());
        }
    }

    /**
     * Asigna un cliente como seleccionado para la venta actual.
     * Actualiza el campo visual de confirmación.
     */
    private void seleccionarCliente(Cliente cliente) {
        this.clienteSeleccionado = cliente;
        txtNombreClienteSeleccionado.setText(
            cliente.getNombreCompleto() + "  (" + cliente.getCedula() + ")"
        );
    }

    /**
     * Limpia la selección de cliente actual
     */
    private void limpiarSeleccionCliente() {
        this.clienteSeleccionado = null;
        txtNombreClienteSeleccionado.setText("");
        txtNumeroDocumento.setText("");
        cbTipoDocumento.setSelectedIndex(0);
    }

    // =============================================
    // Carga de Combos (sin clientes - Fase 1)
    // =============================================

    private void loadCombos() {
        try {
            // Ya NO se cargan clientes en un combo. Se buscan por cédula (Fase 1).
            trabajadoraRepo.findAll().forEach(t -> {
                cbTrabajadora.addItem(t);
                cbTrabajadoraPropina.addItem(t); // Fase 3: combo de propinas
            });
            servicioRepo.findAll().forEach(cbServicio::addItem);

            // Renderer compartido para mostrar nombres legibles
            ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Trabajadora t) setText(t.getNombres() + " " + t.getApellidos());
                    else if (value instanceof Servicio s) setText(s.getNombre());
                    else if (value instanceof TipoCabello tc) setText(tc.toString());
                    else if (value == null) setText("-- Seleccionar --");
                    return this;
                }
            };
            cbTrabajadora.setRenderer(renderer);
            cbServicio.setRenderer(renderer);
            cbTipoCabello.setRenderer(renderer);

        } catch (DatabaseException e) {
            ToastNotification.showError(this, "Error cargando datos", e.getMessage());
        }
    }

    // =============================================
    // Lógica de Servicios (sin cambios funcionales)
    // =============================================

    private void updateServiceOptions() {
        Servicio s = (Servicio) cbServicio.getSelectedItem();
        if (s != null) {
            chkClienteTraeProducto.setVisible(s.isPermiteClienteProducto());
            chkClienteTraeProducto.setSelected(false);
        } else {
            chkClienteTraeProducto.setVisible(false);
        }
    }

    private void tryAddService() {
        Trabajadora t = (Trabajadora) cbTrabajadora.getSelectedItem();
        Servicio s = (Servicio) cbServicio.getSelectedItem();
        TipoCabello tc = (TipoCabello) cbTipoCabello.getSelectedItem();

        if (t == null || s == null || tc == null) {
            ToastNotification.showWarning(this, "Complete todos los campos del servicio.");
            return;
        }

        boolean clienteTrae = chkClienteTraeProducto.isSelected();
        double precioFinal = clienteTrae && s.getPrecioClienteProducto() > 0 
                ? s.getPrecioClienteProducto() 
                : s.getPrecio(tc);

        // Si el servicio requiere inventario y el cliente NO trae el producto
        boolean requiereProducto = !clienteTrae && s.getCategoria() != null && 
            (s.getCategoria() == CategoriaServicio.QUIMICO || s.getCategoria().getLabel().contains("Color"));

        if (requiereProducto) {
            ModalOption opt = ModalOption.getDefault().setCloseOnClickOutside(false).setAnimationEnabled(true);
            ProductoSelectorModal modal = new ProductoSelectorModal(producto -> {
                addVentaItem(t, s, precioFinal, clienteTrae, producto);
            });
            ModalManager.showModal(this, modal, opt, "producto_selector");
        } else {
            addVentaItem(t, s, precioFinal, clienteTrae, null);
        }
    }

    private void addVentaItem(Trabajadora t, Servicio s, double precio, boolean clienteTrae, Producto p) {
        VentaItem item = new VentaItem();
        item.setTrabajadoraId(t.getId());
        item.setNombreTrabajadora(t.getNombres() + " " + t.getApellidos());
        item.setServicioId(s.getId());
        item.setNombreServicio(s.getNombre());
        item.setPrecioVenta(precio);
        item.setClienteTrajoProducto(clienteTrae);
        
        String prodName = "N/A";
        if (p != null) {
            item.setProductoId(p.getId());
            prodName = p.getNombre();
        } else if (clienteTrae) {
            prodName = "(Traído por Cliente)";
        }

        ventaActual.getItems().add(item);
        
        tableModel.addRow(new Object[]{
            item.getNombreTrabajadora(),
            item.getNombreServicio(),
            prodName,
            String.format("$ %.2f", precio)
        });

        updateTotals();
        ToastNotification.showSuccess(this, "Servicio Agregado", "Añadido a la orden de venta.");
        
        // Reset form simple
        cbServicio.setSelectedIndex(0);
    }

    private void updateTotals() {
        double subtotal = ventaActual.getItems().stream().mapToDouble(VentaItem::getPrecioVenta).sum();
        ventaActual.setSubtotal(subtotal);

        // Descuento
        double desc = 0.0;
        String tipoDesc = (String) cbTipoDescuento.getSelectedItem();
        if (!"Sin descuento".equals(tipoDesc) && !txtMontoDescuento.getText().trim().isEmpty()) {
            try {
                double val = Double.parseDouble(txtMontoDescuento.getText().trim().replace(",", "."));
                if ("% Porcentaje".equals(tipoDesc)) {
                    desc = subtotal * (val / 100.0);
                    ventaActual.setTipoDescuento("PORCENTAJE");
                } else {
                    desc = val;
                    ventaActual.setTipoDescuento("MONTO");
                }
                // No permitir descuento mayor al subtotal
                if (desc > subtotal) desc = subtotal;
            } catch (Exception e) {}
        } else {
            ventaActual.setTipoDescuento(null);
        }
        ventaActual.setMontoDescuento(desc);
        
        double subtotalConDesc = subtotal - desc;

        double iva = ivaExento ? 0.0 : subtotalConDesc * IVA_RATE;
        ventaActual.setMontoIva(iva);
        ventaActual.setTotal(subtotalConDesc + iva);

        lblSubtotal.setText(String.format("Subtotal: $ %.2f", subtotal));
        lblDescuento.setText(String.format("Descuento: -$ %.2f", desc));
        lblIva.setText(ivaExento ? "IVA: EXENTO" : String.format("IVA (16%%): $ %.2f", iva));
        lblTotal.setText(String.format("Total USD: $ %.2f", ventaActual.getTotal()));
        
        double rate = BCVService.getCachedRate();
        lblTotalBS.setText(String.format("Total Bs: %.2f", ventaActual.getTotal() * rate));

        btnProcesarVenta.setEnabled(!ventaActual.getItems().isEmpty());
        
        actualizarSaldoRestante(); // Actualizar saldo al cambiar totales
    }

    // =============================================
    // FASE 4.5: Descuentos
    // =============================================
    
    private void aplicarDescuento() {
        if (ventaActual.getItems().isEmpty()) {
            ToastNotification.showWarning(this, "Agregue servicios primero.");
            return;
        }
        if ("Sin descuento".equals(cbTipoDescuento.getSelectedItem())) {
            txtMontoDescuento.setText("");
            updateTotals();
            return;
        }
        
        // Petición de clave admin
        String clave = null;
        if (AuthService.getCurrentUser() != null && "ADMIN".equals(AuthService.getCurrentUser().getRol())) {
            // El usuario actual ya es ADMIN, no pedir clave adicional a menos que se quiera por seguridad extrema.
            // Para mantener el flujo rápido, asumimos que puede aplicar el descuento.
            clave = "ADMIN_OVERRIDE";
        } else {
            JPasswordField pf = new JPasswordField();
            int okCxl = JOptionPane.showConfirmDialog(this, pf, "Ingrese contraseña de Administrador", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (okCxl == JOptionPane.OK_OPTION) {
                clave = new String(pf.getPassword());
            }
        }

        if ("ADMIN_OVERRIDE".equals(clave) || (clave != null && AuthService.authenticateAdmin(clave))) {
            updateTotals();
            ToastNotification.showSuccess(this, "Descuento Aplicado", "El descuento ha sido aplicado al total.");
        } else if (clave != null) {
            ToastNotification.showError(this, "Clave Incorrecta", "Acceso denegado o clave incorrecta.");
            cbTipoDescuento.setSelectedIndex(0);
            txtMontoDescuento.setText("");
            updateTotals();
        }
    }

    // =============================================
    // Procesamiento de Venta (Fase 2: modo histórico)
    // =============================================

    private void submitVenta() {
        if (ventaActual.getItems().isEmpty()) return;

        try {
            ventaActual.setClienteId(clienteSeleccionado != null ? clienteSeleccionado.getId() : null);

            // Fase 2: si modo histórico, usar fecha/correlativo/tasa del panel
            if (modoHistorico) {
                Date date = (Date) spinnerFechaHistorica.getValue();
                LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                ventaActual.setFechaVenta(ld.atStartOfDay());

                String corrHist = txtCorrelativoHistorico.getText().trim();
                if (!corrHist.isEmpty()) {
                    ventaActual.setNumeroCorrelativo(corrHist);
                }

                String tasaStr = txtTasaHistorica.getText().trim().replace(",", ".");
                if (!tasaStr.isEmpty()) {
                    ventaActual.setTasaBcv(Double.parseDouble(tasaStr));
                }
            } else {
                ventaActual.setFechaVenta(LocalDateTime.now());
            }

            // Fase 4: transferir pagos de la tabla UI al modelo
            ventaActual.getPagos().clear();
            for (int i = 0; i < tblPagosModel.getRowCount(); i++) {
                Pago pago = new Pago();
                pago.setMetodoPago((String) tblPagosModel.getValueAt(i, 0));
                pago.setMoneda((String) tblPagosModel.getValueAt(i, 1));
                pago.setMonto((Double) tblPagosModel.getValueAt(i, 2));
                pago.setDestinoPago((String) tblPagosModel.getValueAt(i, 3));
                pago.setReferenciaPago((String) tblPagosModel.getValueAt(i, 4));
                pago.setTasaBcvAlPago(BCVService.getCachedRate());
                ventaActual.getPagos().add(pago);
            }

            // Fase 3: transferir propinas de la tabla UI al modelo
            ventaActual.getPropinas().clear();
            for (int i = 0; i < tblPropinaModel.getRowCount(); i++) {
                Propina p = new Propina();
                p.setNombreTrabajadora((String) tblPropinaModel.getValueAt(i, 0));
                // Buscar ID de trabajadora por nombre
                String nombreProp = p.getNombreTrabajadora();
                for (int j = 0; j < cbTrabajadoraPropina.getItemCount(); j++) {
                    Trabajadora tw = cbTrabajadoraPropina.getItemAt(j);
                    if ((tw.getNombres() + " " + tw.getApellidos()).equals(nombreProp)) {
                        p.setTrabajadoraId(tw.getId());
                        break;
                    }
                }
                p.setMonto((Double) tblPropinaModel.getValueAt(i, 1));
                ventaActual.getPropinas().add(p);
            }

            // Fase 4.5: Validar saldo restante y Cuentas por Cobrar
            double totalPagado = 0;
            double rate = BCVService.getCachedRate();
            for (Pago p : ventaActual.getPagos()) {
                if ("Bs".equals(p.getMoneda()) && rate > 0) {
                    totalPagado += p.getMonto() / rate;
                } else {
                    totalPagado += p.getMonto();
                }
            }
            
            double restante = ventaActual.getTotal() - totalPagado;
            if (restante > 0.01) {
                if (clienteSeleccionado == null) {
                    ToastNotification.showError(this, "Pago Incompleto", "Para dejar deuda pendiente debe seleccionar un cliente.");
                    return;
                }
                
                int resp = JOptionPane.showConfirmDialog(this, 
                    String.format("La venta tiene un saldo pendiente de $%.2f.\n¿Desea registrarlo como Cuenta por Cobrar?", restante),
                    "Pago Incompleto", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (resp == JOptionPane.YES_OPTION) {
                    ventaActual.setEstatus("PENDIENTE");
                } else {
                    return; // No procesar
                }
            } else {
                ventaActual.setEstatus("PAGADA");
            }

            ventaService.procesarVenta(ventaActual);

            ToastNotification.showSuccess(this, "Venta Exitosa",
                "Factura #" + ventaActual.getNumeroCorrelativo() + " procesada correctamente.");
                
            // Fase 5: Generar y abrir ticket PDF
            String nombreClientePdf = clienteSeleccionado != null ? clienteSeleccionado.getNombreCompleto() : "Cliente Casual";
            TicketPDFService.generateAndOpenTicket(ventaActual, nombreClientePdf);

            // Limpiar todo
            ventaActual.getItems().clear();
            ventaActual.getPagos().clear();
            ventaActual.getPropinas().clear();
            ventaActual.setNumeroCorrelativo(null);
            tableModel.setRowCount(0);
            tblPagosModel.setRowCount(0);
            tblPropinaModel.setRowCount(0);
            actualizarTotalPropinas();
            actualizarSaldoRestante();
            txtMontoDescuento.setText("");
            cbTipoDescuento.setSelectedIndex(0);
            ivaExento = false;
            updateTotals();
            limpiarSeleccionCliente();
            cargarCorrelativoActual();

        } catch (NumberFormatException e) {
            ToastNotification.showError(this, "Error", "Tasa BCV o correlativo inválido.");
        } catch (ValidationException e) {
            ToastNotification.showValidationErrors(this, e);
        } catch (DatabaseException e) {
            ToastNotification.showError(this, "Error de Sistema", "No se pudo procesar la venta: " + e.getMessage());
        }
    }

    // =============================================
    // FASE 2: Panel Histórico (Ctrl+F4)
    // =============================================

    private JPanel createHistoricoPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 8 15 8 15, fillx", "[]10[120!]20[]10[120!]20[]10[120!]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:darken($Panel.background,3%)");

        JLabel lblTag = new JLabel("⏳ MODO HISTÓRICO");
        lblTag.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Warning.color");

        panel.add(new JLabel("Fecha:"));
        spinnerFechaHistorica = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spinnerFechaHistorica, "dd/MM/yyyy");
        spinnerFechaHistorica.setEditor(dateEditor);
        spinnerFechaHistorica.addChangeListener(e -> onFechaHistoricaChanged());
        panel.add(spinnerFechaHistorica);

        panel.add(new JLabel("Correlativo:"));
        txtCorrelativoHistorico = new JTextField();
        txtCorrelativoHistorico.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "000000");
        txtCorrelativoHistorico.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(txtCorrelativoHistorico);

        panel.add(new JLabel("Tasa BCV:"));
        txtTasaHistorica = new JTextField();
        txtTasaHistorica.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0.00");
        txtTasaHistorica.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panel.add(txtTasaHistorica);

        return panel;
    }

    private void toggleModoHistorico() {
        modoHistorico = !modoHistorico;
        panelHistorico.setVisible(modoHistorico);
        revalidate();
        repaint();

        if (modoHistorico) {
            ToastNotification.showWarning(this, "Modo Histórico", "Ingresando ventas en modo histórico. Los datos de fecha, correlativo y tasa se toman del panel superior.");
            onFechaHistoricaChanged();
        } else {
            ToastNotification.showInfo(this, "Modo Normal", "Volviendo al modo de facturación normal.");
            cargarCorrelativoActual();
        }
    }

    private void onFechaHistoricaChanged() {
        Date date = (Date) spinnerFechaHistorica.getValue();
        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        try {
            Double tasa = ventaService.buscarTasaBcvPorFecha(ld);
            if (tasa != null) {
                txtTasaHistorica.setText(String.format("%.2f", tasa));
                ToastNotification.showInfo(this, "Tasa encontrada: " + String.format("%.2f", tasa) + " para " + ld);
            } else {
                txtTasaHistorica.setText("");
                ToastNotification.showWarning(this, "No se encontró tasa BCV para " + ld + ". Ingrese manualmente.");
            }
        } catch (DatabaseException e) {
            logger.warn("Error buscando tasa por fecha: {}", e.getMessage());
        }
    }

    // =============================================
    // FASE 2: IVA Exento (Ctrl+I)
    // =============================================

    private void toggleIvaExento() {
        if (!ivaExento) {
            String clave = JOptionPane.showInputDialog(this,
                "Ingrese la clave de autorización para exentar IVA:",
                "Autorización Requerida", JOptionPane.WARNING_MESSAGE);

            if ("Capelli2026".equals(clave)) {
                ivaExento = true;
                ToastNotification.showSuccess(this, "IVA Exento", "Se ha eliminado el IVA de esta transacción.");
                updateTotals();
            } else if (clave != null) {
                ToastNotification.showError(this, "Clave Incorrecta", "La clave ingresada no es válida.");
            }
        } else {
            ivaExento = false;
            ToastNotification.showInfo(this, "IVA Restaurado", "El IVA 16% ha sido re-aplicado.");
            updateTotals();
        }
    }

    // =============================================
    // FASE 2: Correlativo
    // =============================================

    private void cargarCorrelativoActual() {
        try {
            String correlativo = ventaService.obtenerCorrelativoActual();
            lblNumeroFactura.setText("Factura #" + correlativo);
            lblTasaBcvActual.setText("Tasa BCV: " + String.format("%.2f", BCVService.getCachedRate()));
        } catch (DatabaseException e) {
            logger.warn("No se pudo cargar el correlativo: {}", e.getMessage());
            lblNumeroFactura.setText("Factura #------");
        }
    }

    // =============================================
    // FASE 2: Keybindings
    // =============================================

    private void registerKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Ctrl+F4 → Toggle modo histórico
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.CTRL_DOWN_MASK), "toggleHistorico");
        am.put("toggleHistorico", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleModoHistorico();
            }
        });

        // Ctrl+I → Toggle IVA exento
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "toggleIva");
        am.put("toggleIva", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleIvaExento();
            }
        });
    }

    // =============================================
    // FASE 3: Panel de Propinas
    // =============================================

    private JPanel createPropinasPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", "[]5[]5[]5[]"));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel("Propinas");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panel.add(lblTitle);

        // Fila de entrada: [Trabajadora] [Monto] [+]
        JPanel rowInput = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]5[80!]5[]", "[]"));
        rowInput.setOpaque(false);

        cbTrabajadoraPropina = new JComboBox<>();
        cbTrabajadoraPropina.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Trabajadora t) setText(t.getNombres() + " " + t.getApellidos());
                return this;
            }
        });
        rowInput.add(cbTrabajadoraPropina);

        txtMontoPropina = new JTextField();
        txtMontoPropina.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "$0.00");
        txtMontoPropina.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        rowInput.add(txtMontoPropina);

        JButton btnAgregarPropina = new JButton("+");
        btnAgregarPropina.putClientProperty(FlatClientProperties.STYLE, "arc:8; background:$Component.accentColor; foreground:#fff; font:bold");
        btnAgregarPropina.addActionListener(e -> agregarPropina());
        rowInput.add(btnAgregarPropina);

        panel.add(rowInput);

        // Tabla de propinas
        tblPropinaModel = new DefaultTableModel(new String[]{"Trabajadora", "Monto ($)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) { return col == 1 ? Double.class : String.class; }
        };
        tblPropinas = new JTable(tblPropinaModel);
        tblPropinas.setRowHeight(26);

        // Menú contextual para eliminar
        JPopupMenu popupPropina = new JPopupMenu();
        JMenuItem itemEliminar = new JMenuItem("Eliminar Propina");
        itemEliminar.addActionListener(e -> eliminarPropina());
        popupPropina.add(itemEliminar);
        tblPropinas.setComponentPopupMenu(popupPropina);

        JScrollPane scrollPropinas = new JScrollPane(tblPropinas);
        scrollPropinas.setPreferredSize(new Dimension(0, 90));
        panel.add(scrollPropinas);

        lblTotalPropinas = new JLabel("Total Propinas: $0.00");
        lblTotalPropinas.putClientProperty(FlatClientProperties.STYLE, "font:bold");
        panel.add(lblTotalPropinas);

        return panel;
    }

    private void agregarPropina() {
        Trabajadora t = (Trabajadora) cbTrabajadoraPropina.getSelectedItem();
        if (t == null) {
            ToastNotification.showWarning(this, "Seleccione una trabajadora para la propina.");
            return;
        }

        String montoStr = txtMontoPropina.getText().trim().replace(",", ".");
        if (montoStr.isEmpty()) {
            ToastNotification.showWarning(this, "Ingrese el monto de la propina.");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
            if (monto <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ToastNotification.showError(this, "Monto inválido", "Ingrese un número positivo.");
            return;
        }

        String nombreCompleto = t.getNombres() + " " + t.getApellidos();

        // Verificar duplicado
        for (int i = 0; i < tblPropinaModel.getRowCount(); i++) {
            if (tblPropinaModel.getValueAt(i, 0).equals(nombreCompleto)) {
                int opcion = JOptionPane.showConfirmDialog(this,
                    "Ya existe una propina para " + nombreCompleto + ".\n¿Desea sumar al monto existente?",
                    "Propina Duplicada", JOptionPane.YES_NO_CANCEL_OPTION);
                if (opcion == JOptionPane.YES_OPTION) {
                    double actual = (Double) tblPropinaModel.getValueAt(i, 1);
                    tblPropinaModel.setValueAt(actual + monto, i, 1);
                    txtMontoPropina.setText("");
                    actualizarTotalPropinas();
                    ToastNotification.showSuccess(this, "Propina actualizada para " + nombreCompleto);
                    return;
                } else if (opcion == JOptionPane.NO_OPTION) {
                    tblPropinaModel.setValueAt(monto, i, 1);
                    txtMontoPropina.setText("");
                    actualizarTotalPropinas();
                    ToastNotification.showSuccess(this, "Propina reemplazada para " + nombreCompleto);
                    return;
                } else {
                    return;
                }
            }
        }

        tblPropinaModel.addRow(new Object[]{nombreCompleto, monto});
        txtMontoPropina.setText("");
        actualizarTotalPropinas();
        ToastNotification.showSuccess(this, "Propina Agregada", "$" + String.format("%.2f", monto) + " para " + nombreCompleto);
    }

    private void eliminarPropina() {
        int row = tblPropinas.getSelectedRow();
        if (row >= 0) {
            String nombre = (String) tblPropinaModel.getValueAt(row, 0);
            tblPropinaModel.removeRow(row);
            actualizarTotalPropinas();
            ToastNotification.showInfo(this, "Propina eliminada para " + nombre);
        }
    }

    private void actualizarTotalPropinas() {
        double total = 0;
        for (int i = 0; i < tblPropinaModel.getRowCount(); i++) {
            total += (Double) tblPropinaModel.getValueAt(i, 1);
        }
        lblTotalPropinas.setText(String.format("Total Propinas: $%.2f", total));
    }

    // =============================================
    // FASE 4: Panel de Pagos Múltiples
    // =============================================

    private JPanel createPagosPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 5, fillx, wrap 1", "[grow, fill]", "[]5[]5[]5[]"));
        panel.setOpaque(false);

        JLabel lblTitle = new JLabel("Registrar Pagos");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2; foreground:$Component.accentColor");
        panel.add(lblTitle);

        // Fila 1: Método + Moneda
        JPanel row1 = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]5[90!]", "[]"));
        row1.setOpaque(false);
        cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Zelle", "Pago Móvil", "Transferencia", "Punto de Venta"});
        cbMetodoPago.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        cbMetodoPago.addActionListener(e -> onMetodoPagoChanged());
        row1.add(cbMetodoPago);

        cbMonedaPago = new JComboBox<>(new String[]{"$", "Bs"});
        cbMonedaPago.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        row1.add(cbMonedaPago);
        panel.add(row1);

        // Fila 2: Monto + Referencia
        JPanel row2 = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]5[grow, fill]", "[]"));
        row2.setOpaque(false);
        txtMontoPago = new JTextField();
        txtMontoPago.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Monto");
        txtMontoPago.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        row2.add(txtMontoPago);

        txtReferenciaPago = new JTextField();
        txtReferenciaPago.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Referencia (opcional)");
        txtReferenciaPago.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        row2.add(txtReferenciaPago);
        panel.add(row2);

        // Fila 3: Destino Pago Móvil (visible solo si es PagoMóvil/Transferencia)
        panelPagoMovil = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]5[grow, fill]", "[]"));
        panelPagoMovil.setOpaque(false);
        panelPagoMovil.setVisible(false);
        cbDestinoPM = new JComboBox<>(new String[]{"Rosa", "Jean"});
        cbDestinoPM.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panelPagoMovil.add(cbDestinoPM);
        cbBancoRosa = new JComboBox<>(new String[]{"Banesco", "Mercantil", "Venezuela", "Provincial"});
        cbBancoRosa.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        panelPagoMovil.add(cbBancoRosa);
        panel.add(panelPagoMovil);

        // Botón agregar pago
        JButton btnAgregarPago = new JButton("+ Agregar Pago");
        btnAgregarPago.putClientProperty(FlatClientProperties.STYLE, "arc:8; background:$Component.accentColor; foreground:#fff; font:bold");
        btnAgregarPago.addActionListener(e -> agregarPago());
        panel.add(btnAgregarPago);

        // Tabla de pagos registrados
        tblPagosModel = new DefaultTableModel(new String[]{"Método", "Moneda", "Monto", "Destino", "Ref."}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int col) { return col == 2 ? Double.class : String.class; }
        };
        tblPagos = new JTable(tblPagosModel);
        tblPagos.setRowHeight(26);

        JPopupMenu popupPago = new JPopupMenu();
        JMenuItem itemElimPago = new JMenuItem("Eliminar Pago");
        itemElimPago.addActionListener(e -> eliminarPago());
        popupPago.add(itemElimPago);
        tblPagos.setComponentPopupMenu(popupPago);

        JScrollPane scrollPagos = new JScrollPane(tblPagos);
        scrollPagos.setPreferredSize(new Dimension(0, 90));
        panel.add(scrollPagos);

        // Saldo restante
        lblSaldoRestante = new JLabel("Saldo restante: $0.00");
        lblSaldoRestante.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:$Warning.color");
        panel.add(lblSaldoRestante);

        return panel;
    }

    private void onMetodoPagoChanged() {
        String method = (String) cbMetodoPago.getSelectedItem();
        boolean showDest = "Pago Móvil".equals(method) || "Transferencia".equals(method);
        panelPagoMovil.setVisible(showDest);
        revalidate();
    }

    private void agregarPago() {
        String montoStr = txtMontoPago.getText().trim().replace(",", ".");
        if (montoStr.isEmpty()) {
            ToastNotification.showWarning(this, "Ingrese el monto del pago.");
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
            if (monto <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ToastNotification.showError(this, "Monto Inválido", "Ingrese un número positivo.");
            return;
        }

        String metodo = (String) cbMetodoPago.getSelectedItem();
        String moneda = (String) cbMonedaPago.getSelectedItem();
        String referencia = txtReferenciaPago.getText().trim();

        String destino = "";
        if (panelPagoMovil.isVisible()) {
            String dest = (String) cbDestinoPM.getSelectedItem();
            String banco = (String) cbBancoRosa.getSelectedItem();
            destino = dest + " - " + banco;
        }

        tblPagosModel.addRow(new Object[]{metodo, moneda, monto, destino, referencia});

        txtMontoPago.setText("");
        txtReferenciaPago.setText("");
        actualizarSaldoRestante();

        ToastNotification.showSuccess(this, "Pago Registrado",
            String.format("%s %s %.2f", metodo, moneda, monto));
    }

    private void eliminarPago() {
        int row = tblPagos.getSelectedRow();
        if (row >= 0) {
            tblPagosModel.removeRow(row);
            actualizarSaldoRestante();
            ToastNotification.showInfo(this, "Pago eliminado.");
        }
    }

    private void actualizarSaldoRestante() {
        double totalPagado = 0;
        double rate = BCVService.getCachedRate();
        for (int i = 0; i < tblPagosModel.getRowCount(); i++) {
            double monto = (Double) tblPagosModel.getValueAt(i, 2);
            String moneda = (String) tblPagosModel.getValueAt(i, 1);
            if ("Bs".equals(moneda) && rate > 0) {
                totalPagado += monto / rate;
            } else {
                totalPagado += monto;
            }
        }

        double restante = ventaActual.getTotal() - totalPagado;
        if (restante <= 0.01) {
            lblSaldoRestante.setText(String.format("Saldo: PAGADO ✓ (Vuelto: $%.2f)", Math.abs(restante)));
            lblSaldoRestante.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:$Success.color");
        } else {
            lblSaldoRestante.setText(String.format("Saldo restante: $%.2f", restante));
            lblSaldoRestante.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:$Warning.color");
        }
    }
}
