package app.view.modals;

import app.component.Modal;
import app.exception.DatabaseException;
import app.model.CategoriaServicio;
import app.model.ReglaComision;
import app.model.Trabajadora;
import app.repository.ReglaComisionRepository;
import app.repository.ReglaComisionRepositorySQLite;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
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

    private final ReglaComisionRepository repository;
    private final TrabajadoraRepository trabajadoraRepository;
    private final ComisionCallback callback;
    private ReglaComision reglaActual;

    private JComboBox<TrabajadoraWrapper> cbTrabajadora;
    private JComboBox<CategoriaServicio> cbCategoria;
    private JTextField txtTasa;
    private JButton btnGuardar;

    public ComisionModal(ReglaComision reglaActual, ComisionCallback callback) {
        this.reglaActual = reglaActual;
        this.callback = callback;
        this.trabajadoraRepository = new TrabajadoraRepositorySQLite();
        this.repository = new ReglaComisionRepositorySQLite(this.trabajadoraRepository);
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(500, 350));
        putClientProperty(FlatClientProperties.STYLE, "arc:15");

        JPanel mainPanel = new JPanel(new MigLayout("fillx,wrap,insets 25 30 25 30", "[grow,fill]", "[]15[]"));
        mainPanel.setOpaque(false);

        createHeader(mainPanel);
        createForm(mainPanel);
        createButtons(mainPanel);

        add(mainPanel, BorderLayout.CENTER);

        if (reglaActual != null) {
            loadData();
        }
    }

    private void createHeader(JPanel parent) {
        JPanel headerPanel = new JPanel(new MigLayout("insets 0", "[]push[]", "[]"));
        headerPanel.setOpaque(false);

        String title = reglaActual == null ? "Nueva Regla de Comisión" : "Editar Regla de Comisión";
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
        cbTrabajadora = new JComboBox<>();
        try {
            List<Trabajadora> trabajadoras = trabajadoraRepository.findAll();
            for (Trabajadora t : trabajadoras) {
                cbTrabajadora.addItem(new TrabajadoraWrapper(t));
            }
        } catch (Exception e) {
            logger.error("Error al cargar trabajadoras", e);
        }
        parent.add(createFieldPanel("Trabajadora:", cbTrabajadora));

        cbCategoria = new JComboBox<>(CategoriaServicio.values());
        parent.add(createFieldPanel("Categoría de Servicio:", cbCategoria));

        txtTasa = new JTextField();
        txtTasa.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej: 0.70 para 70%");
        txtTasa.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        parent.add(createFieldPanel("Tasa de Comisión:", txtTasa));
        
        JLabel helpLabel = new JLabel("La tasa debe ser un número decimal entre 0.0 y 1.0 (Ej: 0.50 para 50%)");
        helpLabel.putClientProperty(FlatClientProperties.STYLE, "font:-2;foreground:$Label.disabledForeground");
        parent.add(helpLabel, "gapleft 130");
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
        JPanel panel = new JPanel(new MigLayout("insets 0", "[120]10[grow,fill]", "[]"));
        panel.setOpaque(false);
        panel.add(new JLabel(label));
        panel.add(field);
        return panel;
    }

    private void loadData() {
        for (int i = 0; i < cbTrabajadora.getItemCount(); i++) {
            if (cbTrabajadora.getItemAt(i).getId() == reglaActual.getTrabajadoraId()) {
                cbTrabajadora.setSelectedIndex(i);
                break;
            }
        }
        
        for (int i = 0; i < cbCategoria.getItemCount(); i++) {
            if (cbCategoria.getItemAt(i).name().equals(reglaActual.getCategoriaServicio())) {
                cbCategoria.setSelectedIndex(i);
                break;
            }
        }
        
        txtTasa.setText(String.valueOf(reglaActual.getPorcentajeComision()));
        
        // No se puede cambiar la trabajadora ni categoría al editar porque es única
        cbTrabajadora.setEnabled(false);
        cbCategoria.setEnabled(false);
    }

    private void guardar() {
        btnGuardar.setEnabled(false);
        try {
            TrabajadoraWrapper seleccion = (TrabajadoraWrapper) cbTrabajadora.getSelectedItem();
            CategoriaServicio categoria = (CategoriaServicio) cbCategoria.getSelectedItem();
            
            if (seleccion == null) {
                ToastNotification.showWarning(this, "Atención", "Debe seleccionar una trabajadora");
                return;
            }
            if (categoria == null) {
                ToastNotification.showWarning(this, "Atención", "Debe seleccionar una categoría");
                return;
            }

            double tasa;
            try {
                String tasaStr = txtTasa.getText().trim().replace(',', '.');
                tasa = Double.parseDouble(tasaStr);
                if (tasa < 0.0 || tasa > 1.0) {
                    ToastNotification.showWarning(this, "Atención", "La tasa debe estar entre 0.0 y 1.0");
                    return;
                }
            } catch (NumberFormatException e) {
                ToastNotification.showWarning(this, "Atención", "La tasa de comisión debe ser un número válido");
                return;
            }

            ReglaComision r = reglaActual != null ? reglaActual : new ReglaComision();
            r.setTrabajadoraId(seleccion.getId());
            r.setCategoriaServicio(categoria.name());
            r.setPorcentajeComision(tasa);

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
            ToastNotification.showError(this, "Error de Base de Datos", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado guardando regla", e);
            ToastNotification.showError(this, "Error Inesperado", "Ha ocurrido un error inesperado.");
        } finally {
            btnGuardar.setEnabled(true);
        }
    }

    private static class TrabajadoraWrapper {
        private final Trabajadora trabajadora;

        public TrabajadoraWrapper(Trabajadora trabajadora) {
            this.trabajadora = trabajadora;
        }

        public int getId() {
            return trabajadora.getId();
        }

        @Override
        public String toString() {
            return trabajadora.getNombreCompleto() + " (" + trabajadora.getCedula() + ")";
        }
    }

    public interface ComisionCallback {
        void onSuccess(ReglaComision regla);
    }
}
