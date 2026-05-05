package app.view;

import app.exception.DatabaseException;
import app.exception.trabajadora.TrabajadoraNotFoundException;
import app.model.Trabajadora;
import app.option.ModalOption;
import app.repository.TrabajadoraRepository;
import app.repository.TrabajadoraRepositorySQLite;
import app.system.ModalManager;
import app.util.ToastNotification;
import app.view.modals.TrabajadoraModal;
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

public class TrabajadorasView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(TrabajadorasView.class);

    private final TrabajadoraRepository repository;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Trabajadora> listaCache;
    private JTextField txtSearch;
    private Timer searchTimer;

    public TrabajadorasView() {
        logger.info("Inicializando TrabajadorasView");
        this.repository = new TrabajadoraRepositorySQLite();
        init();
        loadData();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

        JPanel toolbar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]5[]5[]5[]"));

        JLabel title = new JLabel("Gestión de Trabajadoras");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre o cédula...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) { scheduleSearch(); }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = createToolButton("icons/add.svg", "Nueva Trabajadora", "$Component.accentColor", "#fff");
        cmdAdd.addActionListener(e -> showModal(null));
        toolbar.add(cmdAdd);

        JButton cmdEdit = createToolButton("icons/edit.svg", "Editar Selección", null, null);
        cmdEdit.addActionListener(e -> editSelected());
        toolbar.add(cmdEdit);

        JButton cmdDel = createToolButton("icons/delete.svg", "Eliminar Selección", "$Error.color", "#fff");
        cmdDel.addActionListener(e -> deleteSelected());
        toolbar.add(cmdDel);

        add(toolbar, "growx, wrap");

        String[] columns = {"ID", "Cédula", "Nombres", "Apellidos", "Teléfono", "Bono Activo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEdit = new JMenuItem("Editar Trabajadora");
        itemEdit.addActionListener(e -> editSelected());
        JMenuItem itemDelete = new JMenuItem("Eliminar Trabajadora");
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
        btn.setIcon(new FlatSVGIcon(iconPath, 20, 20));
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
            logger.info("✓ Cargadas {} trabajadoras", listaCache.size());
        } catch (DatabaseException e) {
            logger.error("Error cargando trabajadoras: {}", e.getMessage(), e);
            ToastNotification.showError(this, "Error al Cargar", "No se pudieron cargar las trabajadoras: " + e.getMessage());
            listaCache = List.of();
        }
    }

    private void filterData(String query) {
        tableModel.setRowCount(0);
        if (listaCache == null || listaCache.isEmpty()) return;

        String q = query.toLowerCase().trim();
        for (Trabajadora t : listaCache) {
            if (q.isEmpty()
                || t.getNombreCompleto().toLowerCase().contains(q)
                || (t.getCedula() != null && t.getCedula().toLowerCase().contains(q))) {

                tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getCedula(),
                    t.getNombres(),
                    t.getApellidos(),
                    t.getTelefono(),
                    t.isBonoActivo() ? "Sí ($" + String.format("%.2f", t.getMontoBono()) + ")" : "No"
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

    private void showModal(Trabajadora trabajadoraEditar) {
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

        TrabajadoraModal modal = new TrabajadoraModal(trabajadoraEditar, t -> {
            logger.info("Callback: Trabajadora guardada exitosamente");
            loadData();
        });

        ModalManager.showModal(this, modal, option);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione una trabajadora de la tabla para eliminar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        String nombre = table.getValueAt(row, 2) + " " + table.getValueAt(row, 3);
        String cedula = (String) table.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Está seguro de eliminar a %s (Cédula: %s)?\n\nEsta acción no se puede deshacer.", nombre, cedula),
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                repository.delete(id);
                ToastNotification.showSuccess(this, "Trabajadora Eliminada", "La trabajadora ha sido eliminada correctamente");
                loadData();
            } catch (TrabajadoraNotFoundException e) {
                ToastNotification.showWarning(this, "No Encontrada", "La trabajadora ya no existe. Recargando...");
                loadData();
            } catch (DatabaseException e) {
                ToastNotification.showError(this, "Error al Eliminar", e.getMessage());
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione una trabajadora de la tabla para editar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        Trabajadora seleccionada = listaCache.stream()
                .filter(t -> t.getId() == id)
                .findFirst().orElse(null);

        if (seleccionada != null) {
            showModal(seleccionada);
        } else {
            ToastNotification.showError(this, "Error", "No se encontró la trabajadora. Recargando...");
            loadData();
        }
    }
}
