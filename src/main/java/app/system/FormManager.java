package app.system;

import app.service.AuthService;
import app.util.ToastNotification;
import app.view.LoginView;
import raven.modal.Drawer;
import raven.modal.Toast;

import javax.swing.*;
import java.awt.*;

public class FormManager {

    private final JPanel contentPane;
    private final app.main.MainFrame mainFrame;

    public FormManager(JPanel contentPane, app.main.MainFrame mainFrame) {
        this.contentPane = contentPane;
        this.mainFrame = mainFrame;
    }
    
    public app.main.MainFrame getMainFrame() {
        return mainFrame;
    }

    public void showForm(Component form) {
        contentPane.removeAll();
        contentPane.add(form, BorderLayout.CENTER);
        contentPane.revalidate();
        contentPane.repaint();
    }
    
    public void showToast(String message) {
        Toast.show(contentPane, Toast.Type.INFO, message);
    }

    public void logout() {
        logout(true);
    }

    public void logout(boolean confirm) {
        if (confirm) {
            String currentUser = AuthService.getCurrentUser() != null ? AuthService.getCurrentUser().getUsername() : "";
            String msg = currentUser.isEmpty() 
                    ? "¿Está seguro de que desea cerrar la sesión actual?"
                    : "¿Está seguro de que desea cerrar la sesión de \"" + currentUser + "\"?";

            int option = JOptionPane.showConfirmDialog(
                    mainFrame,
                    msg,
                    "Cerrar Sesión",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Limpiar estado de sesión
        AuthService.logout();

        // Ocultar menú lateral y barra de herramientas
        Drawer.setVisible(false);
        if (mainFrame != null) {
            mainFrame.hideToolbar();
        }

        // Mostrar vista de login
        LoginView loginView = new LoginView(this);
        showForm(loginView);

        // Notificación Toast
        ToastNotification.showInfo(mainFrame, "Sesión Finalizada", "Ha cerrado sesión correctamente.");
    }
}