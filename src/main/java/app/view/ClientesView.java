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
        // Verificar que tenemos una ventana padre válida
        Component parentComponent = SwingUtilities.getRoot(this);
        if (parentComponent == null || !(parentComponent instanceof Window)) {
            Toast.show(this, Toast.Type.ERROR, "Error: No se puede mostrar el formulario");
            return;
        }

        // Referencia final para usar en el lambda
        final Component owner = parentComponent;

        // Crear el formulario
        ClienteForm form = new ClienteForm();
        if (clienteEditar != null) {
            form.loadData(clienteEditar);
        }

        // Crear el panel contenedor del formulario
        JPanel formPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[grow]"));
        formPanel.add(form, "grow");

        // Configurar opciones del modal
        SimpleModalBorder.Option[] options = {
            new SimpleModalBorder.Option("Guardar", SimpleModalBorder.YES_OPTION),
            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION)
        };

        // Crear el border del modal
        SimpleModalBorder modalBorder = new SimpleModalBorder(
            formPanel,
            (clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente"),
            options,
            (controller, action) -> {
                if (action == SimpleModalBorder.YES_OPTION) {
                    // Intentar guardar
                    try {
                        Cliente c = form.getData();
                        
                        if (clienteEditar == null) {
                            // Crear nuevo
                            repository.create(c);
                            Toast.show(owner, Toast.Type.SUCCESS, "Cliente registrado exitosamente");
                        } else {
                            // Actualizar existente
                            c.setId(clienteEditar.getId());
                            repository.update(c);
                            Toast.show(owner, Toast.Type.SUCCESS, "Cliente actualizado exitosamente");
                        }
                        
                        // Recargar datos
                        loadData();
                        
                        // Cerrar el modal
                        controller.close();
                        
                    } catch (Exception ex) {
                        // Mostrar error pero NO cerrar el modal
                        Toast.show(owner, Toast.Type.WARNING, ex.getMessage());
                    }
                } else {
                    // Cancelar
                    controller.close();
                }
            }
        );

        // Mostrar el modal
        ModalDialog.showModal(owner, modalBorder);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente de la tabla");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String nombre = (String) table.getValueAt(row, 2);
        
        // Obtener ventana padre
        Component parentComponent = SwingUtilities.getRoot(this);
        if (parentComponent == null || !(parentComponent instanceof Window)) {
            Toast.show(this, Toast.Type.ERROR, "Error al mostrar diálogo de confirmación");
            return;
        }

        // Referencia final para usar en el lambda
        final Component owner = parentComponent;

        // Crear mensaje de confirmación
        JPanel msgPanel = new JPanel(new MigLayout("fill, insets 20", "[center]", "[]10[]"));
        JLabel icon = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        JLabel msg = new JLabel("<html><center>¿Estás seguro de eliminar a<br><b>" + nombre + "</b>?<br><br>Esta acción no se puede deshacer.</center></html>");
        msg.setHorizontalAlignment(SwingConstants.CENTER);
        msgPanel.add(icon, "wrap");
        msgPanel.add(msg, "grow");
        
        SimpleModalBorder modalBorder = new SimpleModalBorder(
            msgPanel,
            "Confirmar Eliminación",
            SimpleModalBorder.YES_NO_OPTION,
            (controller, action) -> {
                if (action == SimpleModalBorder.YES_OPTION) {
                    try {
                        repository.delete(id);
                        Toast.show(owner, Toast.Type.SUCCESS, "Cliente eliminado correctamente");
                        loadData();
                    } catch (Exception e) {
                        Toast.show(owner, Toast.Type.ERROR, "Error al eliminar: " + e.getMessage());
                    }
                }
                controller.close();
            }
        );
        
        ModalDialog.showModal(owner, modalBorder);
    }
    
    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            Toast.show(this, Toast.Type.INFO, "Selecciona un cliente de la tabla");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0); 
        Cliente seleccionado = listaClientesCache.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
        
        if (seleccionado != null) {
            showClienteForm(seleccionado);
        } else {
            Toast.show(this, Toast.Type.ERROR, "Error: No se encontró el cliente seleccionado");
        }
    }
}