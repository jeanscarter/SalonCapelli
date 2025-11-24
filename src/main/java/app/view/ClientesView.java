package app.view;

import app.model.Cliente;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import app.view.forms.ClienteForm;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Component;
import java.awt.Window;
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
        JPanel toolbar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]5[]5[]5[]"));
        
        JLabel title = new JLabel("Gestión de Clientes");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                filterData(txtSearch.getText());
            }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = createToolButton("icons/add.svg", "Nuevo Cliente", "$Component.accentColor", "#fff");
        cmdAdd.addActionListener(e -> showClienteForm(null));
        toolbar.add(cmdAdd);
        
        JButton cmdEdit = createToolButton("icons/edit.svg", "Editar Selección", null, null);
        cmdEdit.addActionListener(e -> editSelected());
        toolbar.add(cmdEdit);

        JButton cmdDel = createToolButton("icons/delete.svg", "Eliminar Selección", "$Error.color", "#fff");
        cmdDel.addActionListener(e -> deleteSelected());
        toolbar.add(cmdDel);

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

    private void showClienteForm(Cliente clienteEditar) {
        SwingUtilities.invokeLater(() -> {
            try {
                ClienteForm form = new ClienteForm();
                if (clienteEditar != null) form.loadData(clienteEditar);

                SimpleModalBorder.Option[] options = {
                    new SimpleModalBorder.Option("Guardar", SimpleModalBorder.YES_OPTION),
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION)
                };

                Window window = SwingUtilities.getWindowAncestor(this);
                if (window == null) return; 

                ModalDialog.showModal(window, new SimpleModalBorder(
                        form, 
                        (clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente"), 
                        options,
                        (controller, action) -> {
                            if (action == SimpleModalBorder.YES_OPTION) {
                                try {
                                    Cliente c = form.getData();
                                    if (clienteEditar == null) {
                                        repository.create(c);
                                        Toast.show(window, Toast.Type.SUCCESS, "Cliente registrado");
                                    } else {
                                        c.setId(clienteEditar.getId());
                                        repository.update(c);
                                        Toast.show(window, Toast.Type.SUCCESS, "Datos actualizados");
                                    }
                                    loadData();
                                    controller.close();
                                } catch (Exception ex) {
                                    Toast.show(window, Toast.Type.WARNING, ex.getMessage());
                                }
                            } else {
                                controller.close();
                            }
                        }
                ));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente");
            return;
        }
        int id = (int) table.getValueAt(row, 0);
        String nombre = (String) table.getValueAt(row, 2);
        
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window == null) return;

            JLabel msgLabel = new JLabel("<html>¿Estás seguro de eliminar a <b>" + nombre + "</b>?</html>");
            ModalDialog.showModal(window, new SimpleModalBorder(
                    msgLabel, "Confirmar Eliminación", SimpleModalBorder.YES_NO_OPTION,
                    (controller, action) -> {
                        if (action == SimpleModalBorder.YES_OPTION) {
                            try {
                                repository.delete(id);
                                Toast.show(window, Toast.Type.SUCCESS, "Cliente eliminado");
                                loadData();
                            } catch (Exception e) {
                                Toast.show(window, Toast.Type.ERROR, "Error: " + e.getMessage());
                            }
                        }
                        controller.close();
                    }
            ));
        });
    }
    
    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente");
            return;
        }
        int id = (int) table.getValueAt(row, 0); 
        Cliente seleccionado = listaClientesCache.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
        if (seleccionado != null) showClienteForm(seleccionado);
    }
}