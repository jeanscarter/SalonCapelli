package app.main;

import app.menu.MyDrawerBuilder;
import app.system.FormManager;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import raven.modal.Drawer;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainFrame extends JFrame {

    private FormManager formManager;
    private JPanel body;
    private JToolBar toolbar;
    private MyDrawerBuilder drawerBuilder;
    private JLabel lblToolbarUser;
    private JButton btnToolbarLogout;

    public MainFrame() {
        init();
    }

    private void init() {
        setTitle("Capelli Sales Dashboard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            URL imgURL = getClass().getResource("/images/CapelliPng.png");
            if (imgURL != null) {
                setIconImage(new ImageIcon(imgURL).getImage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");
        setContentPane(contentPane);

        // Toolbar superior
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setVisible(false); // Oculto hasta login exitoso
        toolbar.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background; border:0,0,1,0,$Component.borderColor; margin:4,8,4,8");

        JButton cmdMenu = new JButton();
        cmdMenu.setIcon(new FlatSVGIcon("icons/menu.svg", 20, 20));
        cmdMenu.setToolTipText("Abrir / Cerrar Menú");
        cmdMenu.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:4,8,4,8");
        cmdMenu.addActionListener(e -> Drawer.showDrawer());
        toolbar.add(cmdMenu);

        // Espaciador flexible para empujar el perfil y logout a la derecha
        toolbar.add(Box.createHorizontalGlue());

        // Etiqueta de usuario activo en la barra superior
        lblToolbarUser = new JLabel();
        lblToolbarUser.setIcon(new FlatSVGIcon("icons/user.svg", 18, 18));
        lblToolbarUser.putClientProperty(FlatClientProperties.STYLE, "font:bold; foreground:$Label.foreground; border:0,10,0,10");
        toolbar.add(lblToolbarUser);

        // Botón de Cerrar Sesión en la barra superior
        btnToolbarLogout = new JButton("Cerrar Sesión");
        btnToolbarLogout.setIcon(new FlatSVGIcon("icons/logout.svg", 16, 16));
        btnToolbarLogout.setToolTipText("Cerrar sesión de usuario");
        btnToolbarLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToolbarLogout.putClientProperty(FlatClientProperties.STYLE, 
                "arc:10; margin:4,10,4,10; font:bold -1; " +
                "background:#FF4C4C18; foreground:#E03131; " +
                "hoverBackground:#FF4C4C2E; hoverForeground:#C92A2A; " +
                "border:1,1,1,1,#FF4C4C40; " +
                "[dark]background:#FF4C4C22; [dark]foreground:#FF8787; " +
                "[dark]hoverBackground:#FF4C4C3E; [dark]hoverForeground:#FFA8A8; " +
                "[dark]border:1,1,1,1,#FF6B6B50");
        btnToolbarLogout.addActionListener(e -> {
            if (formManager != null) {
                formManager.logout();
            }
        });
        toolbar.add(btnToolbarLogout);

        contentPane.add(toolbar, BorderLayout.NORTH);

        // Panel central (Body)
        body = new JPanel(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);

        // 1. Instanciar FormManager
        formManager = new FormManager(body, this);

        // 2. Instalar Drawer
        drawerBuilder = new MyDrawerBuilder(formManager);
        Drawer.installDrawer(this, drawerBuilder);

        // Ocultar drawer inicialmente mientras no haya sesión activa
        Drawer.setVisible(false);

        // Vista inicial (Login)
        formManager.showForm(new app.view.LoginView(formManager));
    }
    
    public void showToolbar() {
        if (toolbar != null) {
            toolbar.setVisible(true);
        }
    }

    public void hideToolbar() {
        if (toolbar != null) {
            toolbar.setVisible(false);
        }
    }
    
    public void updateDrawerFooter(String username, String rol) {
        if (drawerBuilder != null) {
            drawerBuilder.updateUser(username, rol);
        }
        if (lblToolbarUser != null) {
            String roleText = (rol != null && !rol.isEmpty()) ? " (" + rol + ")" : "";
            lblToolbarUser.setText((username != null ? username : "Usuario") + roleText);
        }
        repaint();
    }
}