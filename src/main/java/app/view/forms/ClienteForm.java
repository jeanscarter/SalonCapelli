package app.view.forms;

import app.model.Cliente;
import app.model.TipoCabello;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
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

    // Campos de Fecha (Usamos Texto con formato simple por ahora)
    private JFormattedTextField txtCumpleanos;
    private JFormattedTextField txtUltimoTinte;
    private JFormattedTextField txtUltimoQuimico;
    private JFormattedTextField txtUltimaKeratina;
    private JFormattedTextField txtUltimoMantenimiento;

    public ClienteForm() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2, fillx, insets 25 35 25 35", "[label, 120]15[grow, fill]", "[]15[]"));
        
        // Estilo del Panel (Opcional, para fondo blanco limpio)
        putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background;");

        // Título
        JLabel lbTitle = new JLabel("Información del Cliente");
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +5");
        add(lbTitle, "span 2, gapbottom 10");

        // --- Datos Personales ---
        addSeparator("Datos Personales");
        
        txtCedula = new JTextField();
        add(new JLabel("Cédula:"));
        add(txtCedula);

        txtNombre = new JTextField();
        add(new JLabel("Nombre Completo:"));
        add(txtNombre);

        txtTelefono = new JTextField();
        add(new JLabel("Teléfono:"));
        add(txtTelefono);

        txtDireccion = new JTextField();
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

        // --- Historial (Fechas) ---
        addSeparator("Historial y Fechas (DD/MM/AAAA)");

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
     * Si es null, limpia el formulario para uno nuevo.
     */
    public void loadData(Cliente c) {
        this.clienteActual = c;
        if (c != null) {
            txtCedula.setText(c.getCedula());
            txtNombre.setText(c.getNombreCompleto());
            txtTelefono.setText(c.getTelefono());
            txtDireccion.setText(c.getDireccion());
            comboTipoCabello.setSelectedItem(c.getTipoCabello());
            txtTipoExtensiones.setText(c.getTipoExtensiones());
            
            setLocalDate(txtCumpleanos, c.getFechaCumpleanos());
            setLocalDate(txtUltimoTinte, c.getFechaUltimoTinte());
            setLocalDate(txtUltimoQuimico, c.getFechaUltimoQuimico());
            setLocalDate(txtUltimaKeratina, c.getFechaUltimaKeratina());
            setLocalDate(txtUltimoMantenimiento, c.getFechaUltimoMantenimiento());
            
            txtCedula.setEnabled(false); // No permitir cambiar la cédula al editar (clave única)
        } else {
            // Limpiar campos
            txtCedula.setText("");
            txtNombre.setText("");
            txtTelefono.setText("");
            txtDireccion.setText("");
            comboTipoCabello.setSelectedIndex(0);
            txtTipoExtensiones.setText("");
            
            txtCumpleanos.setValue(null);
            txtUltimoTinte.setValue(null);
            txtUltimoQuimico.setValue(null);
            txtUltimaKeratina.setValue(null);
            txtUltimoMantenimiento.setValue(null);
            
            txtCedula.setEnabled(true);
        }
    }

    /**
     * Recoge los datos del formulario y devuelve un objeto Cliente.
     * Realiza validaciones básicas.
     */
    public Cliente getData() throws Exception {
        // Validación básica
        if (txtCedula.getText().trim().isEmpty()) throw new Exception("La cédula es obligatoria.");
        if (txtNombre.getText().trim().isEmpty()) throw new Exception("El nombre es obligatorio.");

        Cliente c = (clienteActual != null) ? clienteActual : new Cliente();
        
        c.setCedula(txtCedula.getText().trim());
        c.setNombreCompleto(txtNombre.getText().trim());
        c.setTelefono(txtTelefono.getText().trim());
        c.setDireccion(txtDireccion.getText().trim());
        c.setTipoCabello((TipoCabello) comboTipoCabello.getSelectedItem());
        c.setTipoExtensiones(txtTipoExtensiones.getText().trim());
        
        c.setFechaCumpleanos(getLocalDate(txtCumpleanos));
        c.setFechaUltimoTinte(getLocalDate(txtUltimoTinte));
        c.setFechaUltimoQuimico(getLocalDate(txtUltimoQuimico));
        c.setFechaUltimaKeratina(getLocalDate(txtUltimaKeratina));
        c.setFechaUltimoMantenimiento(getLocalDate(txtUltimoMantenimiento));

        return c;
    }

    // --- Helpers Visuales ---

    private void addSeparator(String text) {
        JLabel lb = new JLabel(text);
        lb.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        add(lb, "span 2, gaptop 10");
        add(new JSeparator(), "span 2, growx, gapbottom 5");
    }

    private JFormattedTextField createDateField() {
        // Máscara simple para obligar formato numérico
        JFormattedTextField f = new JFormattedTextField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "dd/mm/yyyy");
        return f;
    }
    
    // --- Helpers de Fecha ---
    
    private void setLocalDate(JFormattedTextField field, LocalDate date) {
        if (date != null) {
            field.setText(date.format(dateFormatter));
        } else {
            field.setText("");
        }
    }
    
    private LocalDate getLocalDate(JFormattedTextField field) {
        String text = field.getText().trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDate.parse(text, dateFormatter);
        } catch (DateTimeParseException e) {
            return null; // O lanzar error si quieres ser estricto
        }
    }
}