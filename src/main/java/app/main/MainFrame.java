package app.main;

import app.menu.MyDrawerBuilder;
import app.system.FormManager;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import raven.modal.Drawer;

public class MainFrame extends JFrame {

    public MainFrame() {
        init();
    }

    private void init() {
        setTitle("Capelli Sales Dashboard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        // --- NUEVO: Configuración del Icono de la App ---
        // Esto pone el logo en la barra de tareas y en la barra de título
        try {
            URL imgURL = getClass().getResource("/images/CapelliPng.png");
            if (imgURL != null) {
                ImageIcon icon = new ImageIcon(imgURL);
                setIconImage(icon.getImage());
            } else {
                System.err.println("No se encontró la imagen del icono: /images/CapelliPng.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- NUEVO: Maximizar la ventana por defecto ---
        // Usamos esto en lugar de setSize para que ocupe toda la pantalla
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        
        // 1. Panel de Contenido Principal
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.putClientProperty(FlatClientProperties.STYLE, "background:$Main.background");
        setContentPane(contentPane);

        // 2. Toolbar simple (Opcional)
        JToolBar toolbar = new JToolBar();
        JButton cmdMenu = new JButton("Menú");
        cmdMenu.addActionListener(e -> Drawer.showDrawer()); 
        toolbar.add(cmdMenu);
        contentPane.add(toolbar, BorderLayout.NORTH);

        // 3. Panel central donde se cargarán los formularios
        JPanel body = new JPanel(new BorderLayout());
        contentPane.add(body, BorderLayout.CENTER);

        // 4. Instalar nuestro FormManager
        FormManager.install(body);

        // 5. Instalación del Drawer
        Drawer.installDrawer(this, new MyDrawerBuilder());
        
        // Vista inicial
        FormManager.showForm(new javax.swing.JLabel("Bienvenido al Sistema Capelli", javax.swing.SwingConstants.CENTER));
    }
}