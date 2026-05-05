package app.view;

import app.exception.DatabaseException;
import app.exception.servicio.ServicioNotFoundException;
import app.model.Servicio;
import app.option.ModalOption;
import app.repository.ServicioRepository;
import app.repository.ServicioRepositorySQLite;
import app.system.ModalManager;
import app.util.ToastNotification;
import app.view.modals.ServicioModal;
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

public class ServiciosView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ServiciosView.class);

    private final ServicioRepository repository;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Servicio> listaCache;
    private JTextField txtSearch;
    private Timer searchTimer;

    public ServiciosView() {
        logger.info("Inicializando ServiciosView");
        this.repository = new ServicioRepositorySQLite();
        init();
        loadData();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

        JPanel toolbar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]5[]5[]5[]"));

        JLabel title = new JLabel("Catálogo de Servicios");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar servicio...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) { scheduleSearch(); }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = createToolButton("icons/add.svg", "Nuevo Servicio", "$Component.accentColor", "#fff");
        cmdAdd.addActionListener(e -> showModal(null));
        toolbar.add(cmdAdd);

        JButton cmdEdit = createToolButton("icons/edit.svg", "Editar Selección", null, null);
        cmdEdit.addActionListener(e -> editSelected());
        toolbar.add(cmdEdit);

        JButton cmdDel = createToolButton("icons/delete.svg", "Eliminar Selección", "$Error.color", "#fff");
        cmdDel.addActionListener(e -> deleteSelected());
        toolbar.add(cmdDel);

        add(toolbar, "growx, wrap");

        String[] columns = {"ID", "Nombre", "Categoría", "Corto", "Mediano", "Largo", "Extensiones"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEdit = new JMenuItem("Editar Servicio");
        itemEdit.addActionListener(e -> editSelected());
        JMenuItem itemDelete = new JMenuItem("Eliminar Servicio");
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
            logger.info("✓ Cargados {} servicios", listaCache.size());
        } catch (DatabaseException e) {
            logger.error("Error cargando servicios: {}", e.getMessage(), e);
            ToastNotification.showError(this, "Error al Cargar", e.getMessage());
            listaCache = List.of();
        }
    }

    private void filterData(String query) {
        tableModel.setRowCount(0);
        if (listaCache == null || listaCache.isEmpty()) return;

        String q = query.toLowerCase().trim();
        for (Servicio s : listaCache) {
            if (q.isEmpty() || s.getNombre().toLowerCase().contains(q)) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getNombre(),
                    s.getCategoria() != null ? s.getCategoria().toString() : "",
                    String.format("$%.2f", s.getPrecioCorto()),
                    String.format("$%.2f", s.getPrecioMediano()),
                    String.format("$%.2f", s.getPrecioLargo()),
                    String.format("$%.2f", s.getPrecioExtensiones())
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

    private void showModal(Servicio servicioEditar) {
        ModalOption option = ModalOption.getDefault()
                .setHorizontalPosition(ModalOption.Position.RIGHT)
                .setVerticalPosition(ModalOption.Position.CENTER)
                .setAnimationDirection(ModalOption.AnimationDirection.RIGHT_TO_LEFT)
                .setAnimationEnabled(true)
                .setDuration(350)
                .setMargin(0).setPadding(0).setRoundness(0)
                .setCloseOnEscape(true)
                .setCloseOnClickOutside(false);

        ServicioModal modal = new ServicioModal(servicioEditar, s -> {
            logger.info("Callback: Servicio guardado");
            loadData();
        });

        ModalManager.showModal(this, modal, option);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione un servicio para eliminar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        String nombre = (String) table.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Desactivar el servicio '%s'?\n\nEl servicio no se eliminará permanentemente.", nombre),
                "Confirmar Desactivación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                repository.delete(id);
                ToastNotification.showSuccess(this, "Servicio Desactivado", "El servicio ha sido desactivado");
                loadData();
            } catch (ServicioNotFoundException e) {
                ToastNotification.showWarning(this, "No Encontrado", "El servicio ya no existe.");
                loadData();
            } catch (DatabaseException e) {
                ToastNotification.showError(this, "Error", e.getMessage());
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.showInfo(this, "Seleccione un servicio para editar");
            return;
        }

        int id = (int) table.getValueAt(row, 0);
        Servicio sel = listaCache.stream().filter(s -> s.getId() == id).findFirst().orElse(null);

        if (sel != null) {
            showModal(sel);
        } else {
            ToastNotification.showError(this, "Error", "No se encontró el servicio. Recargando...");
            loadData();
        }
    }
}
