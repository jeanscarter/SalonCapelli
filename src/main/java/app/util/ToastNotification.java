package app.util;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raven.modal.Toast;
import raven.modal.toast.option.ToastLocation;
import raven.modal.toast.option.ToastOption;
import raven.modal.toast.option.ToastStyle;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

public class ToastNotification {
    
    private static final Logger logger = LoggerFactory.getLogger(ToastNotification.class);
    
    private enum NotificationType {
        // Formato: (Título, Color para Modo Claro, Color Vívido para Modo Oscuro)
        SUCCESS("Éxito",       new Color(0, 150, 60),    new Color(0, 255, 128)),  // Verde Neón
        INFO("Información",    new Color(33, 150, 243),  new Color(64, 196, 255)), // Azul Cielo Brillante
        WARNING("Advertencia", new Color(255, 143, 0),   new Color(255, 193, 7)),  // Ámbar Brillante
        ERROR("Error",         new Color(211, 47, 47),   new Color(255, 82, 82));  // Rojo Brillante/Salmón

        final String defaultTitle;
        final Color lightColor;
        final Color darkColor;

        NotificationType(String defaultTitle, Color lightColor, Color darkColor) {
            this.defaultTitle = defaultTitle;
            this.lightColor = lightColor;
            this.darkColor = darkColor;
        }
    }

    private ToastNotification() {
    }
    
    // --- Métodos Públicos ---

    public static void showSuccess(Component owner, String message) {
        showCustomNotification(owner, NotificationType.SUCCESS, null, message);
    }
    
    public static void showSuccess(Component owner, String title, String message) {
        showCustomNotification(owner, NotificationType.SUCCESS, title, message);
    }
    
    public static void showInfo(Component owner, String message) {
        showCustomNotification(owner, NotificationType.INFO, null, message);
    }
    
    public static void showInfo(Component owner, String title, String message) {
        showCustomNotification(owner, NotificationType.INFO, title, message);
    }
    
    public static void showWarning(Component owner, String message) {
        showCustomNotification(owner, NotificationType.WARNING, null, message);
    }
    
    public static void showWarning(Component owner, String title, String message) {
        showCustomNotification(owner, NotificationType.WARNING, title, message);
    }
    
    public static void showError(Component owner, String message) {
        showCustomNotification(owner, NotificationType.ERROR, null, message);
    }
    
    public static void showError(Component owner, String title, String message) {
        showCustomNotification(owner, NotificationType.ERROR, title, message);
    }
    
    public static void showValidationErrors(Component owner, app.exception.ValidationException e) {
        logger.warn("Mostrando errores de validación: {} errores", e.getErrorCount());
        
        if (e.getErrorCount() == 1) {
            app.exception.ValidationException.ValidationError error = e.getErrors().get(0);
            showError(owner, "Error de Validación", error.getMessage());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><div style='width: 250px;'>");
            sb.append("<b>Por favor corrija los siguientes errores:</b><br>");
            sb.append("<ul style='margin-left: 10px; margin-top: 5px;'>");
            
            for (app.exception.ValidationException.ValidationError error : e.getErrors()) {
                sb.append("<li>").append(error.getMessage()).append("</li>");
            }
            
            sb.append("</ul></div></html>");
            
            showCustomNotification(owner, NotificationType.WARNING, "Errores de Validación", sb.toString());
        }
    }

    // --- Lógica Privada ---

    private static void showCustomNotification(Component owner, NotificationType type, String title, String message) {
        SwingUtilities.invokeLater(() -> {
            if (owner != null && owner.isDisplayable()) {
                
                // Determinamos si es modo oscuro para elegir el color "POP" o el estándar
                boolean isDark = FlatLaf.isLafDark();
                Color accentColor = isDark ? type.darkColor : type.lightColor;
                Color textColor = isDark ? new Color(240, 240, 240) : new Color(60, 60, 60);

                // Panel
                JPanel panel = new JPanel(new MigLayout("insets 8 12 8 12, fillx", "[fill]", "[]2[]"));
                panel.setOpaque(false);
                
                // Borde izquierdo de 4px iluminado
                panel.setBorder(new MatteBorder(0, 4, 0, 0, accentColor));

                // Título
                String finalTitle = (title == null || title.isEmpty()) ? type.defaultTitle : title;
                JLabel lblTitle = new JLabel(finalTitle);
                lblTitle.setForeground(accentColor);
                lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +1");
                
                // Mensaje
                String finalMessage = message;
                if (message != null && !message.toLowerCase().trim().startsWith("<html>")) {
                    finalMessage = "<html><div style='width: 260px;'>" + message + "</div></html>";
                }
                
                JLabel lblMessage = new JLabel(finalMessage);
                lblMessage.setForeground(textColor); // Texto más brillante en modo oscuro
                
                panel.add(lblTitle, "wrap");
                panel.add(lblMessage);
                
                Toast.showCustom(owner, panel, createDefaultOption());
                
            } else {
                logger.warn("No se pudo mostrar toast - componente no visible");
            }
        });
    }
    
    private static ToastOption createDefaultOption() {
        ToastOption option = ToastOption.getDefault();
        
        ToastStyle style = option.getStyle();
        style.setShowLabel(false);
        style.setShowIcon(false);
        style.setShowCloseButton(true);
        
        option.getLayoutOption()
            .setLocation(ToastLocation.TOP_CENTER);
            
        option.setAutoClose(true)
            .setDelay(4000)
            .setDuration(300)
            .setPauseDelayOnHover(true)
            .setCloseOnClick(false);
        
        return option;
    }
}