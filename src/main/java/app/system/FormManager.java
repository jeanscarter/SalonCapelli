package app.system;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;
import raven.modal.Toast;

public class FormManager {

    private final JPanel contentPane;

    public FormManager(JPanel contentPane) {
        this.contentPane = contentPane;
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
}