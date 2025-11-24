package app.main;

import app.db.DatabaseConnection; 
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Font;
import javax.swing.UIManager;

public class Application {

    public static void main(String[] args) {
        // 1. Configuración de Fuente y Tema (FlatLaf)
        FlatRobotoFont.install();
        FlatLaf.registerCustomDefaultsSource("raven.modal.demo.themes");
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        FlatMacLightLaf.setup();

        // 2. CORRECCIÓN CRÍTICA: Inicializar la Base de Datos antes de la GUI
        // Esto crea el archivo salon_capelli.db y la tabla 'clientes' si no existen.
        DatabaseConnection.initDatabase();

        // 3. Iniciar la Ventana Principal
        java.awt.EventQueue.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}