package app.view;

import app.service.AuthService;
import app.system.FormManager;
import app.util.ToastNotification;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class LoginView extends JPanel {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private FormManager formManager;

    public LoginView(FormManager formManager) {
        this.formManager = formManager;
        init();
    }

    private void init() {
        setLayout(new MigLayout("fill", "[center]", "[center]"));

        JPanel panel = new JPanel(new MigLayout("wrap, insets 30 40 30 40", "[fill, 250]", "[]20[]10[]20[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc:20; background:$Panel.background");

        JLabel lblTitle = new JLabel("Salón Capelli");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +15");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTitle);

        JLabel lblSubtitle = new JLabel("Ingreso al Sistema");
        lblSubtitle.putClientProperty(FlatClientProperties.STYLE, "font:+2; foreground:$Label.disabledForeground");
        lblSubtitle.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblSubtitle);

        txtUsername = new JTextField();
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Usuario");
        txtUsername.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:5,10,5,10");
        panel.add(new JLabel("Usuario"), "gapy 10");
        panel.add(txtUsername);

        txtPassword = new JPasswordField();
        txtPassword.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "arc:10; margin:5,10,5,10");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton:true");
        panel.add(new JLabel("Contraseña"), "gapy 10");
        panel.add(txtPassword);

        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.putClientProperty(FlatClientProperties.STYLE, "arc:10; background:$Component.accentColor; foreground:#fff; font:bold; margin:8,0,8,0");
        btnLogin.addActionListener(e -> login());
        panel.add(btnLogin, "gapy 20");

        add(panel);
        
        // Enter para login
        txtPassword.addActionListener(e -> login());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());
    }

    private void login() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            ToastNotification.showWarning(this, "Complete los campos de usuario y contraseña.");
            return;
        }

        if (AuthService.authenticate(user, pass)) {
            ToastNotification.showSuccess(this, "Bienvenido", "Inicio de sesión exitoso.");
            // Ir al dashboard
            formManager.showForm(new HomeView());
            formManager.getMainFrame().showToolbar();
            // Inicializar el Drawer con el rol adecuado (o al menos notificar)
            formManager.getMainFrame().updateDrawerFooter(AuthService.getCurrentUser().getUsername(), AuthService.getCurrentUser().getRol());
        } else {
            ToastNotification.showError(this, "Acceso Denegado", "Usuario o contraseña incorrectos.");
        }
    }
}
