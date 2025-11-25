package app.view.modals;

import app.component.Modal;
import app.exception.ValidationException;
import app.exception.cliente.ClienteDuplicadoException;
import app.exception.cliente.ClienteException;
import app.exception.cliente.ClienteNotFoundException;
import app.model.Cliente;
import app.model.TipoCabello;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import app.util.validator.ValidadorVenezolano;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal para crear/editar clientes con validación robusta
 * Patrones: MVC + Command + Strategy (validación)
 */
public class ClienteModal extends Modal {
    
    private static final Logger logger = LoggerFactory.getLogger(ClienteModal.class);
    
    private final ClienteRepository repository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ClienteCallback callback;
    private final ValidadorVenezolano validador;
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
        logger.debug("Creando ClienteModal - Modo: {}", cliente == null ? "CREAR" : "EDITAR");
        this.clienteActual = cliente;
        this.callback = callback;
        this.repository = new ClienteRepositorySQLite();
        this.validador = ValidadorVenezolano.getInstance();
    }
    
    @Override
    public void installComponent() {
        logger.debug("Instalando componentes del modal");
        
        setLayout(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        setPreferredSize(new Dimension(550, 650));
        
        putClientProperty(FlatClientProperties.STYLE, "arc:15");
        
        createHeader();
        createFormFields();
        createButtons();
        
        if (clienteActual != null) {
            loadData();
        }
        
        logger.debug("✓ Componentes instalados");
    }
    
    private void createHeader() {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);
        
        String title = clienteActual == null ? "Nuevo Cliente" : "Editar Cliente";
        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        headerPanel.add(lblTitle);
        
        JButton btnClose = new JButton(new FlatSVGIcon("icons/delete.svg", 0.4f));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "" +
            "arc:999;" +
            "margin:5,5,5,5;" +
            "borderWidth:0;" +
            "focusWidth:0;" +
            "background:null");
        btnClose.addActionListener(e -> {
            logger.debug("Cerrando modal sin guardar");
            getController().closeModal();
        });
        headerPanel.add(btnClose);
        
        add(headerPanel, "growx,gapbottom 15");
        add(new JSeparator(), "growx,gapbottom 10");
    }
    
    private void createFormFields() {
        // Sección: Datos Personales
        addSectionLabel("Datos Personales");
        
        txtCedula = createTextField("V-12345678 o E-12345678");
        add(createFieldPanel("Cédula:", txtCedula));
        
        txtNombre = createTextField("Nombre completo");
        add(createFieldPanel("Nombre:", txtNombre));
        
        txtTelefono = createTextField("0412-1234567");
        add(createFieldPanel("Teléfono:", txtTelefono));
        
        txtDireccion = createTextField("Dirección completa");
        add(createFieldPanel("Dirección:", txtDireccion));
        
        // Sección: Perfil Capilar
        addSectionLabel("Perfil Capilar");
        
        comboTipoCabello = new JComboBox<>(TipoCabello.values());
        add(createFieldPanel("Tipo Cabello:", comboTipoCabello));
        
        txtTipoExtensiones = createTextField("Tipo de extensiones (opcional)");
        add(createFieldPanel("Extensiones:", txtTipoExtensiones));
        
        // Sección: Historial
        addSectionLabel("Historial (DD/MM/AAAA - Opcional)");
        
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
        btnCancelar.addActionListener(e -> {
            logger.debug("Usuario canceló la operación");
            getController().closeModal();
        });
        
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
        logger.debug("Cargando datos del cliente ID: {}", clienteActual.getId());
        
        txtCedula.setText(clienteActual.getCedula());
        txtCedula.setEnabled(false); // No permitir cambiar cédula
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
        
        logger.debug("✓ Datos cargados");
    }
    
    /**
     * Guarda el cliente con validación y manejo de excepciones específicas
     */
    private void guardarCliente() {
        logger.info("Iniciando proceso de guardado de cliente");
        
        // Deshabilitar botón para evitar doble clic
        btnGuardar.setEnabled(false);
        
        try {
            // Paso 1: Validar y crear objeto Cliente
            Cliente cliente = buildAndValidateCliente();
            
            // Paso 2: Guardar en BD
            if (clienteActual == null) {
                logger.debug("Creando nuevo cliente");
                repository.create(cliente);
                logger.info("✓ Cliente creado: {} - {}", cliente.getCedula(), cliente.getNombreCompleto());
            } else {
                logger.debug("Actualizando cliente ID: {}", clienteActual.getId());
                repository.update(cliente);
                logger.info("✓ Cliente actualizado: {} - {}", cliente.getCedula(), cliente.getNombreCompleto());
            }
            
            // Paso 3: Notificar éxito
            if (callback != null) {
                callback.onSuccess(cliente);
            }
            
            // Paso 4: Cerrar modal
            getController().closeModal();
            
        } catch (ValidationException e) {
            logger.warn("Errores de validación: {}", e.getMessage());
            showValidationErrors(e);
            
        } catch (ClienteDuplicadoException e) {
            logger.warn("Cliente duplicado: {}", e.getCedula());
            showError("Ya existe un cliente con la cédula: " + e.getCedula());
            
        } catch (ClienteNotFoundException e) {
            logger.error("Cliente no encontrado: {}", e.getMessage());
            showError("No se encontró el cliente a actualizar. Recargue la lista.");
            
        } catch (ClienteException e) {
            logger.error("Error de negocio: {}", e.getMessage(), e);
            showError("Error al procesar el cliente: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error inesperado guardando cliente", e);
            showError("Error inesperado: " + e.getMessage());
            
        } finally {
            btnGuardar.setEnabled(true);
        }
    }
    
    /**
     * Construye y valida el objeto Cliente
     */
    private Cliente buildAndValidateCliente() throws ValidationException {
        logger.debug("Construyendo y validando cliente");
        
        Cliente cliente = clienteActual != null ? clienteActual : new Cliente();
        
        // Validar y normalizar cédula
        String cedula = validador.validarYNormalizarCedula(txtCedula.getText());
        cliente.setCedula(cedula);
        
        // Validar nombre
        String nombre = txtNombre.getText().trim();
        validador.validarNombre(nombre);
        cliente.setNombreCompleto(nombre);
        
        // Validar y normalizar teléfono (opcional)
        String telefono = validador.validarYNormalizarTelefono(txtTelefono.getText());
        cliente.setTelefono(telefono);
        
        // Dirección (opcional)
        cliente.setDireccion(txtDireccion.getText().trim());
        
        // Tipo de cabello
        cliente.setTipoCabello((TipoCabello) comboTipoCabello.getSelectedItem());
        
        // Extensiones (opcional)
        cliente.setTipoExtensiones(txtTipoExtensiones.getText().trim());
        
        // Fechas (todas opcionales)
        cliente.setFechaCumpleanos(getDateField(txtCumpleanos));
        cliente.setFechaUltimoTinte(getDateField(txtUltimoTinte));
        cliente.setFechaUltimoQuimico(getDateField(txtUltimoQuimico));
        cliente.setFechaUltimaKeratina(getDateField(txtUltimaKeratina));
        cliente.setFechaUltimoMantenimiento(getDateField(txtUltimoMantenimiento));
        
        logger.debug("✓ Cliente validado correctamente");
        return cliente;
    }
    
    /**
     * Muestra errores de validación de forma amigable
     */
    private void showValidationErrors(ValidationException e) {
        StringBuilder message = new StringBuilder();
        message.append("Por favor corrija los siguientes errores:\n\n");
        
        for (ValidationException.ValidationError error : e.getErrors()) {
            message.append("• ").append(error.getMessage()).append("\n");
        }
        
        JOptionPane.showMessageDialog(
            this,
            message.toString(),
            "Errores de Validación",
            JOptionPane.WARNING_MESSAGE
        );
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
            logger.debug("Formato de fecha inválido: {}", text);
            return null; // Opcional, se ignora
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