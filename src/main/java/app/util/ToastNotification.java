package app.util;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raven.modal.Toast;
import raven.modal.toast.option.ToastLocation;
import raven.modal.toast.option.ToastOption;
import raven.modal.toast.option.ToastStyle;

import javax.swing.*;
import java.awt.*;

/**
 * Utilidad centralizada para mostrar notificaciones Toast
 * Patrón: Facade + Strategy
 */
public class ToastNotification {
    
    private static final Logger logger = LoggerFactory.getLogger(ToastNotification.class);
    
    private ToastNotification() {
    }
    
    /**
     * Muestra un toast de éxito
     */
    public static void showSuccess(Component owner, String message) {
        showSuccess(owner, "Éxito", message);
    }
    
    public static void showSuccess(Component owner, String title, String message) {
        logger.info("Toast Success: {}", message);
        show(owner, Toast.Type.SUCCESS, title, message);
    }
    
    /**
     * Muestra un toast de información
     */
    public static void showInfo(Component owner, String message) {
        showInfo(owner, "Información", message);
    }
    
    public static void showInfo(Component owner, String title, String message) {
        logger.info("Toast Info: {}", message);
        show(owner, Toast.Type.INFO, title, message);
    }
    
    /**
     * Muestra un toast de advertencia
     */
    public static void showWarning(Component owner, String message) {
        showWarning(owner, "Advertencia", message);
    }
    
    public static void showWarning(Component owner, String title, String message) {
        logger.warn("Toast Warning: {}", message);
        show(owner, Toast.Type.WARNING, title, message);
    }
    
    /**
     * Muestra un toast de error
     */
    public static void showError(Component owner, String message) {
        showError(owner, "Error", message);
    }
    
    public static void showError(Component owner, String title, String message) {
        logger.error("Toast Error: {}", message);
        show(owner, Toast.Type.ERROR, title, message);
    }
    
    /**
     * Muestra errores de validación con formato especial
     */
    public static void showValidationErrors(Component owner, app.exception.ValidationException e) {
        logger.warn("Mostrando errores de validación: {} errores", e.getErrorCount());
        
        if (e.getErrorCount() == 1) {
            // Un solo error - mostrar directamente
            app.exception.ValidationException.ValidationError error = e.getErrors().get(0);
            showError(owner, "Error de Validación", error.getMessage());
        } else {
            // Múltiples errores - crear mensaje con lista
            StringBuilder message = new StringBuilder();
            message.append("<html><body style='width: 300px;'>");
            message.append("<b>Por favor corrija los siguientes errores:</b><br><br>");
            
            for (app.exception.ValidationException.ValidationError error : e.getErrors()) {
                message.append("• ").append(error.getMessage()).append("<br>");
            }
            
            message.append("</body></html>");
            
            showCustomError(owner, "Errores de Validación", message.toString());
        }
    }
    
    /**
     * Método privado para mostrar toast básico
     */
    private static void show(Component owner, Toast.Type type, String title, String message) {
        SwingUtilities.invokeLater(() -> {
            if (owner != null && owner.isDisplayable()) {
                ToastOption option = createDefaultOption();
                String formattedMessage = formatMessage(title, message);
                Toast.show(owner, type, formattedMessage, option);
            } else {
                logger.warn("No se pudo mostrar toast - componente no visible");
            }
        });
    }
    
    /**
     * Muestra un toast de error personalizado con HTML
     */
    private static void showCustomError(Component owner, String title, String htmlMessage) {
        SwingUtilities.invokeLater(() -> {
            if (owner != null && owner.isDisplayable()) {
                ToastOption option = createDefaultOption();
                
                // Panel personalizado para mensaje HTML
                JPanel panel = new JPanel(new MigLayout("insets 10", "[]", "[]"));
                panel.setOpaque(false);
                
                JLabel titleLabel = new JLabel(title);
                titleLabel.putClientProperty(FlatClientProperties.STYLE, "" +
                    "font:bold +2;" +
                    "foreground:$Component.accentColor");
                
                JLabel messageLabel = new JLabel(htmlMessage);
                messageLabel.putClientProperty(FlatClientProperties.STYLE, "" +
                    "foreground:$Label.foreground");
                
                panel.add(titleLabel, "wrap");
                panel.add(messageLabel);
                
                Toast.showCustom(owner, panel, option);
            }
        });
    }
    
    /**
     * Formatea el mensaje con título
     */
    private static String formatMessage(String title, String message) {
        if (title == null || title.trim().isEmpty()) {
            return message;
        }
        return String.format("<html><body style='width: 280px;'><b>%s</b><br>%s</body></html>", 
            title, message);
    }
    
    /**
     * Crea las opciones por defecto para los toast
     */
    private static ToastOption createDefaultOption() {
        ToastOption option = ToastOption.getDefault();
        
        // Configurar estilo
        ToastStyle style = option.getStyle();
        style.setShowLabel(false); // No mostrar label redundante
        style.setShowIcon(true);
        style.setShowCloseButton(true);
        
        // Configurar layout
        option.getLayoutOption()
            .setLocation(ToastLocation.TOP_CENTER);
        
        // Configurar comportamiento
        option.setAutoClose(true)
            .setDelay(4000) // 4 segundos
            .setDuration(350)
            .setPauseDelayOnHover(true)
            .setCloseOnClick(false);
        
        return option;
    }
}