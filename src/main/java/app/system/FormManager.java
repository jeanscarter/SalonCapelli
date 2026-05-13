package app.system;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;
import raven.modal.Toast;

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
}