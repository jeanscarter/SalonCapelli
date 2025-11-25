package app.component;

import app.option.ModalOption;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel decorador para el contenido del modal
 * Patrón: Decorator
 */
public class ModalPanel extends JPanel {
    
    private final ModalContainer container;
    private final ModalOption option;
    private Modal modal;
    
    public ModalPanel(ModalContainer container, ModalOption option) {
        this.container = container;
        this.option = option;
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        setOpaque(true);
        
        // Aplicar estilo con sombra y bordes redondeados
        putClientProperty(FlatClientProperties.STYLE, "" +
            "arc:" + option.getRoundness() + ";" +
            "background:$Panel.background");
        
        // Borde con sombra
        setBorder(new EmptyBorder(option.getPadding(), option.getPadding(), 
                                  option.getPadding(), option.getPadding()));
    }
    
    public void initModal(Modal modal) {
        this.modal = modal;
        if (!modal.isInstalled()) {
            modal.installComponent();
            modal.setInstalled(true);
        }
        add(modal, BorderLayout.CENTER);
    }
    
    public void modalOpened() {
        if (modal != null) {
            SwingUtilities.invokeLater(() -> {
                modal.requestFocus();
                modal.modalOpened();
            });
        }
    }
    
    @Override
    public void paint(Graphics g) {
        float animate = container.getAnimate();
        if (animate < 1) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.SrcOver.derive(animate));
        }
        super.paint(g);
    }
}
