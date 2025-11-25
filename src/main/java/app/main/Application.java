package app.main;

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
            // Paso 1: Configurar Look & Feel
            configureLookAndFeel();
            
            // Paso 2: Inicializar Base de Datos
            initializeDatabase();
            
            // Paso 3: Registrar Shutdown Hooks
            registerShutdownHooks();
            
            // Paso 4: Lanzar Interfaz Gráfica
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
            
            // Registrar temas custom
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