package app.main;

import app.menu.MyDrawerBuilder;
import app.system.FormManager;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;

import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import raven.modal.Drawer;

public class MainFrame extends JFrame {

    private FormManager formManager;
    private JPanel body;
    private JToolBar toolbar;
    private MyDrawerBuilder drawerBuilder;

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

        // Toolbar
        toolbar = new JToolBar();
        toolbar.setVisible(false); // Oculto hasta login exitoso

        JButton cmdMenu = new JButton();
        cmdMenu.setIcon(new FlatSVGIcon("icons/menu.svg"));
        cmdMenu.addActionListener(e -> Drawer.showDrawer());

        toolbar.add(cmdMenu);
        contentPane.add(toolbar, BorderLayout.NORTH);

        // Panel central (Body)
        body = new JPanel(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);

        // 1. Instanciar FormManager
        formManager = new FormManager(body, this);

        // 2. Instalar Drawer - Usamos el layout responsivo por defecto
        drawerBuilder = new MyDrawerBuilder(formManager);
        Drawer.installDrawer(this, drawerBuilder);

        // Vista inicial (Login)
        formManager.showForm(new app.view.LoginView(formManager));
    }
    
    public void showToolbar() {
        if (toolbar != null) {
            toolbar.setVisible(true);
        }
    }
    
    /* CORRECCIÓN #5: Actualiza el footer del drawer y fuerza repintado */
    public void updateDrawerFooter(String username, String rol) {
        if (drawerBuilder != null && drawerBuilder.getSimpleFooterData() != null) {
            drawerBuilder.getSimpleFooterData().setTitle(username);
            drawerBuilder.getSimpleFooterData().setDescription(rol);
            // Forzar repintado del componente drawer
            repaint();
        }
    }
}