package app.view.modals;

import app.component.Modal;
import app.model.Cliente;
import app.model.TipoCabello;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal para ingresar o editar un cliente
 * Patrón: MVC + Command
 * 
 * @author Sistema Capelli
 */
public class ClienteModal extends Modal {
    
    private final ClienteRepository repository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ClienteCallback callback;
    private Cliente clienteActual;
    
    // Componentes UI
    private JTextField txtCedula;
    private JTextField txtNombre;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JComboBox<TipoCabello> comboTipoCabello;
    private JTextField txtTipoExtensiones;
    private JFormattedTextField txtCumpleanos;
    private JFormattedTextField txtUltimoTinte;
    private JFormattedTextField txtUltimoQuimico;
    private JFormattedTextField txtUltimaKeratina;
    private JFormattedTextField txtUltimoMantenimiento;
    
    private JButton btnGuardar;
    private JButton btnCancelar;
    
    /**
     * Constructor para nuevo cliente
     */
    public ClienteModal(ClienteCallback callback) {
        this(null, callback);
    }
    
    /**
     * Constructor para editar cliente
     */
    public ClienteModal(Cliente cliente, ClienteCallback callback) {
        this.clienteActual = cliente;
        this.callback = callback;
        this.repository = new ClienteRepositorySQLite();
    }
    
    @Override
    public void installComponent() {
        setLayout(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        setPreferredSize(new Dimension(550, 650));
        
        // Estilo del panel
        putClientProperty(FlatClientProperties.STYLE, "arc:15");
        
        // Crear componentes
        createHeader();
        createFormFields();
        createButtons();
        
        // Cargar datos si es edición
        if (clienteActual != null) {
            loadData();
        }
    }
    
    private void createHeader() {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);
        
        JLabel lblTitle = new JLabel(clienteActual == null ? "Nuevo Cliente" : "Editar Cliente");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        headerPanel.add(lblTitle);
        
        JButton btnClose = new JButton(new FlatSVGIcon("icons/delete.svg", 0.4f));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "" +
            "arc:999;" +
            "margin:5,5,5,5;" +
            "borderWidth:0;" +
            "focusWidth:0;" +
            "background:null");
        btnClose.addActionListener(e -> getController().closeModal());
        headerPanel.add(btnClose);
        
        add(headerPanel, "growx,gapbottom 15");
        add(new JSeparator(), "growx,gapbottom 10");
    }
    
    private void createFormFields() {
        // Sección: Datos Personales
        addSectionLabel("Datos Personales");
        
        txtCedula = createTextField("Cédula");
        add(createFieldPanel("Cédula:", txtCedula));
        
        txtNombre = createTextField("Nombre completo");
        add(createFieldPanel("Nombre:", txtNombre));
        
        txtTelefono = createTextField("Teléfono");
        add(createFieldPanel("Teléfono:", txtTelefono));
        
        txtDireccion = createTextField("Dirección");
        add(createFieldPanel("Dirección:", txtDireccion));
        
        // Sección: Perfil Capilar
        addSectionLabel("Perfil Capilar");
        
        comboTipoCabello = new JComboBox<>(TipoCabello.values());
        add(createFieldPanel("Tipo Cabello:", comboTipoCabello));
        
        txtTipoExtensiones = createTextField("Tipo de extensiones (opcional)");
        add(createFieldPanel("Extensiones:", txtTipoExtensiones));
        
        // Sección: Historial
        addSectionLabel("Historial (DD/MM/AAAA)");
        
        txtCumpleanos = createDateField();
        add(createFieldPanel("Cumpleaños:", txtCumpleanos));
        
        txtUltimoTinte = createDateField();
        add(createFieldPanel("Último Tinte:", txtUltimoTinte));
        
        txtUltimoQuimico = createDateField();
        add(createFieldPanel("Último Químico:", txtUltimoQuimico));
        
        txtUltimaKeratina = createDateField();
        add(createFieldPanel("Última Keratina:", txtUltimaKeratina));
        
        txtUltimoMantenimiento = createDateField();
        add(createFieldPanel("Mantenimiento:", txtUltimoMantenimiento));
    }
    
    private void createButtons() {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10 0 0 0", "[]push[]"));
        buttonPanel.setOpaque(false);
        
        btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCancelar.addActionListener(e -> getController().closeModal());
        
        btnGuardar = new JButton(clienteActual == null ? "Guardar" : "Actualizar");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE, "" +
            "arc:10;" +
            "background:$Component.accentColor;" +
            "foreground:#fff");
        btnGuardar.addActionListener(e -> guardarCliente());
        
        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnGuardar);
        
        add(buttonPanel, "span,growx,gaptop 10");
    }
    
    private JPanel createFieldPanel(String label, JComponent field) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[120]10[grow,fill]", "[]"));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        panel.add(lbl);
        panel.add(field);
        
        return panel;
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        return field;
    }
    
    private JFormattedTextField createDateField() {
        JFormattedTextField field = new JFormattedTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/mm/aaaa");
        field.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        return field;
    }
    
    private void addSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.putClientProperty(FlatClientProperties.STYLE, "" +
            "font:bold +1;" +
            "foreground:$Component.accentColor");
        add(lbl, "gaptop 10,gapbottom 5");
    }
    
    private void loadData() {
        txtCedula.setText(clienteActual.getCedula());
        txtCedula.setEnabled(false);
        txtNombre.setText(clienteActual.getNombreCompleto());
        txtTelefono.setText(clienteActual.getTelefono());
        txtDireccion.setText(clienteActual.getDireccion());
        comboTipoCabello.setSelectedItem(clienteActual.getTipoCabello());
        txtTipoExtensiones.setText(clienteActual.getTipoExtensiones());
        
        setDateField(txtCumpleanos, clienteActual.getFechaCumpleanos());
        setDateField(txtUltimoTinte, clienteActual.getFechaUltimoTinte());
        setDateField(txtUltimoQuimico, clienteActual.getFechaUltimoQuimico());
        setDateField(txtUltimaKeratina, clienteActual.getFechaUltimaKeratina());
        setDateField(txtUltimoMantenimiento, clienteActual.getFechaUltimoMantenimiento());
    }
    
    private void guardarCliente() {
        try {
            // Validaciones
            if (txtCedula.getText().trim().isEmpty()) {
                showError("La cédula es obligatoria");
                return;
            }
            if (txtNombre.getText().trim().isEmpty()) {
                showError("El nombre es obligatorio");
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
            
            // Guardar en BD
            if (clienteActual == null) {
                repository.create(cliente);
            } else {
                repository.update(cliente);
            }
            
            // Notificar éxito
            if (callback != null) {
                callback.onSuccess(cliente);
            }
            
            getController().closeModal();
            
        } catch (Exception e) {
            showError("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setDateField(JFormattedTextField field, LocalDate date) {
        if (date != null) {
            field.setText(date.format(dateFormatter));
        }
    }
    
    private LocalDate getDateField(JFormattedTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDate.parse(text, dateFormatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Interfaz callback para notificar eventos
     * Patrón: Observer
     */
    public interface ClienteCallback {
        void onSuccess(Cliente cliente);
    }
}