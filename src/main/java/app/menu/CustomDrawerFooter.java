package app.menu;

import app.system.FormManager;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.extras.LightDarkButton;
import raven.modal.drawer.menu.AbstractMenuElement;
import raven.modal.drawer.menu.MenuOption;

import javax.swing.*;
import java.awt.*;

/**
 * Pie del menú lateral (Drawer Footer) con tarjeta de usuario,
 * botón de Cerrar Sesión y selector de tema Claro/Oscuro.
 */
public class CustomDrawerFooter extends AbstractMenuElement {

    private final FormManager formManager;
    private MigLayout layout;
    
    private JPanel profileCard;
    private JLabel lblAvatar;
    private JLabel lblUsername;
    private JLabel lblRole;
    private JButton btnLogout;
    private LightDarkButton lightDarkButton;
    private JSeparator separator;

    public CustomDrawerFooter(FormManager formManager) {
        this.formManager = formManager;
        init();
    }

    private void init() {
        layout = new MigLayout("hidemode 3, wrap, fillx, insets 8 12 12 12, gap 6", "25[fill]25", "[]4[]8[]8[]");
        setLayout(layout);
        setOpaque(false);

        // Separador superior
        separator = new JSeparator();
        add(separator, "growx");

        // 1. Tarjeta de Usuario
        profileCard = new JPanel(new MigLayout("insets 6 8 6 8, fillx, gap 8", "[][grow]", "[][]"));
        profileCard.putClientProperty(FlatClientProperties.STYLE, "arc:12; background:$Panel.background; border:1,1,1,1,$Component.borderColor");

        lblAvatar = new JLabel();
        lblAvatar.setIcon(new FlatSVGIcon("icons/user.svg", 24, 24));
        profileCard.add(lblAvatar, "spany 2, aligny center");

        lblUsername = new JLabel("Usuario");
        lblUsername.putClientProperty(FlatClientProperties.STYLE, "font:bold +1; foreground:$Label.foreground");
        profileCard.add(lblUsername, "wrap");

        lblRole = new JLabel("ADMIN");
        lblRole.putClientProperty(FlatClientProperties.STYLE, "font:bold -2; foreground:$Component.accentColor");
        profileCard.add(lblRole);

        add(profileCard, "growx");

        // 2. Botón Cerrar Sesión
        btnLogout = new JButton("Cerrar Sesión");
        btnLogout.setIcon(new FlatSVGIcon("icons/logout.svg", 16, 16));
        btnLogout.setToolTipText("Cerrar sesión de usuario");
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.putClientProperty(FlatClientProperties.STYLE, 
                "arc:10; margin:6,10,6,10; font:bold; " +
                "background:#FF4C4C18; foreground:#E03131; " +
                "hoverBackground:#FF4C4C2E; hoverForeground:#C92A2A; " +
                "border:1,1,1,1,#FF4C4C40; " +
                "[dark]background:#FF4C4C22; [dark]foreground:#FF8787; " +
                "[dark]hoverBackground:#FF4C4C3E; [dark]hoverForeground:#FFA8A8; " +
                "[dark]border:1,1,1,1,#FF6B6B50");

        btnLogout.addActionListener(e -> {
            if (formManager != null) {
                formManager.logout();
            }
        });
        add(btnLogout, "growx");

        // 3. Botón de Tema (Light / Dark)
        lightDarkButton = new LightDarkButton(999);
        lightDarkButton.installAutoLafChangeListener();
        add(lightDarkButton, "growx");
    }

    public void updateUser(String username, String role) {
        if (username != null && !username.isEmpty()) {
            lblUsername.setText(username);
        } else {
            lblUsername.setText("Usuario");
        }

        if (role != null && !role.isEmpty()) {
            lblRole.setText(role.toUpperCase());
        } else {
            lblRole.setText("GENERAL");
        }

        revalidate();
        repaint();
    }

    @Override
    protected void layoutOptionChanged(MenuOption.MenuOpenMode menuOpenMode) {
        if (menuOpenMode == MenuOption.MenuOpenMode.FULL) {
            profileCard.setVisible(true);
            btnLogout.setText("Cerrar Sesión");
            btnLogout.setToolTipText("Cerrar sesión de usuario");
            lightDarkButton.setButtonStyle(LightDarkButton.ButtonStyle.DUAL_BUTTON);
            layout.setColumnConstraints("25[fill]25");
        } else {
            profileCard.setVisible(false);
            btnLogout.setText("");
            btnLogout.setToolTipText("Cerrar Sesión");
            lightDarkButton.setButtonStyle(LightDarkButton.ButtonStyle.TOGGLE_BUTTON);
            layout.setColumnConstraints("[center]");
        }
        revalidate();
        repaint();
    }

    public LightDarkButton getLightDarkButton() {
        return lightDarkButton;
    }
}
