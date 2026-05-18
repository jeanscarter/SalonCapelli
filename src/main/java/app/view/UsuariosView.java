package app.view;

import app.exception.DatabaseException;
import app.model.Usuario;
import app.repository.UsuarioRepository;
import app.repository.UsuarioRepositorySQLite;
import app.service.AuthService;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Vista de Gestión de Usuarios del sistema.
 * Permite: crear, editar perfil/rol, cambiar contraseña, activar/desactivar usuarios.
 *
 * Accesible únicamente para usuarios con rol ADMIN.
 */
public class UsuariosView extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(UsuariosView.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarioRepo;

    private JTable table;
    private DefaultTableModel tableModel;

    public UsuariosView() {
        this.usuarioRepo = new UsuarioRepositorySQLite();
        init();
        cargarDatos();
    }

    private void init() {
        setLayout(new MigLayout("fill, insets 20, gap 15", "[grow, fill]", "[][grow, fill]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");

        // === Header ===
        add(createHeaderPanel(), "growx, wrap");

        // === Tabla ===
        add(createTablePanel(), "grow");
    }

    // ======================================================
    // PANEL: Header con título y botones
    // ======================================================

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new MigLayout("insets 10 15 10 15, fillx", "[]push[]10[]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background");

        JLabel title = new JLabel("Gestión de Usuarios");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +10; foreground:$Component.accentColor");
        panel.add(title);

        JButton btnNuevo = new JButton("+ Nuevo Usuario");
        btnNuevo.putClientProperty(FlatClientProperties.STYLE,
            "arc:10; background:$Component.accentColor; foreground:#fff; font:bold");
        btnNuevo.addActionListener(e -> mostrarDialogoNuevoUsuario());
        panel.add(btnNuevo);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.putClientProperty(FlatClientProperties.STYLE, "arc:10; font:bold");
        btnRefrescar.addActionListener(e -> cargarDatos());
        panel.add(btnRefrescar);

        return panel;
    }

    // ======================================================
    // PANEL: Tabla de usuarios
    // ======================================================

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 15", "[grow, fill]", "[grow, fill]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:15; background:$Panel.background");

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Usuario", "Rol", "Activo", "Fecha Creación"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines:true; intercellSpacing:0,1");

        // Columna ID oculta (ancho mínimo)
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(40);

        // Columna Activo con colores
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if ("Sí".equals(value)) {
                    if (!isSelected) setForeground(new Color(0, 150, 80));
                } else {
                    if (!isSelected) setForeground(new Color(200, 50, 50));
                }
                return c;
            }
        });

        // Popup menú contextual
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itemEditar = new JMenuItem("Editar Perfil");
        itemEditar.addActionListener(e -> editarUsuarioSeleccionado());
        popup.add(itemEditar);

        JMenuItem itemPassword = new JMenuItem("Cambiar Contraseña");
        itemPassword.addActionListener(e -> cambiarPasswordSeleccionado());
        popup.add(itemPassword);

        popup.addSeparator();

        JMenuItem itemToggle = new JMenuItem("Activar/Desactivar");
        itemToggle.addActionListener(e -> toggleActivoSeleccionado());
        popup.add(itemToggle);

        popup.addSeparator();

        JMenuItem itemEliminar = new JMenuItem("Eliminar");
        itemEliminar.addActionListener(e -> eliminarUsuarioSeleccionado());
        popup.add(itemEliminar);

        table.setComponentPopupMenu(popup);

        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, "grow");

        return panel;
    }

    // ======================================================
    // CARGA DE DATOS
    // ======================================================

    private void cargarDatos() {
        tableModel.setRowCount(0);
        try {
            List<Usuario> usuarios = usuarioRepo.findAll();
            for (Usuario u : usuarios) {
                tableModel.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getRol(),
                    u.isActivo() ? "Sí" : "No",
                    u.getFechaCreacion() != null ? u.getFechaCreacion().format(FMT) : "—"
                });
            }
            logger.info("✓ Cargados {} usuarios", usuarios.size());
        } catch (DatabaseException e) {
            logger.error("Error al cargar usuarios", e);
            ToastNotification.showError(this, "Error", "No se pudieron cargar los usuarios.");
        }
    }

    // ======================================================
    // DIÁLOGOS: Crear / Editar / Password / Eliminar
    // ======================================================

    private void mostrarDialogoNuevoUsuario() {
        JPanel formPanel = new JPanel(new MigLayout("fillx, wrap 2", "[right]10[grow, fill]", "[]8[]8[]8[]"));

        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JPasswordField txtConfirm = new JPasswordField(20);
        JComboBox<String> cbRol = new JComboBox<>(new String[]{"ADMIN", "CAJERO", "SUPERVISOR"});

        formPanel.add(new JLabel("Usuario:"));
        formPanel.add(txtUsername);
        formPanel.add(new JLabel("Contraseña:"));
        formPanel.add(txtPassword);
        formPanel.add(new JLabel("Confirmar:"));
        formPanel.add(txtConfirm);
        formPanel.add(new JLabel("Rol:"));
        formPanel.add(cbRol);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Nuevo Usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            String confirm = new String(txtConfirm.getPassword());
            String rol = (String) cbRol.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                ToastNotification.showWarning(this, "Complete todos los campos obligatorios.");
                return;
            }
            if (!password.equals(confirm)) {
                ToastNotification.showError(this, "Error", "Las contraseñas no coinciden.");
                return;
            }
            if (password.length() < 4) {
                ToastNotification.showWarning(this, "La contraseña debe tener al menos 4 caracteres.");
                return;
            }

            try {
                // Verificar si el username ya existe
                if (usuarioRepo.findByUsername(username) != null) {
                    ToastNotification.showError(this, "Error", "El usuario '" + username + "' ya existe.");
                    return;
                }

                Usuario nuevo = new Usuario();
                nuevo.setUsername(username);
                nuevo.setPasswordHash(AuthService.hashPassword(password));
                nuevo.setRol(rol);
                nuevo.setActivo(true);

                usuarioRepo.save(nuevo);
                cargarDatos();
                ToastNotification.showSuccess(this, "Usuario Creado",
                    "Usuario '" + username + "' creado con rol " + rol);
            } catch (DatabaseException e) {
                logger.error("Error al crear usuario", e);
                ToastNotification.showError(this, "Error", "No se pudo crear el usuario: " + e.getMessage());
            }
        }
    }

    private void editarUsuarioSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Seleccione un usuario de la tabla.");
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String currentUsername = (String) tableModel.getValueAt(row, 1);
        String currentRol = (String) tableModel.getValueAt(row, 2);

        JPanel formPanel = new JPanel(new MigLayout("fillx, wrap 2", "[right]10[grow, fill]", "[]8[]"));

        JTextField txtUsername = new JTextField(currentUsername, 20);
        JComboBox<String> cbRol = new JComboBox<>(new String[]{"ADMIN", "CAJERO", "SUPERVISOR"});
        cbRol.setSelectedItem(currentRol);

        formPanel.add(new JLabel("Usuario:"));
        formPanel.add(txtUsername);
        formPanel.add(new JLabel("Rol:"));
        formPanel.add(cbRol);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Editar Usuario (ID: " + userId + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newUsername = txtUsername.getText().trim();
            String newRol = (String) cbRol.getSelectedItem();

            if (newUsername.isEmpty()) {
                ToastNotification.showWarning(this, "El nombre de usuario no puede estar vacío.");
                return;
            }

            try {
                Usuario u = usuarioRepo.findById(userId);
                if (u == null) {
                    ToastNotification.showError(this, "Error", "Usuario no encontrado.");
                    return;
                }

                // Verificar duplicado si cambió el username
                if (!newUsername.equals(currentUsername)) {
                    if (usuarioRepo.findByUsername(newUsername) != null) {
                        ToastNotification.showError(this, "Error", "El usuario '" + newUsername + "' ya existe.");
                        return;
                    }
                }

                u.setUsername(newUsername);
                u.setRol(newRol);

                usuarioRepo.update(u);
                cargarDatos();
                ToastNotification.showSuccess(this, "Usuario Actualizado",
                    "Perfil de '" + newUsername + "' actualizado.");
            } catch (DatabaseException e) {
                logger.error("Error al editar usuario", e);
                ToastNotification.showError(this, "Error", "No se pudo actualizar: " + e.getMessage());
            }
        }
    }

    private void cambiarPasswordSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Seleccione un usuario de la tabla.");
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);

        JPanel formPanel = new JPanel(new MigLayout("fillx, wrap 2", "[right]10[grow, fill]", "[]8[]"));

        JPasswordField txtNewPass = new JPasswordField(20);
        JPasswordField txtConfirm = new JPasswordField(20);

        formPanel.add(new JLabel("Nueva Contraseña:"));
        formPanel.add(txtNewPass);
        formPanel.add(new JLabel("Confirmar:"));
        formPanel.add(txtConfirm);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "Cambiar Contraseña: " + username, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newPass = new String(txtNewPass.getPassword());
            String confirm = new String(txtConfirm.getPassword());

            if (newPass.isEmpty()) {
                ToastNotification.showWarning(this, "La contraseña no puede estar vacía.");
                return;
            }
            if (!newPass.equals(confirm)) {
                ToastNotification.showError(this, "Error", "Las contraseñas no coinciden.");
                return;
            }
            if (newPass.length() < 4) {
                ToastNotification.showWarning(this, "La contraseña debe tener al menos 4 caracteres.");
                return;
            }

            try {
                usuarioRepo.updatePassword(userId, AuthService.hashPassword(newPass));
                ToastNotification.showSuccess(this, "Contraseña Actualizada",
                    "Contraseña de '" + username + "' cambiada exitosamente.");
            } catch (DatabaseException e) {
                logger.error("Error al cambiar contraseña", e);
                ToastNotification.showError(this, "Error", "No se pudo cambiar la contraseña.");
            }
        }
    }

    private void toggleActivoSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Seleccione un usuario de la tabla.");
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);
        boolean currentlyActive = "Sí".equals(tableModel.getValueAt(row, 3));

        // Proteger al admin actual
        Usuario currentUser = AuthService.getCurrentUser();
        if (currentUser != null && currentUser.getId() == userId) {
            ToastNotification.showError(this, "Error", "No puede desactivar su propia cuenta.");
            return;
        }

        String action = currentlyActive ? "desactivar" : "activar";
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de " + action + " al usuario '" + username + "'?",
            "Confirmar", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Usuario u = usuarioRepo.findById(userId);
                if (u != null) {
                    u.setActivo(!currentlyActive);
                    usuarioRepo.update(u);
                    cargarDatos();
                    ToastNotification.showSuccess(this, "Estado Actualizado",
                        "Usuario '" + username + "' " + (currentlyActive ? "desactivado" : "activado") + ".");
                }
            } catch (DatabaseException e) {
                logger.error("Error al cambiar estado de usuario", e);
                ToastNotification.showError(this, "Error", "No se pudo cambiar el estado.");
            }
        }
    }

    private void eliminarUsuarioSeleccionado() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.showWarning(this, "Seleccione un usuario de la tabla.");
            return;
        }

        int userId = (int) tableModel.getValueAt(row, 0);
        String username = (String) tableModel.getValueAt(row, 1);

        // Proteger al admin actual
        Usuario currentUser = AuthService.getCurrentUser();
        if (currentUser != null && currentUser.getId() == userId) {
            ToastNotification.showError(this, "Error", "No puede eliminar su propia cuenta.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Eliminar definitivamente al usuario '" + username + "'?\nEsta acción no se puede deshacer.",
            "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                usuarioRepo.delete(userId);
                cargarDatos();
                ToastNotification.showSuccess(this, "Eliminado", "Usuario '" + username + "' eliminado.");
            } catch (DatabaseException e) {
                logger.error("Error al eliminar usuario", e);
                ToastNotification.showError(this, "Error", "No se pudo eliminar: " + e.getMessage());
            }
        }
    }
}
