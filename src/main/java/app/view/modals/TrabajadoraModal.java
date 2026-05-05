package app.view.modals;

import app.component.Modal;
import app.exception.ValidationException;
import app.exception.trabajadora.TrabajadoraDuplicadaException;
import app.exception.trabajadora.TrabajadoraException;
import app.exception.trabajadora.TrabajadoraNotFoundException;
import app.model.CuentaBancaria;
import app.model.Trabajadora;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
import app.util.ToastNotification;
import app.util.validator.ValidadorVenezolano;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal para crear/editar trabajadoras con gestión de cuentas bancarias
 */
public class TrabajadoraModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(TrabajadoraModal.class);

    private final TrabajadoraRepository repository;
    private final TrabajadoraCallback callback;
    private final ValidadorVenezolano validador;
    private Trabajadora trabajadoraActual;

    // Campos personales
    private JTextField txtCedula;
    private JTextField txtNombres;
    private JTextField txtApellidos;
    private JTextField txtTelefono;
    private JTextField txtCorreo;

    // Campos de bono
    private JCheckBox chkBonoActivo;
    private JTextField txtMontoBono;
    private JTextField txtRazonBono;

    // Tabla de cuentas bancarias
    private DefaultTableModel cuentasTableModel;
    private JTable cuentasTable;

    private JButton btnGuardar;

    public TrabajadoraModal(TrabajadoraCallback callback) {
        this(null, callback);
    }

    public TrabajadoraModal(Trabajadora trabajadora, TrabajadoraCallback callback) {
        this.trabajadoraActual = trabajadora;
        this.callback = callback;
        this.repository = new TrabajadoraRepositorySQLite();
        this.validador = ValidadorVenezolano.getInstance();
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(580, 750));
        putClientProperty(FlatClientProperties.STYLE, "arc:15");

        JPanel mainPanel = new JPanel(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        mainPanel.setOpaque(false);

        createHeader(mainPanel);
        createPersonalFields(mainPanel);
        createBonoFields(mainPanel);
        createCuentasSection(mainPanel);
        createButtons(mainPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");

        add(scrollPane, BorderLayout.CENTER);

        if (trabajadoraActual != null) loadData();
    }

    private void createHeader(JPanel parent) {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);

        String title = trabajadoraActual == null ? "Nueva Trabajadora" : "Editar Trabajadora";
        JLabel lblTitle = new JLabel(title);
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +8");
        headerPanel.add(lblTitle);

        JButton btnClose = new JButton(new FlatSVGIcon("icons/delete.svg", 0.4f));
        btnClose.putClientProperty(FlatClientProperties.STYLE,
            "arc:999;margin:5,5,5,5;borderWidth:0;focusWidth:0;background:null");
        btnClose.addActionListener(e -> getController().closeModal());
        headerPanel.add(btnClose);

        parent.add(headerPanel, "growx,gapbottom 15");
        parent.add(new JSeparator(), "growx,gapbottom 10");
    }

    private void createPersonalFields(JPanel parent) {
        addSectionLabel(parent, "Datos Personales");

        txtCedula = createTextField("V-12345678 o E-12345678");
        parent.add(createFieldPanel("Cédula:", txtCedula));

        txtNombres = createTextField("Nombres");
        parent.add(createFieldPanel("Nombres:", txtNombres));

        txtApellidos = createTextField("Apellidos");
        parent.add(createFieldPanel("Apellidos:", txtApellidos));

        txtTelefono = createTextField("0412-1234567");
        parent.add(createFieldPanel("Teléfono:", txtTelefono));

        txtCorreo = createTextField("correo@ejemplo.com (opcional)");
        parent.add(createFieldPanel("Correo:", txtCorreo));
    }

    private void createBonoFields(JPanel parent) {
        addSectionLabel(parent, "Bono Fijo");

        chkBonoActivo = new JCheckBox("Bono activo");
        parent.add(chkBonoActivo, "gapleft 120");

        txtMontoBono = createTextField("0.00");
        parent.add(createFieldPanel("Monto ($):", txtMontoBono));

        txtRazonBono = createTextField("Razón del bono (opcional)");
        parent.add(createFieldPanel("Razón:", txtRazonBono));
    }

    private void createCuentasSection(JPanel parent) {
        addSectionLabel(parent, "Cuentas Bancarias");

        String[] cols = {"Banco", "Tipo", "Número", "Principal"};
        cuentasTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 3 ? Boolean.class : String.class;
            }
        };
        cuentasTable = new JTable(cuentasTableModel);
        cuentasTable.setRowHeight(30);
        cuentasTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold -1");

        JScrollPane cuentasScroll = new JScrollPane(cuentasTable);
        cuentasScroll.setPreferredSize(new Dimension(0, 120));
        cuentasScroll.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");
        parent.add(cuentasScroll, "growx,h 120");

        JPanel cuentaBtns = new JPanel(new MigLayout("insets 5 0 0 0", "[]5[]5[]push"));
        cuentaBtns.setOpaque(false);

        JButton btnAddCuenta = new JButton("+ Agregar");
        btnAddCuenta.putClientProperty(FlatClientProperties.STYLE, "arc:8;font:-1");
        btnAddCuenta.addActionListener(e -> agregarCuenta());

        JButton btnRemoveCuenta = new JButton("- Quitar");
        btnRemoveCuenta.putClientProperty(FlatClientProperties.STYLE, "arc:8;font:-1");
        btnRemoveCuenta.addActionListener(e -> quitarCuenta());

        cuentaBtns.add(btnAddCuenta);
        cuentaBtns.add(btnRemoveCuenta);
        parent.add(cuentaBtns, "growx");
    }

    private void createButtons(JPanel parent) {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10 0 0 0", "[]push[]"));
        buttonPanel.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCancelar.addActionListener(e -> getController().closeModal());

        btnGuardar = new JButton(trabajadoraActual == null ? "Guardar" : "Actualizar");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;background:$Component.accentColor;foreground:#fff");
        btnGuardar.addActionListener(e -> guardar());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnGuardar);
        parent.add(buttonPanel, "span,growx,gaptop 10");
    }

    // ===== Helpers UI =====

    private JPanel createFieldPanel(String label, JComponent field) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[120]10[grow,fill]", "[]"));
        panel.setOpaque(false);
        panel.add(new JLabel(label));
        panel.add(field);
        return panel;
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        return field;
    }

    private void addSectionLabel(JPanel parent, String text) {
        JLabel lbl = new JLabel(text);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font:bold +1;foreground:$Component.accentColor");
        parent.add(lbl, "gaptop 10,gapbottom 5");
    }

    // ===== Cuentas bancarias =====

    private void agregarCuenta() {
        cuentasTableModel.addRow(new Object[]{"", "Ahorro", "", false});
    }

    private void quitarCuenta() {
        int row = cuentasTable.getSelectedRow();
        if (row >= 0) {
            cuentasTableModel.removeRow(row);
        } else {
            ToastNotification.showInfo(this, "Seleccione una cuenta para quitar");
        }
    }

    private List<CuentaBancaria> buildCuentasFromTable() {
        if (cuentasTable.isEditing()) cuentasTable.getCellEditor().stopCellEditing();

        List<CuentaBancaria> cuentas = new ArrayList<>();
        for (int i = 0; i < cuentasTableModel.getRowCount(); i++) {
            String banco = (String) cuentasTableModel.getValueAt(i, 0);
            String tipo = (String) cuentasTableModel.getValueAt(i, 1);
            String numero = (String) cuentasTableModel.getValueAt(i, 2);
            Boolean principal = (Boolean) cuentasTableModel.getValueAt(i, 3);

            if (banco != null && !banco.trim().isEmpty()) {
                cuentas.add(new CuentaBancaria(
                    banco.trim(),
                    tipo != null ? tipo.trim() : "Ahorro",
                    numero != null ? numero.trim() : "",
                    principal != null && principal
                ));
            }
        }
        return cuentas;
    }

    // ===== Carga de datos =====

    private void loadData() {
        txtCedula.setText(trabajadoraActual.getCedula());
        txtCedula.setEnabled(false);
        txtNombres.setText(trabajadoraActual.getNombres());
        txtApellidos.setText(trabajadoraActual.getApellidos());
        txtTelefono.setText(trabajadoraActual.getTelefono());
        txtCorreo.setText(trabajadoraActual.getCorreoElectronico());

        chkBonoActivo.setSelected(trabajadoraActual.isBonoActivo());
        txtMontoBono.setText(String.valueOf(trabajadoraActual.getMontoBono()));
        txtRazonBono.setText(trabajadoraActual.getRazonBono());

        // Cargar cuentas
        if (trabajadoraActual.getCuentas() != null) {
            for (CuentaBancaria c : trabajadoraActual.getCuentas()) {
                cuentasTableModel.addRow(new Object[]{
                    c.getBanco(), c.getTipoDeCuenta(), c.getNumeroDeCuenta(), c.isEsPrincipal()
                });
            }
        }
    }

    // ===== Guardar =====

    private void guardar() {
        btnGuardar.setEnabled(false);
        try {
            Trabajadora t = buildAndValidate();

            if (trabajadoraActual == null) {
                repository.create(t);
                ToastNotification.showSuccess(this, "Trabajadora Registrada", "La trabajadora ha sido registrada exitosamente");
            } else {
                repository.update(t);
                ToastNotification.showSuccess(this, "Trabajadora Actualizada", "Los datos se han actualizado correctamente");
            }

            if (callback != null) callback.onSuccess(t);
            getController().closeModal();

        } catch (ValidationException e) {
            ToastNotification.showValidationErrors(this, e);
        } catch (TrabajadoraDuplicadaException e) {
            ToastNotification.showError(this, "Trabajadora Duplicada", "Ya existe una trabajadora con la cédula: " + e.getCedula());
        } catch (TrabajadoraNotFoundException e) {
            ToastNotification.showError(this, "Error Crítico", "No se encontró la trabajadora a actualizar.");
        } catch (TrabajadoraException e) {
            ToastNotification.showError(this, "Error al Guardar", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado guardando trabajadora", e);
            ToastNotification.showError(this, "Error Inesperado", "Ha ocurrido un error inesperado.");
        } finally {
            btnGuardar.setEnabled(true);
        }
    }

    private Trabajadora buildAndValidate() throws ValidationException {
        Trabajadora t = trabajadoraActual != null ? trabajadoraActual : new Trabajadora();

        // Validar cédula
        String cedula = validador.validarYNormalizarCedula(txtCedula.getText());
        t.setCedula(cedula);

        // Validar nombres
        String nombres = txtNombres.getText().trim();
        validador.validarNombre(nombres);
        t.setNombres(nombres);

        // Validar apellidos
        String apellidos = txtApellidos.getText().trim();
        validador.validarNombre(apellidos);
        t.setApellidos(apellidos);

        // Teléfono (opcional)
        String telefono = validador.validarYNormalizarTelefono(txtTelefono.getText());
        t.setTelefono(telefono);

        // Correo (opcional)
        t.setCorreoElectronico(txtCorreo.getText().trim());

        // Bono
        t.setBonoActivo(chkBonoActivo.isSelected());
        try {
            String montoStr = txtMontoBono.getText().trim();
            t.setMontoBono(montoStr.isEmpty() ? 0.0 : Double.parseDouble(montoStr));
        } catch (NumberFormatException e) {
            throw new ValidationException("Monto Bono", "El monto del bono debe ser un número válido");
        }
        t.setRazonBono(txtRazonBono.getText().trim());

        // Cuentas bancarias
        t.setCuentas(buildCuentasFromTable());

        return t;
    }

    /**
     * Callback para notificar éxito
     */
    public interface TrabajadoraCallback {
        void onSuccess(Trabajadora trabajadora);
    }
}
