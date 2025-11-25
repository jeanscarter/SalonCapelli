package app.view;

import app.exception.DatabaseException;
import app.exception.cliente.ClienteException;
import app.exception.cliente.ClienteNotFoundException;
import app.model.Cliente;
import app.option.ModalOption;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import app.system.ModalManager;
import app.view.modals.ClienteModal;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raven.modal.Toast;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Vista principal de gestión de clientes con manejo robusto de excepciones
 * Patrón: MVC + Observer
 */
public class ClientesView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ClientesView.class);
    
    private final ClienteRepository repository;
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Cliente> listaClientesCache; 
    private JTextField txtSearch;
    private Timer searchTimer; // Para debouncing

    public ClientesView() {
        logger.info("Inicializando ClientesView");
        this.repository = new ClienteRepositorySQLite();
        init();
        loadData(); 
        logger.debug("✓ ClientesView inicializada");
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20", "[grow]", "[][grow]"));

        // --- Toolbar ---
        JPanel toolbar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]5[]5[]5[]"));
        
        JLabel title = new JLabel("Gestión de Clientes");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        toolbar.add(title);

        // Campo de búsqueda con debouncing
        txtSearch = new JTextField(20);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar por nombre o cédula...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                scheduleSearch();
            }
        });
        toolbar.add(txtSearch);

        JButton cmdAdd = createToolButton("icons/add.svg", "Nuevo Cliente", "$Component.accentColor", "#fff");
        cmdAdd.addActionListener(e -> showClienteModal(null));
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
        
        // Menú contextual
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
        
        logger.debug("✓ Componentes UI creados");
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

    /**
     * Carga todos los clientes desde la BD
     */
    private void loadData() {
        logger.info("Cargando lista de clientes");
        
        try {
            listaClientesCache = repository.findAll();
            filterData(""); 
            
            logger.info("✓ Cargados {} clientes", listaClientesCache.size());
            // Toast eliminado - el usuario verá los datos en la tabla
            
        } catch (DatabaseException e) {
            logger.error("Error cargando clientes: {}", e.getMessage(), e);
            showToastSafe(Toast.Type.ERROR, "Error al cargar clientes: " + e.getMessage());
            listaClientesCache = List.of(); // Lista vacía para evitar NullPointer
        }
    }

    /**
     * Filtra los clientes por query (búsqueda local en caché)
     */
    private void filterData(String query) {
        logger.debug("Filtrando clientes con query: '{}'", query);
        
        tableModel.setRowCount(0); 
        
        if (listaClientesCache == null || listaClientesCache.isEmpty()) {
            logger.debug("Cache vacío, no hay datos para filtrar");
            return;
        }
        
        String q = query.toLowerCase().trim();
        int count = 0;
        
        for (Cliente c : listaClientesCache) {
            if (q.isEmpty() || 
                c.getNombreCompleto().toLowerCase().contains(q) || 
                (c.getCedula() != null && c.getCedula().toLowerCase().contains(q))) {
                
                tableModel.addRow(new Object[]{
                    c.getId(), 
                    c.getCedula(), 
                    c.getNombreCompleto(), 
                    c.getTelefono(), 
                    c.getTipoCabello(), 
                    c.getTipoExtensiones()
                });
                count++;
            }
        }
        
        logger.debug("✓ Filtrados {} clientes", count);
    }
    
    /**
     * Programa una búsqueda con debouncing (300ms después de dejar de escribir)
     */
    private void scheduleSearch() {
        if (searchTimer != null) {
            searchTimer.stop();
        }
        
        searchTimer = new Timer(300, e -> {
            String query = txtSearch.getText();
            logger.debug("Ejecutando búsqueda programada: '{}'", query);
            filterData(query);
        });
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    /**
     * Muestra el modal de cliente usando el nuevo sistema
     */
    private void showClienteModal(Cliente clienteEditar) {
        String accion = clienteEditar == null ? "crear" : "editar";
        logger.info("Abriendo modal para {} cliente", accion);
        
        // Crear opciones del modal con animación de derecha a izquierda
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
        
        // Crear el modal con callback
        ClienteModal modal = new ClienteModal(clienteEditar, cliente -> {
            logger.info("Callback: Cliente guardado exitosamente");
            loadData();
            
            String mensaje = clienteEditar == null ? 
                "Cliente registrado exitosamente" : 
                "Cliente actualizado exitosamente";
            showToastSafe(Toast.Type.SUCCESS, mensaje);
        });
        
        // Mostrar el modal
        ModalManager.showModal(this, modal, option);
    }

    /**
     * Elimina el cliente seleccionado con confirmación
     */
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            logger.debug("Intento de eliminar sin selección");
            showToastSafe(Toast.Type.INFO, "Selecciona un cliente de la tabla");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0);
        String nombre = (String) table.getValueAt(row, 2);
        String cedula = (String) table.getValueAt(row, 1);
        
        logger.info("Solicitando confirmación para eliminar cliente ID: {} - {}", id, nombre);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            String.format("¿Estás seguro de eliminar a %s (Cédula: %s)?\n\nEsta acción no se puede deshacer.", 
                nombre, cedula),
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            logger.info("Usuario confirmó eliminación");
            
            try {
                repository.delete(id);
                
                logger.info("✓ Cliente eliminado: ID {} - {}", id, nombre);
                showToastSafe(Toast.Type.SUCCESS, "Cliente eliminado correctamente");
                loadData();
                
            } catch (ClienteNotFoundException e) {
                logger.warn("Cliente no encontrado al intentar eliminar: {}", e.getMessage());
                showToastSafe(Toast.Type.WARNING, 
                    "El cliente ya no existe. Recargando lista...");
                loadData();
                
            } catch (DatabaseException e) {
                logger.error("Error al eliminar cliente: {}", e.getMessage(), e);
                showToastSafe(Toast.Type.ERROR, 
                    "Error al eliminar: " + e.getMessage());
                
            } catch (Exception e) {
                logger.error("Error inesperado al eliminar cliente", e);
                showToastSafe(Toast.Type.ERROR, 
                    "Error inesperado: " + e.getMessage());
            }
        } else {
            logger.debug("Usuario canceló la eliminación");
        }
    }
    
    /**
     * Edita el cliente seleccionado
     */
    private void editSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            logger.debug("Intento de editar sin selección");
            showToastSafe(Toast.Type.INFO, "Selecciona un cliente de la tabla");
            return;
        }
        
        int id = (int) table.getValueAt(row, 0); 
        logger.info("Editando cliente ID: {}", id);
        
        Cliente seleccionado = listaClientesCache.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
        
        if (seleccionado != null) {
            showClienteModal(seleccionado);
        } else {
            logger.error("Cliente ID {} no encontrado en caché", id);
            showToastSafe(Toast.Type.ERROR, 
                "Error: Cliente no encontrado. Recargando lista...");
            loadData();
        }
    }
    
    /**
     * Muestra un toast de forma segura (verifica que el componente esté en la jerarquía)
     */
    private void showToastSafe(Toast.Type type, String message) {
        // Diferir hasta que el componente esté visible
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                Toast.show(this, type, message);
            } else {
                logger.warn("Toast no mostrado (componente no visible): {}", message);
            }
        });
    }
}