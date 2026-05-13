package app.view.modals;

import app.component.Modal;
import app.exception.DatabaseException;
import app.model.CategoriaServicio;
import app.model.ReglaComisionDetallada;
import app.model.Trabajadora;
import app.model.Servicio;
import app.repository.ReglaComisionDetalladaRepository;
import app.repository.ReglaComisionDetalladaRepositorySQLite;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
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
import java.util.List;

public class ComisionModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(ComisionModal.class);

    private final ReglaComisionDetalladaRepository repository;
    private final TrabajadoraRepository trabajadoraRepository;
    private final ServicioRepository servicioRepository;
    private final ComisionCallback callback;
    private ReglaComisionDetallada reglaActual;

    private JTextField txtDescripcion;
    private JComboBox<TrabajadoraWrapper> cbTrabajadora;
    private JComboBox<CategoriaWrapper> cbCategoria;
    private JComboBox<ServicioWrapper> cbServicio;
    private JComboBox<String> cbClienteTraeProducto;
    private JComboBox<String> cbTipoComision;
    private JTextField txtValorComision;
    private JTextField txtPrecioCondicion;
    private JSpinner spinPrioridad;
    private JCheckBox chkActivo;
    private JButton btnGuardar;

    public ComisionModal(ReglaComisionDetallada reglaActual, ComisionCallback callback) {
        this.reglaActual = reglaActual;
        this.callback = callback;
        this.trabajadoraRepository = new TrabajadoraRepositorySQLite();
        this.servicioRepository = new ServicioRepositorySQLite();
        this.repository = new ReglaComisionDetalladaRepositorySQLite();
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(650, 600));
        putClientProperty(FlatClientProperties.STYLE, "arc:15");

        JPanel mainPanel = new JPanel(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        mainPanel.setOpaque(false);

        createHeader(mainPanel);
        createForm(mainPanel);
        createButtons(mainPanel);

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        if (reglaActual != null) {
            loadData();
        }
    }

    private void createHeader(JPanel parent) {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);

        String title = reglaActual == null ? "Nueva Regla de Comisión (Detallada)" : "Editar Regla de Comisión (Detallada)";
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

    private void createForm(JPanel parent) {
        txtDescripcion = new JTextField();
        parent.add(createFieldPanel("Descripción:", txtDescripcion));

        cbTrabajadora = new JComboBox<>();
        cbTrabajadora.addItem(new TrabajadoraWrapper(null)); // "Todas"
        try {
            List<Trabajadora> trabajadoras = trabajadoraRepository.findAll();
            for (Trabajadora t : trabajadoras) {
                cbTrabajadora.addItem(new TrabajadoraWrapper(t));
            }
        } catch (Exception e) {
            logger.error("Error al cargar trabajadoras", e);
        }
        parent.add(createFieldPanel("Trabajadora (Op):", cbTrabajadora));

        cbCategoria = new JComboBox<>();
        cbCategoria.addItem(new CategoriaWrapper(null)); // "Todas"
        for (CategoriaServicio cat : CategoriaServicio.values()) {
            cbCategoria.addItem(new CategoriaWrapper(cat));
        }
        parent.add(createFieldPanel("Categoría (Op):", cbCategoria));

        cbServicio = new JComboBox<>();
        cbServicio.addItem(new ServicioWrapper(null)); // "Todos"
        try {
            List<Servicio> servicios = servicioRepository.findAll();
            for (Servicio s : servicios) {
                cbServicio.addItem(new ServicioWrapper(s));
            }
        } catch (Exception e) {
            logger.error("Error al cargar servicios", e);
        }
        parent.add(createFieldPanel("Servicio (Op):", cbServicio));

        cbClienteTraeProducto = new JComboBox<>(new String[]{"No Importa", "Sí", "No"});
        parent.add(createFieldPanel("Trae Producto?:", cbClienteTraeProducto));

        cbTipoComision = new JComboBox<>(new String[]{"PORCENTAJE", "MONTO_FIJO"});
        parent.add(createFieldPanel("Tipo Comisión:", cbTipoComision));

        txtValorComision = new JTextField();
        txtValorComision.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 0.5 para 50%, o 10 para $10");
        parent.add(createFieldPanel("Valor:", txtValorComision));

        txtPrecioCondicion = new JTextField();
        txtPrecioCondicion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 8 (Solo aplica si venta es $8)");
        parent.add(createFieldPanel("Condición Precio (Op):", txtPrecioCondicion));

        spinPrioridad = new JSpinner(new SpinnerNumberModel(10, 1, 999, 1));
        parent.add(createFieldPanel("Prioridad:", spinPrioridad));

        chkActivo = new JCheckBox("Regla Activa");
        chkActivo.setSelected(true);
        parent.add(chkActivo);
    }

    private void createButtons(JPanel parent) {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 10 0 0 0", "[]push[]"));
        buttonPanel.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCancelar.addActionListener(e -> getController().closeModal());

        btnGuardar = new JButton(reglaActual == null ? "Guardar" : "Actualizar");
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
            "arc:10;background:$Component.accentColor;foreground:#fff");
        btnGuardar.addActionListener(e -> guardar());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnGuardar);
        parent.add(buttonPanel, "span,growx,gaptop 10");
    }

    private JPanel createFieldPanel(String label, JComponent field) {
        JPanel panel = new JPanel(new MigLayout("insets 0", "[140]10[grow,fill]", "[]"));
        panel.setOpaque(false);
        panel.add(new JLabel(label));
        panel.add(field);
        return panel;
    }

    private void loadData() {
        txtDescripcion.setText(reglaActual.getDescripcion());
        chkActivo.setSelected(reglaActual.isActivo());
        
        for (int i = 0; i < cbTrabajadora.getItemCount(); i++) {
            TrabajadoraWrapper w = cbTrabajadora.getItemAt(i);
            if ((reglaActual.getTrabajadoraId() == null && w.getId() == null) || 
                (reglaActual.getTrabajadoraId() != null && w.getId() != null && w.getId().equals(reglaActual.getTrabajadoraId()))) {
                cbTrabajadora.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < cbCategoria.getItemCount(); i++) {
            CategoriaWrapper w = cbCategoria.getItemAt(i);
            if ((reglaActual.getCategoriaServicio() == null && w.getNombre() == null) || 
                (reglaActual.getCategoriaServicio() != null && w.getNombre() != null && w.getNombre().equals(reglaActual.getCategoriaServicio()))) {
                cbCategoria.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < cbServicio.getItemCount(); i++) {
            ServicioWrapper w = cbServicio.getItemAt(i);
            if ((reglaActual.getServicioId() == null && w.getId() == null) || 
                (reglaActual.getServicioId() != null && w.getId() != null && w.getId().equals(reglaActual.getServicioId()))) {
                cbServicio.setSelectedIndex(i);
                break;
            }
        }

        if (reglaActual.getClienteTraeProducto() == null) cbClienteTraeProducto.setSelectedIndex(0);
        else if (reglaActual.getClienteTraeProducto()) cbClienteTraeProducto.setSelectedIndex(1);
        else cbClienteTraeProducto.setSelectedIndex(2);

        cbTipoComision.setSelectedItem(reglaActual.getTipoComision());
        txtValorComision.setText(String.valueOf(reglaActual.getValorComision()));
        
        if (reglaActual.getPrecioCondicion() != null) {
            txtPrecioCondicion.setText(String.valueOf(reglaActual.getPrecioCondicion()));
        }

        spinPrioridad.setValue(reglaActual.getPrioridad());
    }

    private void guardar() {
        btnGuardar.setEnabled(false);
        try {
            String desc = txtDescripcion.getText().trim();
            if (desc.isEmpty()) {
                ToastNotification.showWarning(this, "Atención", "Debe ingresar una descripción");
                return;
            }

            double valor;
            try {
                valor = Double.parseDouble(txtValorComision.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                ToastNotification.showWarning(this, "Atención", "El valor de la comisión debe ser un número");
                return;
            }

            Double precioCondicion = null;
            String precioStr = txtPrecioCondicion.getText().trim();
            if (!precioStr.isEmpty()) {
                try {
                    precioCondicion = Double.parseDouble(precioStr.replace(',', '.'));
                } catch (NumberFormatException e) {
                    ToastNotification.showWarning(this, "Atención", "El precio condición debe ser un número");
                    return;
                }
            }

            ReglaComisionDetallada r = reglaActual != null ? reglaActual : new ReglaComisionDetallada();
            r.setDescripcion(desc);
            
            TrabajadoraWrapper tw = (TrabajadoraWrapper) cbTrabajadora.getSelectedItem();
            r.setTrabajadoraId(tw.getId());

            CategoriaWrapper cw = (CategoriaWrapper) cbCategoria.getSelectedItem();
            r.setCategoriaServicio(cw.getNombre());

            ServicioWrapper sw = (ServicioWrapper) cbServicio.getSelectedItem();
            r.setServicioId(sw.getId());

            int idxProducto = cbClienteTraeProducto.getSelectedIndex();
            if (idxProducto == 0) r.setClienteTraeProducto(null);
            else if (idxProducto == 1) r.setClienteTraeProducto(true);
            else r.setClienteTraeProducto(false);

            r.setTipoComision((String) cbTipoComision.getSelectedItem());
            r.setValorComision(valor);
            r.setPrecioCondicion(precioCondicion);
            r.setPrioridad((Integer) spinPrioridad.getValue());
            r.setActivo(chkActivo.isSelected());

            if (reglaActual == null) {
                repository.create(r);
                ToastNotification.showSuccess(this, "Regla Creada", "La regla se ha creado exitosamente");
            } else {
                repository.update(r);
                ToastNotification.showSuccess(this, "Regla Actualizada", "Los datos se han actualizado correctamente");
            }

            if (callback != null) callback.onSuccess(r);
            getController().closeModal();

        } catch (DatabaseException e) {
            ToastNotification.showError(this, "Error de BD", e.getMessage());
        } finally {
            btnGuardar.setEnabled(true);
        }
    }

    private static class TrabajadoraWrapper {
        private final Trabajadora trabajadora;
        public TrabajadoraWrapper(Trabajadora trabajadora) { this.trabajadora = trabajadora; }
        public Integer getId() { return trabajadora == null ? null : trabajadora.getId(); }
        @Override public String toString() { return trabajadora == null ? "Todas" : trabajadora.getNombreCompleto(); }
    }

    private static class CategoriaWrapper {
        private final CategoriaServicio cat;
        public CategoriaWrapper(CategoriaServicio cat) { this.cat = cat; }
        public String getNombre() { return cat == null ? null : cat.name(); }
        @Override public String toString() { return cat == null ? "Todas" : cat.name(); }
    }

    private static class ServicioWrapper {
        private final Servicio serv;
        public ServicioWrapper(Servicio serv) { this.serv = serv; }
        public Integer getId() { return serv == null ? null : serv.getId(); }
        @Override public String toString() { return serv == null ? "Todos" : serv.getNombre(); }
    }

    public interface ComisionCallback {
        void onSuccess(ReglaComisionDetallada regla);
    }
}
