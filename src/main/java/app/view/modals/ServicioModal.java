package app.view.modals;

import app.component.Modal;
import app.exception.ValidationException;
import app.exception.servicio.ServicioException;
import app.exception.servicio.ServicioNotFoundException;
import app.model.CategoriaServicio;
import app.model.Servicio;
import app.repository.ServicioRepository;
import app.repository.ServicioRepositorySQLite;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Modal para crear/editar servicios con precios dinámicos por tipo de cabello
 */
public class ServicioModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(ServicioModal.class);

    private final ServicioRepository repository;
    private final ServicioCallback callback;
    private Servicio servicioActual;

    private JTextField txtNombre;
    private JComboBox<CategoriaServicio> comboCategoria;
    private JTextField txtPrecioCorto;
    private JTextField txtPrecioMediano;
    private JTextField txtPrecioLargo;
    private JTextField txtPrecioExtensiones;
    private JCheckBox chkPermiteProducto;
    private JTextField txtPrecioClienteProducto;
    private JButton btnGuardar;

    public ServicioModal(ServicioCallback callback) {
        this(null, callback);
    }

    public ServicioModal(Servicio servicio, ServicioCallback callback) {
        this.servicioActual = servicio;
        this.callback = callback;
        this.repository = new ServicioRepositorySQLite();
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 600));
        putClientProperty(FlatClientProperties.STYLE, "arc:15");

        JPanel mainPanel = new JPanel(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        mainPanel.setOpaque(false);

        createHeader(mainPanel);
        createFormFields(mainPanel);
        createButtons(mainPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");

        add(scrollPane, BorderLayout.CENTER);

        if (servicioActual != null) loadData();
    }

    private void createHeader(JPanel parent) {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);

        String title = servicioActual == null ? "Nuevo Servicio" : "Editar Servicio";
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

    private void createFormFields(JPanel parent) {
        addSectionLabel(parent, "Información del Servicio");

        txtNombre = createTextField("Nombre del servicio");
        parent.add(createFieldPanel("Nombre:", txtNombre));

        comboCategoria = new JComboBox<>(CategoriaServicio.values());
        parent.add(createFieldPanel("Categoría:", comboCategoria));

        addSectionLabel(parent, "Precios por Tipo de Cabello ($)");

        txtPrecioCorto = createTextField("0.00");
        parent.add(createFieldPanel("Corto:", txtPrecioCorto));

        txtPrecioMediano = createTextField("0.00");
        parent.add(createFieldPanel("Mediano:", txtPrecioMediano));

        txtPrecioLargo = createTextField("0.00");
        parent.add(createFieldPanel("Largo:", txtPrecioLargo));

        txtPrecioExtensiones = createTextField("0.00");
        parent.add(createFieldPanel("Extensiones:", txtPrecioExtensiones));

        addSectionLabel(parent, "Producto del Cliente");

        chkPermiteProducto = new JCheckBox("El cliente puede traer su producto");
        parent.add(chkPermiteProducto, "gapleft 120");

        txtPrecioClienteProducto = createTextField("0.00");
        parent.add(createFieldPanel("Precio especial:", txtPrecioClienteProducto));
    }

    private void createButtons(JPanel parent) {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10 0 0 0", "[]push[]"));
        buttonPanel.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCancelar.addActionListener(e -> getController().closeModal());

        btnGuardar = new JButton(servicioActual == null ? "Guardar" : "Actualizar");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;background:$Component.accentColor;foreground:#fff");
        btnGuardar.addActionListener(e -> guardar());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnGuardar);
        parent.add(buttonPanel, "span,growx,gaptop 10");
    }

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

    private void loadData() {
        txtNombre.setText(servicioActual.getNombre());
        comboCategoria.setSelectedItem(servicioActual.getCategoria());
        txtPrecioCorto.setText(String.valueOf(servicioActual.getPrecioCorto()));
        txtPrecioMediano.setText(String.valueOf(servicioActual.getPrecioMediano()));
        txtPrecioLargo.setText(String.valueOf(servicioActual.getPrecioLargo()));
        txtPrecioExtensiones.setText(String.valueOf(servicioActual.getPrecioExtensiones()));
        chkPermiteProducto.setSelected(servicioActual.isPermiteClienteProducto());
        txtPrecioClienteProducto.setText(String.valueOf(servicioActual.getPrecioClienteProducto()));
    }

    private void guardar() {
        btnGuardar.setEnabled(false);
        try {
            Servicio s = buildAndValidate();

            if (servicioActual == null) {
                repository.create(s);
                ToastNotification.showSuccess(this, "Servicio Creado", "El servicio ha sido creado exitosamente");
            } else {
                repository.update(s);
                ToastNotification.showSuccess(this, "Servicio Actualizado", "El servicio ha sido actualizado");
            }

            if (callback != null) callback.onSuccess(s);
            getController().closeModal();

        } catch (ValidationException e) {
            ToastNotification.showValidationErrors(this, e);
        } catch (ServicioNotFoundException e) {
            ToastNotification.showError(this, "Error Crítico", "No se encontró el servicio a actualizar.");
        } catch (ServicioException e) {
            ToastNotification.showError(this, "Error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado guardando servicio", e);
            ToastNotification.showError(this, "Error Inesperado", "Ha ocurrido un error inesperado.");
        } finally {
            btnGuardar.setEnabled(true);
        }
    }

    private Servicio buildAndValidate() throws ValidationException {
        Servicio s = servicioActual != null ? servicioActual : new Servicio();

        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            throw ValidationException.requiredField("Nombre");
        }
        if (nombre.length() < 2) {
            throw ValidationException.invalidLength("Nombre", 2, 100);
        }
        s.setNombre(nombre);

        s.setCategoria((CategoriaServicio) comboCategoria.getSelectedItem());

        s.setPrecioCorto(parsePrice(txtPrecioCorto, "Precio Corto"));
        s.setPrecioMediano(parsePrice(txtPrecioMediano, "Precio Mediano"));
        s.setPrecioLargo(parsePrice(txtPrecioLargo, "Precio Largo"));
        s.setPrecioExtensiones(parsePrice(txtPrecioExtensiones, "Precio Extensiones"));

        s.setPermiteClienteProducto(chkPermiteProducto.isSelected());
        s.setPrecioClienteProducto(parsePrice(txtPrecioClienteProducto, "Precio Cliente Producto"));

        return s;
    }

    private double parsePrice(JTextField field, String fieldName) throws ValidationException {
        String text = field.getText().trim();
        if (text.isEmpty()) return 0.0;
        try {
            double value = Double.parseDouble(text);
            if (value < 0) throw new ValidationException(fieldName, "El precio no puede ser negativo");
            return value;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName, "Debe ser un número válido");
        }
    }

    public interface ServicioCallback {
        void onSuccess(Servicio servicio);
    }
}
