package app.system;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;
import raven.modal.Toast;

public class FormManager {

    private static FormManager instance;
    private final JPanel contentPane;

    private FormManager(JPanel contentPane) {
        this.contentPane = contentPane;
    }

    public static void install(JPanel contentPane) {
        instance = new FormManager(contentPane);
    }

    public static void showForm(Component form) {
        if (instance == null) {
            System.err.println("FormManager not installed");
            return;
        }
        instance.contentPane.removeAll();
        instance.contentPane.add(form, BorderLayout.CENTER);
        instance.contentPane.revalidate();
        instance.contentPane.repaint();
    }
    
    public static void showToast(String message) {
        Toast.show(instance.contentPane, Toast.Type.INFO, message);
    }
}