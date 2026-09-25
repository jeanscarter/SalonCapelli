package app.view.modals;

import app.component.Modal;
import app.exception.DatabaseException;
import app.model.Cliente;
import app.option.ModalOption;
import app.repository.ClienteRepository;
import app.repository.ClienteRepositorySQLite;
import app.system.ModalManager;
import app.util.FastKeySelectionManager;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal interactivo para búsqueda en tiempo real y selección de clientes.
 * Permite buscar simultáneamente por Nombre, Apellido, Cédula o Teléfono.
 */
public class ClienteSelectorModal extends Modal {

    private static final Logger logger = LoggerFactory.getLogger(ClienteSelectorModal.class);

    private final ClienteRepository repository;
    private final Consumer<Cliente> onSelected;
    private final String initialQuery;

    private JTextField txtSearch;
    private JTable tableResultados;
    private DefaultTableModel tableModel;
    private JLabel lblContador;
    private Timer searchTimer;
    private List<Cliente> listaCache = new ArrayList<>();
    private List<Cliente> listaFiltrada = new ArrayList<>();

    public ClienteSelectorModal(Consumer<Cliente> onSelected) {
        this(null, onSelected);
    }

    public ClienteSelectorModal(String initialQuery, Consumer<Cliente> onSelected) {
        this.repository = new ClienteRepositorySQLite();
        this.onSelected = onSelected;
        this.initialQuery = initialQuery;
    }

