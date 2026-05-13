package app.view;

import app.exception.DatabaseException;
import app.model.ReglaComisionDetallada;
import app.option.ModalOption;
import app.repository.ReglaComisionDetalladaRepository;
import app.repository.ReglaComisionDetalladaRepositorySQLite;
import app.system.ModalManager;
import app.util.ToastNotification;
import app.view.modals.ComisionModal;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.text.DecimalFormat;

public class ComisionesView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ComisionesView.class);

    private final ReglaComisionDetalladaRepository repository;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<ReglaComisionDetallada> listaCache;
    private JTextField txtSearch;
    private Timer searchTimer;
    private final DecimalFormat df = new DecimalFormat("0.##");

    public ComisionesView() {
        logger.info("Inicializando ComisionesView (Detalladas)");
        this.repository = new ReglaComisionDetalladaRepositorySQLite();
        init();
        loadData();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

        JPanel toolbar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]5[]5[]5[]"));

        JLabel title = new JLabel("Configuración de Comisiones");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por descripción...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) { scheduleSearch(); }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = createToolButton("icons/add.svg", "Nueva Regla", "$Component.accentColor", "#fff");
        cmdAdd.addActionListener(e -> showModal(null));
        toolbar.add(cmdAdd);

        JButton cmdEdit = createToolButton("icons/edit.svg", "Editar Selección", null, null);
        cmdEdit.addActionListener(e -> editSelected());
        toolbar.add(cmdEdit);

        JButton cmdDel = createToolButton("icons/delete.svg", "Eliminar Selección", "$Error.color", "#fff");
        cmdDel.addActionListener(e -> deleteSelected());
        toolbar.add(cmdDel);

        add(toolbar, "growx, wrap");

        String[] columns = {"ID", "Descripción", "Trabajadora", "Servicio/Categoría", "Tipo", "Valor", "Prioridad", "Activo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEdit = new JMenuItem("Editar Regla");
        itemEdit.addActionListener(e -> editSelected());
        JMenuItem itemDelete = new JMenuItem("Eliminar Regla");
        itemDelete.addActionListener(e -> deleteSelected());
        popup.add(itemEdit);
        popup.add(itemDelete);
        table.setComponentPopupMenu(popup);

        JScrollPane scroll = new JScrollPane(table);
        scroll.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");
        add(scroll, "grow");
    }

    private JButton createToolButton(String iconPath, String tooltip, String bgColor, String fgColor) {
        JButton btn = new JButton();
        try {
            btn.setIcon(new FlatSVGIcon(iconPath, 20, 20));
        } catch (Exception e) {
            btn.setText("+");
        }
        btn.setToolTipText(tooltip);
        String style = "arc:10; margin:5,10,5,10;";
        if (bgColor != null) style += "background:" + bgColor + ";";
        if (fgColor != null) style += "foreground:" + fgColor + ";";
        btn.putClientProperty(FlatClientProperties.STYLE, style);
        return btn;
    }

    private void loadData() {
        try {
            listaCache = repository.findAll();
            filterData("");
            logger.info("✓ Cargadas {} reglas de comisión detalladas", listaCache.size());
        } catch (DatabaseException e) {
            logger.error("Error cargando reglas de comisión: {}", e.getMessage(), e);
            ToastNotification.showError(this, "Error al Cargar", "No se pudieron cargar las reglas: " + e.getMessage());
            listaCache = List.of();
        }
    }

    private void filterData(String query) {
        tableModel.setRowCount(0);
        if (listaCache == null || listaCache.isEmpty()) return;

        String q = query.toLowerCase().trim();
        for (ReglaComisionDetallada r : listaCache) {
            String desc = r.getDescripcion() != null ? r.getDescripcion() : "";
            if (q.isEmpty() || desc.toLowerCase().contains(q)) {
                
                String trabajadora = r.getTrabajadoraId() != null ? r.getNombreTrabajadora() : "Todas";
                String aplicacion = "";
                if (r.getServicioId() != null) aplicacion = "Srv: " + r.getNombreServicio();
                else if (r.getCategoriaServicio() != null) aplicacion = "Cat: " + r.getCategoriaServicio();
                else aplicacion = "Todos";
                
                String valor = r.getTipoComision().equals("PORCENTAJE") ? 
                        df.format(r.getValorComision() * 100) + "%" : 
                        "$" + df.format(r.getValorComision());

                tableModel.addRow(new Object[]{
                    r.getId(),
                    desc,
                    trabajadora,
                    aplicacion,
                    r.getTipoComision(),
                    valor,
                    r.getPrioridad(),
                    r.isActivo() ? "Sí" : "No"
                });
            }
        }
    }

    private void scheduleSearch() {
        if (searchTimer != null) searchTimer.stop();
        searchTimer = new Timer(300, e -> filterData(txtSearch.getText()));
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    private void showModal(ReglaComisionDetallada reglaEditar) {
        ModalOption option = ModalOption.getDefault()
                .setHorizontalPosition(ModalOption.Position.RIGHT)
                .setVerticalPosition(ModalOption.Position.CENTER)
                .setAnimationDirection(ModalOption.AnimationDirection.RIGHT_TO_LEFT)
                .setAnimationEnabled(true)
                .setDuration(350)
                .setMargin(0)
                .setPadding(0)
                .setRoundness(0)
                .setCloseOnEscape(true)
                .setCloseOnClickOutside(false);

        ComisionModal modal = new ComisionModal(reglaEditar, r -> {
            logger.info("Callback: Regla guardada exitosamente");
            loadData();
        });

        ModalManager.showModal(this, modal, option);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione una regla de la tabla para eliminar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        String desc = (String) table.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Está seguro de eliminar la regla '%s'?\n\nEsta acción no se puede deshacer.", desc),
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                repository.delete(id);
                ToastNotification.showSuccess(this, "Regla Eliminada", "La regla de comisión ha sido eliminada correctamente");
                loadData();
            } catch (DatabaseException e) {
                ToastNotification.showError(this, "Error al Eliminar", e.getMessage());
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione una regla de la tabla para editar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        ReglaComisionDetallada seleccionada = listaCache.stream()
                .filter(r -> r.getId() == id)
                .findFirst().orElse(null);

        if (seleccionada != null) {
            showModal(seleccionada);
        } else {
            ToastNotification.showError(this, "Error", "No se encontró la regla. Recargando...");
            loadData();
        }
    }
}
