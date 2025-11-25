package app.view.modals;

import app.model.Cliente;
import app.model.TipoCabello;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.ModalBorderOption;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal para ingresar o editar un cliente
 * Usa SimpleModalBorder de la librería modal-dialog
 */
public class ClienteModal extends SimpleModalBorder {
    
    private final ClienteRepository repository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ClienteCallback callback;
    private Cliente clienteActual;
    
    // Componentes UI del formulario
    private JTextField txtCedula;
    private JTextField txtNombre;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JComboBox<TipoCabello> comboTipoCabello;
    private JTextField txtTipoExtensiones;
    private JTextField txtCumpleanos;
    private JTextField txtUltimoTinte;
    private JTextField txtUltimoQuimico;
    private JTextField txtUltimaKeratina;
    private JTextField txtUltimoMantenimiento;

    /**
     * Constructor para nuevo cliente
     */
    public ClienteModal(ClienteCallback callback) {
        this(null, callback);
    }

    /**
     * Constructor para editar cliente existente
     */
    public ClienteModal(Cliente cliente, ClienteCallback callback) {
        super(
            createFormPanel(), 
            cliente == null ? "Nuevo Cliente" : "Editar Cliente",
            new ModalBorderOption()
                .setUseScroll(true)
                .setPadding(ModalBorderOption.PaddingType.LARGE),
            CLOSE_OPTION,
            (controller, action) -> {
                if (action == CLOSE_OPTION) {
                    controller.close();
                }
            }
        );
        
        this.clienteActual = cliente;
        this.callback = callback;
        this.repository = new ClienteRepositorySQLite();
    }

