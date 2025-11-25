package app.view.forms;

import app.model.Cliente;
import app.model.TipoCabello;
import com.formdev.flatlaf.FlatClientProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class ClienteForm extends JPanel {

    private Cliente clienteActual;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Componentes UI
    private JTextField txtCedula;
    private JTextField txtNombre;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JComboBox<TipoCabello> comboTipoCabello;
    private JTextField txtTipoExtensiones;

    // Campos de Fecha (Usando JTextField simple - TODOS OPCIONALES)
    private JTextField txtCumpleanos;
    private JTextField txtUltimoTinte;
    private JTextField txtUltimoQuimico;
    private JTextField txtUltimaKeratina;
    private JTextField txtUltimoMantenimiento;

    public ClienteForm() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2, fillx, insets 25 35 25 35", "[label, 120]15[grow, fill]", "[]15[]"));
        
        // Estilo del Panel
        putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background;");

        // Título
        JLabel lbTitle = new JLabel("Información del Cliente");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(lbTitle, "span 2, gapbottom 10");

        // --- Datos Personales ---
        addSeparator("Datos Personales");
        
        txtCedula = new JTextField();
        txtCedula.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 1234567890");
        add(new JLabel("Cédula:"));
        add(txtCedula);

        txtNombre = new JTextField();
        txtNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre completo");
        add(new JLabel("Nombre Completo:"));
        add(txtNombre);

        txtTelefono = new JTextField();
        txtTelefono.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 0412-1234567");
        add(new JLabel("Teléfono:"));
        add(txtTelefono);

        txtDireccion = new JTextField();
        txtDireccion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Dirección completa");
        add(new JLabel("Dirección:"));
        add(txtDireccion);

        // --- Datos del Cabello ---
        addSeparator("Perfil Capilar");

        comboTipoCabello = new JComboBox<>(TipoCabello.values());
        add(new JLabel("Tipo de Cabello:"));
        add(comboTipoCabello);

        txtTipoExtensiones = new JTextField();
        txtTipoExtensiones.setToolTipText("Dejar vacío si no tiene");
        txtTipoExtensiones.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: Keratina, Clip, Cosidas...");
        add(new JLabel("Extensiones:"));
        add(txtTipoExtensiones);

        // --- Historial (Fechas) - TODOS OPCIONALES ---
        addSeparator("Historial y Fechas (DD/MM/AAAA - Opcional)");

        txtCumpleanos = createDateField();
        add(new JLabel("Cumpleaños:"));
        add(txtCumpleanos);

        txtUltimoTinte = createDateField();
        add(new JLabel("Último Tinte:"));
        add(txtUltimoTinte);

        txtUltimoQuimico = createDateField();
        add(new JLabel("Último Químico:"));
        add(txtUltimoQuimico);

        txtUltimaKeratina = createDateField();
        add(new JLabel("Última Keratina:"));
        add(txtUltimaKeratina);
        
        txtUltimoMantenimiento = createDateField();
        add(new JLabel("Mantenimiento:"));
        add(txtUltimoMantenimiento);
    }

    // --- Métodos de Lógica ---

    /**
     * Carga los datos de un cliente en el formulario para editar.
     */
    public void loadData(Cliente c) {
        this.clienteActual = c;
        if (c != null) {
            txtCedula.setText(c.getCedula() != null ? c.getCedula() : "");
            txtNombre.setText(c.getNombreCompleto() != null ? c.getNombreCompleto() : "");
            txtTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
            txtDireccion.setText(c.getDireccion() != null ? c.getDireccion() : "");
            
            if (c.getTipoCabello() != null) {
                comboTipoCabello.setSelectedItem(c.getTipoCabello());
            }
            
            txtTipoExtensiones.setText(c.getTipoExtensiones() != null ? c.getTipoExtensiones() : "");
            
            setLocalDate(txtCumpleanos, c.getFechaCumpleanos());
            setLocalDate(txtUltimoTinte, c.getFechaUltimoTinte());
            setLocalDate(txtUltimoQuimico, c.getFechaUltimoQuimico());
            setLocalDate(txtUltimaKeratina, c.getFechaUltimaKeratina());
            setLocalDate(txtUltimoMantenimiento, c.getFechaUltimoMantenimiento());
            
            txtCedula.setEnabled(false); // No permitir cambiar cédula al editar
        } else {
            // Limpiar campos para nuevo cliente
            txtCedula.setText("");
            txtNombre.setText("");
            txtTelefono.setText("");
            txtDireccion.setText("");
            comboTipoCabello.setSelectedIndex(0);
            txtTipoExtensiones.setText("");
            
            txtCumpleanos.setText("");
            txtUltimoTinte.setText("");
            txtUltimoQuimico.setText("");
            txtUltimaKeratina.setText("");
            txtUltimoMantenimiento.setText("");
            
            txtCedula.setEnabled(true);
        }
    }

    /**
     * Recoge los datos del formulario y devuelve un objeto Cliente.
     * Solo valida campos obligatorios al guardar.
     */
    public Cliente getData() throws Exception {
        System.out.println("\n========== DEBUG: getData() iniciado ==========");
        
        try {
            // Validación SOLO al guardar
            String cedula = txtCedula.getText().trim();
            String nombre = txtNombre.getText().trim();
            
            System.out.println("Cédula ingresada: [" + cedula + "]");
            System.out.println("Nombre ingresado: [" + nombre + "]");
            
            if (cedula.isEmpty()) {
                System.err.println("ERROR: Cédula vacía");
                throw new Exception("La cédula es obligatoria.");
            }
            if (nombre.isEmpty()) {
                System.err.println("ERROR: Nombre vacío");
                throw new Exception("El nombre es obligatorio.");
            }

            Cliente c = (clienteActual != null) ? clienteActual : new Cliente();
            
            c.setCedula(cedula);
            c.setNombreCompleto(nombre);
            c.setTelefono(txtTelefono.getText().trim());
            c.setDireccion(txtDireccion.getText().trim());
            c.setTipoCabello((TipoCabello) comboTipoCabello.getSelectedItem());
            c.setTipoExtensiones(txtTipoExtensiones.getText().trim());
            
            System.out.println("Procesando fechas...");
            // Las fechas son opcionales - retornan null si están vacías o con formato incorrecto
            c.setFechaCumpleanos(getLocalDate(txtCumpleanos));
            c.setFechaUltimoTinte(getLocalDate(txtUltimoTinte));
            c.setFechaUltimoQuimico(getLocalDate(txtUltimoQuimico));
            c.setFechaUltimaKeratina(getLocalDate(txtUltimaKeratina));
            c.setFechaUltimoMantenimiento(getLocalDate(txtUltimoMantenimiento));

            System.out.println("✓ Cliente creado exitosamente: " + c.getNombreCompleto());
            System.out.println("========== DEBUG: getData() finalizado ==========\n");
            return c;
            
        } catch (Exception e) {
            System.err.println("✗ ERROR en getData(): " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== DEBUG: getData() con error ==========\n");
            throw e;
        }
    }

    // --- Helpers Visuales ---

    private void addSeparator(String text) {
        JLabel lb = new JLabel(text);
        lb.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        add(lb, "span 2, gaptop 10");
        add(new JSeparator(), "span 2, growx, gapbottom 5");
    }

    private JTextField createDateField() {
        JTextField f = new JTextField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/mm/yyyy");
        return f;
    }
    
    // --- Helpers de Fecha ---
    
    private void setLocalDate(JTextField field, LocalDate date) {
        if (date != null) {
            field.setText(date.format(dateFormatter));
        } else {
            field.setText("");
        }
    }
    
    private LocalDate getLocalDate(JTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return null; // Campo vacío = opcional, retorna null
        }
        try {
            return LocalDate.parse(text, dateFormatter);
        } catch (DateTimeParseException e) {
            // Formato incorrecto = se ignora y retorna null (no es un error bloqueante)
            return null;
        }
    }
}