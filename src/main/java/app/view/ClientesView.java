package app.view;

import app.model.Cliente;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import app.view.forms.ClienteForm;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;

public class ClientesView extends JPanel {

    private final ClienteRepository repository;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Cliente> listaClientesCache; 
    private JTextField txtSearch;

    public ClientesView() {
        this.repository = new ClienteRepositorySQLite();
        init();
        loadData(); 
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

        // --- Toolbar ---
        JPanel toolbar = new JPanel(new MigLayout("insets 0", "[]push[][]"));
        
        JLabel title = new JLabel("Gestión de Clientes");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                filterData(txtSearch.getText());
            }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = new JButton("Nuevo Cliente");
        cmdAdd.putClientProperty(FlatClientProperties.STYLE, "background:$Component.accentColor; foreground:#fff; font:bold");
        cmdAdd.addActionListener(e -> showClienteForm(null));
        toolbar.add(cmdAdd);

        add(toolbar, "growx, wrap");

        // --- Tabla ---
        String[] columnNames = {"ID", "Cédula", "Nombre", "Teléfono", "Tipo Cabello", "Extensiones"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(40); 
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");
        
        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEdit = new JMenuItem("Editar Cliente");
        itemEdit.addActionListener(e -> editSelected());
        JMenuItem itemDelete = new JMenuItem("Eliminar Cliente");
        itemDelete.addActionListener(e -> deleteSelected());
        
        popup.add(itemEdit);
        popup.add(itemDelete);
        table.setComponentPopupMenu(popup);

        JScrollPane scroll = new JScrollPane(table);
        scroll.putClientProperty(FlatClientProperties.STYLE, "border:0,0,0,0");
        add(scroll, "grow");
        
        JPanel footer = new JPanel(new MigLayout("insets 10 0 0 0", "push[]10[]"));
        JButton btnEdit = new JButton("Editar Selección");
        btnEdit.addActionListener(e -> editSelected());
        JButton btnDel = new JButton("Eliminar Selección");
        btnDel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Error.color; hoverBackground:$Error.color; hoverForeground:#fff");
        btnDel.addActionListener(e -> deleteSelected());
        
        footer.add(btnEdit);
        footer.add(btnDel);
        add(footer, "growx");
    }

    private void loadData() {
        try {
            listaClientesCache = repository.findAll();
            filterData(""); 
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error DB: " + e.getMessage());
        }
    }

    private void filterData(String query) {
        tableModel.setRowCount(0); 
        if (listaClientesCache == null) return;

        String q = query.toLowerCase();
        for (Cliente c : listaClientesCache) {
            if (c.getNombreCompleto().toLowerCase().contains(q) || 
                (c.getCedula() != null && c.getCedula().contains(q))) {
                tableModel.addRow(new Object[]{
                    c.getId(), c.getCedula(), c.getNombreCompleto(), c.getTelefono(), c.getTipoCabello(), c.getTipoExtensiones()
                });
            }
        }
    }

    // --- MÉTODOS CORREGIDOS (ModalDialog.showModal) ---

    private void showClienteForm(Cliente clienteEditar) {
        ClienteForm form = new ClienteForm();
        if (clienteEditar != null) {
            form.loadData(clienteEditar);
        }

        SimpleModalBorder.Option[] options = {
            new SimpleModalBorder.Option("Guardar", SimpleModalBorder.YES_OPTION),
            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION)
        };

        // CORRECCIÓN: Usamos showModal(this, ...) en lugar de showSimpleModal
        ModalDialog.showModal(this, new SimpleModalBorder(
                form, 
                (clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente"), 
                options,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        try {
                            Cliente c = form.getData();
                            if (clienteEditar == null) {
                                repository.create(c);
                                Toast.show(this, Toast.Type.SUCCESS, "Cliente registrado");
                            } else {
                                c.setId(clienteEditar.getId());
                                repository.update(c);
                                Toast.show(this, Toast.Type.SUCCESS, "Datos actualizados");
                            }
                            loadData();
                            controller.close();
                        } catch (Exception ex) {
                            Toast.show(this, Toast.Type.WARNING, ex.getMessage());
                        }
                    } else {
                        controller.close();
                    }
                }
        ));
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String nombre = (String) table.getValueAt(row, 2);

        JLabel msgLabel = new JLabel("<html>¿Estás seguro de eliminar a <b>" + nombre + "</b>?<br>Esta acción no se puede deshacer.</html>");
        
        // CORRECCIÓN: Usamos showModal(this, ...)
        ModalDialog.showModal(this, new SimpleModalBorder(
                msgLabel, 
                "Confirmar Eliminación",
                SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        try {
                            repository.delete(id);
                            Toast.show(this, Toast.Type.SUCCESS, "Cliente eliminado");
                            loadData();
                        } catch (Exception e) {
                            Toast.show(this, Toast.Type.ERROR, "Error: " + e.getMessage());
                        }
                        controller.close();
                    } else {
                        controller.close();
                    }
                }
        ));
    }
    
    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente");
            return;
        }
        int id = (int) table.getValueAt(row, 0); 
        Cliente seleccionado = listaClientesCache.stream()
                .filter(c -> c.getId() == id).findFirst().orElse(null);
        if (seleccionado != null) showClienteForm(seleccionado);
    }
}