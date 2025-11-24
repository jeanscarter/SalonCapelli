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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import raven.modal.Drawer;

public class MainFrame extends JFrame {
    
    private FormManager formManager;

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
        JToolBar toolbar = new JToolBar();
        
        // CORRECCIÓN: Usamos Drawer.showDrawer() directamente
        JButton cmdMenu = new JButton();
        cmdMenu.setIcon(new FlatSVGIcon("icons/menu.svg")); 
        cmdMenu.addActionListener(e -> Drawer.showDrawer());
        
        toolbar.add(cmdMenu);
        contentPane.add(toolbar, BorderLayout.NORTH);

        // Panel central (Body)
        JPanel body = new JPanel(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);

        // 1. Instanciar FormManager
        formManager = new FormManager(body);

        // 2. Instalar Drawer
        Drawer.installDrawer(this, new MyDrawerBuilder(formManager));
        
        // Vista inicial
        formManager.showForm(new JLabel("Bienvenido al Sistema Capelli", SwingConstants.CENTER));
    }
}