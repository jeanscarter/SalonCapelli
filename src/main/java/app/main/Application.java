package app.main; // Trigger rebuild

import app.db.DatabaseConnection;
import app.exception.DatabaseException;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Font;

/**
 * Clase principal de la aplicación
 * Patrón: Template Method + Singleton (implícito en main)
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    // Información de la aplicación
    private static final String APP_NAME = "Sistema Capelli";
    private static final String APP_VERSION = "1.0.0";
    
    public static void main(String[] args) {
    logger.info("========================================");
    logger.info("Iniciando {} v{}", APP_NAME, APP_VERSION);
    logger.info("========================================");
    
    try {
        configureLookAndFeel();
        initializeDatabase();
        registerShutdownHooks();
        launchGUI();
        
        logger.info("✓ Aplicación iniciada exitosamente");
        
    } catch (Exception e) {
        logger.error("ERROR CRÍTICO: No se pudo iniciar la aplicación", e);
        showFatalErrorDialog(e);
        System.exit(1);
    }
}

    /**
     * Configura el Look & Feel de la aplicación
     */
    private static void configureLookAndFeel() {
        logger.info("Configurando Look & Feel...");
        
        try {
            // Instalar fuente Roboto
            FlatRobotoFont.install();
            logger.debug("✓ Fuente Roboto instalada");
            
            // Configurar paletas dinámicas y UI (Coral y tema Light/Dark)
            java.util.Map<String, String> customPalette = new java.util.HashMap<>();
            
            // Estructura y Bordes (Compartidos)
            customPalette.put("Component.arc", "12");
            customPalette.put("Button.arc", "14");
            customPalette.put("TextComponent.arc", "10");
            customPalette.put("Table.showHorizontalLines", "true");
            customPalette.put("Table.showVerticalLines", "false");
            customPalette.put("Table.rowHeight", "30");

            // Modo Claro (Light)
            customPalette.put("@background", "#FFFFFF");
            customPalette.put("@control", "#F8F9FA");
            customPalette.put("@accentColor", "#FF7F6F");
            customPalette.put("Button.default.background", "#FF7F6F");
            customPalette.put("@foreground", "#2D3436");
            customPalette.put("Component.accentColor", "#FF7F6F");
            customPalette.put("Success.color", "#00D29E");
            customPalette.put("Error.color", "#FF4C4C");
            
            // Modo Oscuro (Dark)
            customPalette.put("[dark]@background", "#121212");
            customPalette.put("[dark]@control", "#1E1E1E");
            customPalette.put("[dark]@accentColor", "#FF8B7D");
            customPalette.put("[dark]Button.default.background", "#FF7F6F");
            customPalette.put("[dark]@foreground", "#E0E0E0");
            customPalette.put("[dark]Component.accentColor", "#FF8B7D");
            customPalette.put("[dark]Success.color", "#00D29E");
            customPalette.put("[dark]Error.color", "#FF4C4C");

            FlatLaf.setGlobalExtraDefaults(customPalette);

            // Registrar temas custom de la librería Drawer
            FlatLaf.registerCustomDefaultsSource("raven.modal.demo.themes");
            logger.debug("✓ Temas personalizados registrados");
            
            // Configurar fuente por defecto
            UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
            logger.debug("✓ Fuente por defecto configurada: Roboto 13pt");
            
            // Aplicar tema FlatLaf
            FlatMacLightLaf.setup();
            logger.info("✓ Look & Feel aplicado: FlatMacLightLaf");
            
        } catch (Exception e) {
            logger.warn("No se pudo configurar el Look & Feel custom, usando por defecto", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                logger.info("✓ Look & Feel del sistema aplicado");
            } catch (Exception ex) {
                logger.error("Error aplicando Look & Feel del sistema", ex);
            }
        }
    }

    /**
     * Inicializa la base de datos
     */
    private static void initializeDatabase() throws DatabaseException {
        logger.info("Inicializando base de datos...");
        
        long startTime = System.currentTimeMillis();
        DatabaseConnection.initDatabase();
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        logger.info("✓ Base de datos inicializada en {} ms", elapsedTime);
    }

    /**
     * Registra hooks para limpieza al cerrar la aplicación
     */
    private static void registerShutdownHooks() {
        logger.info("Registrando shutdown hooks...");
        
        // Hook para cerrar la base de datos
        DatabaseConnection.registerShutdownHook();
        
        // Hook general de la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("========================================");
            logger.info("Cerrando {} v{}", APP_NAME, APP_VERSION);
            logger.info("========================================");
            
            // Aquí se pueden agregar más tareas de limpieza
            // Por ejemplo: guardar configuraciones, cerrar conexiones, etc.
            
            logger.info("✓ Aplicación cerrada correctamente");
        }, "Application-Shutdown-Hook"));
        
        logger.debug("✓ Shutdown hooks registrados");
    }

    /**
     * Lanza la interfaz gráfica en el Event Dispatch Thread
     */
    private static void launchGUI() {
        logger.info("Lanzando interfaz gráfica...");
        
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                
                logger.info("✓ Ventana principal mostrada");
                
            } catch (Exception e) {
                logger.error("Error al crear la ventana principal", e);
                showFatalErrorDialog(e);
                System.exit(1);
            }
        });
    }

    /**
     * Muestra un diálogo de error crítico y termina la aplicación
     */
    private static void showFatalErrorDialog(Exception e) {
    String message = String.format(
        "Error crítico al iniciar la aplicación:\n\n%s\n\n" +
        "La aplicación se cerrará. Revise los logs para más detalles.",
        e.getMessage()
    );
    
    JOptionPane.showMessageDialog(
        null,
        message,
        "Error Crítico - " + APP_NAME,
        JOptionPane.ERROR_MESSAGE
    );
}
    
    /**
     * Obtiene la versión de la aplicación
     */
    public static String getVersion() {
        return APP_VERSION;
    }
    
    /**
     * Obtiene el nombre de la aplicación
     */
    public static String getName() {
        return APP_NAME;
    }
}