package app.view;

import app.exception.DatabaseException;
import app.exception.ValidationException;
import app.model.*;
import app.option.ModalOption;
import app.repository.*;
import app.service.VentaService;
import app.system.ModalManager;
import app.util.ToastNotification;
import app.view.modals.ProductoSelectorModal;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;

public class VentaView extends JPanel {

    // Repositorios y Servicios
    private final TrabajadoraRepository trabajadoraRepo;
    private final ServicioRepository servicioRepo;
    private final ClienteRepository clienteRepo;
    private final VentaService ventaService;

    // Componentes del Formulario
    private JComboBox<Cliente> cbCliente;
    private JComboBox<Trabajadora> cbTrabajadora;
    private JComboBox<Servicio> cbServicio;
    private JComboBox<TipoCabello> cbTipoCabello;
    private JCheckBox chkClienteTraeProducto;
    private JButton btnAddService;

    // Tabla de Pre-Visualización
    private JTable tableItems;
    private DefaultTableModel tableModel;
    private JLabel lblTotal;
    private JComboBox<String> cbMetodoPago;
    private JButton btnProcesarVenta;

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
        updateTotals();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[350::350, fill][grow, fill]", "[grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // --- Panel Izquierdo: Formulario de Ítems ---
        JPanel pnlForm = new JPanel(new MigLayout("wrap 1, insets 20, fillx", "[grow, fill]", "[]10[]"));
        pnlForm.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        JLabel lblFormTitle = new JLabel("Agregar Servicio");
        lblFormTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +5; foreground:$Component.accentColor");
        pnlForm.add(lblFormTitle);

        pnlForm.add(new JLabel("Cliente (Opcional):"));
        cbCliente = new JComboBox<>();
        pnlForm.add(cbCliente);

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

        // Totales y Pago
        JPanel pnlCheckout = new JPanel(new MigLayout("fillx, insets 10", "[][grow, right]", "[]10[]10[]"));
        pnlCheckout.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        lblTotal = new JLabel("Total: $0.00");
        lblTotal.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Success.color");
        pnlCheckout.add(lblTotal, "span, right, wrap");

        pnlCheckout.add(new JLabel("Método de Pago (100%):"));
        cbMetodoPago = new JComboBox<>(new String[]{"Efectivo USD", "Zelle", "Efectivo Bs", "Pago Móvil"});
        pnlCheckout.add(cbMetodoPago, "wrap");

        btnProcesarVenta = new JButton("Procesar y Facturar Venta");
        btnProcesarVenta.putClientProperty(FlatClientProperties.STYLE, "background:$Success.color; foreground:#fff; font:bold +2; margin:10,10,10,10");
        btnProcesarVenta.addActionListener(e -> submitVenta());
        pnlCheckout.add(btnProcesarVenta, "span, growx");

        pnlResumen.add(pnlCheckout);
        add(pnlResumen);
    }

    private void loadCombos() {
        try {
            cbCliente.addItem(null); // Opción vacía
            clienteRepo.findAll().forEach(cbCliente::addItem);

            trabajadoraRepo.findAll().forEach(cbTrabajadora::addItem);
            servicioRepo.findAll().forEach(cbServicio::addItem);

            // Set renderers para mostrar solo el nombre
            ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Cliente c) setText(c.getNombreCompleto() + " (" + c.getCedula() + ")");
                    else if (value instanceof Trabajadora t) setText(t.getNombres() + " " + t.getApellidos());
                    else if (value instanceof Servicio s) setText(s.getNombre());
                    else if (value instanceof TipoCabello tc) setText(tc.toString());
                    else if (value == null) setText("-- Seleccionar --");
                    return this;
                }
            };
            cbCliente.setRenderer(renderer);
            cbTrabajadora.setRenderer(renderer);
            cbServicio.setRenderer(renderer);
            cbTipoCabello.setRenderer(renderer);

        } catch (DatabaseException e) {
            ToastNotification.showError(this, "Error cargando datos", e.getMessage());
        }
    }

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
        // Asumimos que categorías como Químicos, Color, Tratamientos requieren producto
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
        ventaActual.setTotal(subtotal); // Sin IVA por ahora
        lblTotal.setText(String.format("Total: $ %.2f", ventaActual.getTotal()));
        
        btnProcesarVenta.setEnabled(!ventaActual.getItems().isEmpty());
    }

    private void submitVenta() {
        if (ventaActual.getItems().isEmpty()) return;

        try {
            Cliente c = (Cliente) cbCliente.getSelectedItem();
            ventaActual.setClienteId(c != null ? c.getId() : null);
            ventaActual.setFechaVenta(LocalDateTime.now());

            // Crear un pago único por el total para simplificar
            Pago pago = new Pago();
            pago.setMonto(ventaActual.getTotal());
            String method = (String) cbMetodoPago.getSelectedItem();
            if (method.contains("Bs") || method.contains("Móvil")) {
                pago.setMoneda("Bs");
                // Convertir monto a Bs
                double rate = app.service.BCVService.getCachedRate();
                pago.setMonto(ventaActual.getTotal() * rate);
                pago.setTasaBcvAlPago(rate);
            } else {
                pago.setMoneda("$");
                pago.setTasaBcvAlPago(app.service.BCVService.getCachedRate());
            }
            pago.setMetodoPago(method);
            
            ventaActual.getPagos().clear();
            ventaActual.getPagos().add(pago);

            ventaService.procesarVenta(ventaActual);
            
            ToastNotification.showSuccess(this, "Venta Exitosa", "La venta ha sido facturada correctamente.");
            
            // Limpiar la vista
            ventaActual.getItems().clear();
            ventaActual.getPagos().clear();
            tableModel.setRowCount(0);
            updateTotals();
            cbCliente.setSelectedIndex(0);

        } catch (ValidationException e) {
            ToastNotification.showValidationErrors(this, e);
        } catch (DatabaseException e) {
            ToastNotification.showError(this, "Error de Sistema", "No se pudo procesar la venta: " + e.getMessage());
        }
    }
}