    /**
     * Crea el panel del formulario (se ejecuta antes del constructor completo)
     */
    private static JPanel createFormPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 0", "[grow,fill]", "[]"));
        panel.setPreferredSize(new Dimension(550, 600));
        return panel;
    }

    @Override
    public void installComponent() {
        super.installComponent();
        
        // Ahora sí inicializamos los componentes del formulario
        initializeFormComponents();
        
        // Si es edición, cargar los datos
        if (clienteActual != null) {
            loadData();
            txtCedula.setEnabled(false);
        }
    }

    /**
     * Inicializa todos los componentes del formulario
     */
    private void initializeFormComponents() {
        // Obtener el panel de contenido (component es el panel que pasamos al super)
        JPanel formPanel = (JPanel) component;
        formPanel.removeAll();
        formPanel.setLayout(new MigLayout("fillx,wrap,insets 15", "[120,right][grow,fill]", "[]10[]"));

        // === DATOS PERSONALES ===
        addSectionLabel(formPanel, "Datos Personales");
        
        txtCedula = createTextField("Ej: 1234567890");
        addField(formPanel, "Cédula:", txtCedula);
        
        txtNombre = createTextField("Nombre completo");
        addField(formPanel, "Nombre:", txtNombre);
        
        txtTelefono = createTextField("Ej: 0412-1234567");
        addField(formPanel, "Teléfono:", txtTelefono);
        
        txtDireccion = createTextField("Dirección completa");
        addField(formPanel, "Dirección:", txtDireccion);

        // === PERFIL CAPILAR ===
        addSectionLabel(formPanel, "Perfil Capilar");
        
        comboTipoCabello = new JComboBox<>(TipoCabello.values());
        comboTipoCabello.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        addField(formPanel, "Tipo Cabello:", comboTipoCabello);
        
        txtTipoExtensiones = createTextField("Tipo de extensiones (opcional)");
        addField(formPanel, "Extensiones:", txtTipoExtensiones);

        // === HISTORIAL ===
        addSectionLabel(formPanel, "Historial (DD/MM/AAAA - Opcional)");
        
        txtCumpleanos = createDateField();
        addField(formPanel, "Cumpleaños:", txtCumpleanos);
        
        txtUltimoTinte = createDateField();
        addField(formPanel, "Último Tinte:", txtUltimoTinte);
        
        txtUltimoQuimico = createDateField();
        addField(formPanel, "Último Químico:", txtUltimoQuimico);
        
        txtUltimaKeratina = createDateField();
        addField(formPanel, "Última Keratina:", txtUltimaKeratina);
        
        txtUltimoMantenimiento = createDateField();
        addField(formPanel, "Mantenimiento:", txtUltimoMantenimiento);

        // === BOTONES ===
        JPanel buttonPanel = createButtonPanel();
        formPanel.add(buttonPanel, "span 2,growx,gaptop 20");
    }

    /**
     * Crea el panel de botones
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[]push[]"));
        panel.setOpaque(false);
        
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCancelar.addActionListener(e -> getController().closeModal());
        
        JButton btnGuardar = new JButton(clienteActual == null ? "Guardar" : "Actualizar");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE, 
            "arc:10;background:$Component.accentColor;foreground:#fff");
        btnGuardar.addActionListener(e -> guardarCliente());
        
        panel.add(btnCancelar);
        panel.add(btnGuardar);
        
        return panel;
    }

    /**
     * Agrega una etiqueta de sección
     */
    private void addSectionLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, 
            "font:bold +1;foreground:$Component.accentColor");
        panel.add(label, "span 2,gaptop 10");
        panel.add(new JSeparator(), "span 2,growx,gapbottom 5");
    }

    /**
     * Agrega un campo al formulario
     */
    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        panel.add(label);
        panel.add(field);
    }

    /**
     * Crea un campo de texto estándar
     */
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        return field;
    }

    /**
     * Crea un campo de fecha
     */
    private JTextField createDateField() {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/mm/aaaa");
        field.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        return field;
    }

    /**
     * Carga los datos del cliente en el formulario
     */
    private void loadData() {
        if (clienteActual == null) return;
        
        txtCedula.setText(clienteActual.getCedula() != null ? clienteActual.getCedula() : "");
        txtNombre.setText(clienteActual.getNombreCompleto() != null ? clienteActual.getNombreCompleto() : "");
        txtTelefono.setText(clienteActual.getTelefono() != null ? clienteActual.getTelefono() : "");
        txtDireccion.setText(clienteActual.getDireccion() != null ? clienteActual.getDireccion() : "");
        
        if (clienteActual.getTipoCabello() != null) {
            comboTipoCabello.setSelectedItem(clienteActual.getTipoCabello());
        }
        
        txtTipoExtensiones.setText(clienteActual.getTipoExtensiones() != null ? clienteActual.getTipoExtensiones() : "");
        
        setDateField(txtCumpleanos, clienteActual.getFechaCumpleanos());
        setDateField(txtUltimoTinte, clienteActual.getFechaUltimoTinte());
        setDateField(txtUltimoQuimico, clienteActual.getFechaUltimoQuimico());
        setDateField(txtUltimaKeratina, clienteActual.getFechaUltimaKeratina());
        setDateField(txtUltimoMantenimiento, clienteActual.getFechaUltimoMantenimiento());
    }

    /**
     * Establece el valor de un campo de fecha
     */
    private void setDateField(JTextField field, LocalDate date) {
        if (date != null) {
            field.setText(date.format(dateFormatter));
        }
    }

    /**
     * Obtiene la fecha de un campo de texto
     */
    private LocalDate getDateField(JTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        
        try {
            return LocalDate.parse(text, dateFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Guarda el cliente en la base de datos
     */
    private void guardarCliente() {
        try {
            // Validaciones básicas
            if (txtCedula.getText().trim().isEmpty()) {
                showError("La cédula es obligatoria");
                txtCedula.requestFocus();
                return;
            }
            
            if (txtNombre.getText().trim().isEmpty()) {
                showError("El nombre es obligatorio");
                txtNombre.requestFocus();
                return;
            }

            // Crear o actualizar cliente
            Cliente cliente = clienteActual != null ? clienteActual : new Cliente();
            cliente.setCedula(txtCedula.getText().trim());
            cliente.setNombreCompleto(txtNombre.getText().trim());
            cliente.setTelefono(txtTelefono.getText().trim());
            cliente.setDireccion(txtDireccion.getText().trim());
            cliente.setTipoCabello((TipoCabello) comboTipoCabello.getSelectedItem());
            cliente.setTipoExtensiones(txtTipoExtensiones.getText().trim());
            
            cliente.setFechaCumpleanos(getDateField(txtCumpleanos));
            cliente.setFechaUltimoTinte(getDateField(txtUltimoTinte));
            cliente.setFechaUltimoQuimico(getDateField(txtUltimoQuimico));
            cliente.setFechaUltimaKeratina(getDateField(txtUltimaKeratina));
            cliente.setFechaUltimoMantenimiento(getDateField(txtUltimoMantenimiento));

            // Guardar en base de datos
            if (clienteActual == null) {
                repository.create(cliente);
            } else {
                repository.update(cliente);
            }

            // Notificar éxito mediante callback
            if (callback != null) {
                callback.onSuccess(cliente);
            }

            // Cerrar el modal
            getController().closeModal();

        } catch (Exception e) {
            showError("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Muestra un mensaje de error
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this, 
            message, 
            "Error", 
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Interfaz callback para notificar eventos
     */
    public interface ClienteCallback {
        void onSuccess(Cliente cliente);
    }
}