    @Override
    public void installComponent() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(720, 520));
        putClientProperty(FlatClientProperties.STYLE, "arc:15");

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 20", "[grow]", "[][][grow][]"));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");

        // Header
        JPanel header = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[]"));
        header.setOpaque(false);

        JPanel titles = new JPanel(new MigLayout("insets 0, wrap 1", "[]", "[]2[]"));
        titles.setOpaque(false);
        JLabel lblTitle = new JLabel("Directorio y Búsqueda de Clientes");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +6; foreground:$Component.accentColor");
        JLabel lblSubtitle = new JLabel("Busque por nombre, apellido, cédula o teléfono y presione Enter o doble clic");
        lblSubtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        titles.add(lblTitle);
        titles.add(lblSubtitle);
        header.add(titles);

        JButton btnClose = new JButton(new FlatSVGIcon("icons/delete.svg", 0.4f));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc:999; margin:5,5,5,5; borderWidth:0; focusWidth:0; background:null");
        btnClose.addActionListener(e -> cerrarModal());
        header.add(btnClose);
        mainPanel.add(header, "growx, wrap, gapbottom 10");

        // Barra de búsqueda y botón nuevo
        JPanel searchBar = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]10[]", "[]"));
        searchBar.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Escriba nombre, apellido, cédula o teléfono para filtrar...");
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc:10; font:+1");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (tableResultados.getRowCount() > 0) {
                        tableResultados.requestFocusInWindow();
                        tableResultados.setRowSelectionInterval(0, 0);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarFilaActual();
                } else {
                    scheduleFilter();
                }
            }
        });
        searchBar.add(txtSearch);

        JButton btnNuevo = new JButton("➕ Registrar Nuevo");
        btnNuevo.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Component.accentColor; foreground:#fff; font:bold");
        btnNuevo.addActionListener(e -> abrirCreacionNuevoCliente());
        searchBar.add(btnNuevo);
        mainPanel.add(searchBar, "growx, wrap, gapbottom 10");

        // Tabla de resultados
        String[] cols = {"ID", "Cédula", "Nombre Completo", "Teléfono", "Tipo Cabello"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tableResultados = new JTable(tableModel);
        tableResultados.setRowHeight(32);
        tableResultados.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "font:bold");
        tableResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Anchos de columna
        tableResultados.getColumnModel().getColumn(0).setMinWidth(40);
        tableResultados.getColumnModel().getColumn(0).setMaxWidth(60);
        tableResultados.getColumnModel().getColumn(1).setPreferredWidth(110);
        tableResultados.getColumnModel().getColumn(2).setPreferredWidth(240);
        tableResultados.getColumnModel().getColumn(3).setPreferredWidth(120);

        // Doble clic o Enter para seleccionar
        tableResultados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    seleccionarFilaActual();
                }
            }
        });
        tableResultados.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    seleccionarFilaActual();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tableResultados);
        scroll.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        mainPanel.add(scroll, "grow, wrap, gapbottom 10");

        // Footer
        JPanel footer = new JPanel(new MigLayout("insets 0, fillx", "[]push[]10[]", "[]"));
        footer.setOpaque(false);

        lblContador = new JLabel("Cargando clientes...");
        lblContador.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");
        footer.add(lblContador);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnCancelar.addActionListener(e -> cerrarModal());
        footer.add(btnCancelar);

        JButton btnSeleccionar = new JButton("Seleccionar Cliente");
        btnSeleccionar.putClientProperty(FlatClientProperties.STYLE, "arc:8; background:$Success.color; foreground:#fff; font:bold");
        btnSeleccionar.addActionListener(e -> seleccionarFilaActual());
        footer.add(btnSeleccionar);

        mainPanel.add(footer, "growx");

        add(mainPanel, BorderLayout.CENTER);

        // Cargar clientes
        cargarDatos();

        if (initialQuery != null && !initialQuery.isBlank()) {
            txtSearch.setText(initialQuery.trim());
            scheduleFilter();
        }

        SwingUtilities.invokeLater(() -> txtSearch.requestFocusInWindow());
    }

    private void cargarDatos() {
        try {
            listaCache = repository.findAll();
            filterData(initialQuery != null ? initialQuery : "");
        } catch (DatabaseException e) {
            logger.error("Error al cargar clientes", e);
            lblContador.setText("Error al cargar lista de clientes.");
        }
    }

    private void scheduleFilter() {
        if (searchTimer != null) {
            searchTimer.stop();
        }
        searchTimer = new Timer(150, e -> filterData(txtSearch.getText()));
        searchTimer.setRepeats(false);
        searchTimer.start();
    }

    private void filterData(String rawQuery) {
        String q = normalize(rawQuery);
        tableModel.setRowCount(0);
        listaFiltrada.clear();

        for (Cliente c : listaCache) {
            if (q.isEmpty() ||
                normalize(c.getNombreCompleto()).contains(q) ||
                (c.getCedula() != null && normalize(c.getCedula()).contains(q)) ||
                (c.getTelefono() != null && c.getTelefono().contains(q))) {

                listaFiltrada.add(c);
                tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getCedula() != null ? c.getCedula() : "",
                    c.getNombreCompleto(),
                    c.getTelefono() != null ? c.getTelefono() : "",
                    c.getTipoCabello() != null ? c.getTipoCabello().toString() : ""
                });
            }
        }

        lblContador.setText(String.format("Mostrando %d de %d clientes", listaFiltrada.size(), listaCache.size()));

        if (!listaFiltrada.isEmpty()) {
            tableResultados.setRowSelectionInterval(0, 0);
        }
    }

    private void seleccionarFilaActual() {
        int row = tableResultados.getSelectedRow();
        if (row < 0 || row >= listaFiltrada.size()) {
            ToastNotification.showWarning(this, "Seleccione un cliente de la lista.");
            return;
        }

        Cliente seleccionado = listaFiltrada.get(row);
        if (onSelected != null) {
            onSelected.accept(seleccionado);
        }
        cerrarModal();
    }

    private void abrirCreacionNuevoCliente() {
        String sugerido = txtSearch.getText().trim();
        ModalOption opt = ModalOption.getDefault()
                .setCloseOnClickOutside(false)
                .setAnimationEnabled(true);

        // Si lo escrito parece una cédula
        ClienteModal modal;
        if (sugerido.matches("^[0-9]+$") || sugerido.matches("^[VvJjGgPpEe]-[0-9]+$")) {
            String cedula = sugerido.contains("-") ? sugerido.toUpperCase() : "V-" + sugerido;
            modal = new ClienteModal(cedula, nuevo -> {
                if (onSelected != null) onSelected.accept(nuevo);
                cerrarModal();
            });
        } else {
            modal = new ClienteModal(nuevo -> {
                if (onSelected != null) onSelected.accept(nuevo);
                cerrarModal();
            });
        }

        ModalManager.showModal(this, modal, opt, "cliente_nuevo_desde_selector");
    }

    private void cerrarModal() {
        try {
            if (getController() != null) {
                getController().closeModal();
            } else {
                ModalManager.closeModal("cliente_selector");
            }
        } catch (Exception e) {
            ModalManager.closeModal("cliente_selector");
        }
    }

    private static String normalize(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str.toLowerCase().trim(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
