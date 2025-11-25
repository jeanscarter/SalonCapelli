package app.component;

import app.option.ModalOption;
import app.system.ModalManager;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Contenedor que maneja la presentación y animación del modal
 * Patrón: Facade
 */
public class ModalContainer extends JComponent {
    
    private final String id;
    private final Component owner;
    private final ModalOption option;
    private final ModalPanel modalPanel;
    private Animator animator;
    private boolean showing;
    private float animate = 1f;
    private ActionListener escapeAction;
    
    public ModalContainer(ModalManager.ModalContainerLayer containerLayer, Component owner, ModalOption option, String id) {
        this.owner = owner;
        this.option = option;
        this.id = id;
        this.modalPanel = new ModalPanel(this, option);
        init();
    }
    
    private void init() {
        setLayout(new ModalLayout(option));
        setOpaque(false);
        add(modalPanel);
    }
    
    public void initModal(Modal modal) {
        modalPanel.initModal(modal);
        modal.setController(this::closeModal);
    }
    
    public void showModal() {
        if (!option.isAnimationEnabled()) {
            showing = true;
            modalPanel.modalOpened();
            return;
        }
        
        if (animator == null) {
            animator = new Animator(option.getDuration(), new Animator.TimingTarget() {
                @Override
                public void timingEvent(float v) {
                    animate = showing ? v : 1f - v;
                    repaint();
                    revalidate();
                }
                
                @Override
                public void end() {
                    if (showing) {
                        modalPanel.modalOpened();
                    } else {
                        removeModal();
                    }
                }
            });
            animator.setInterpolator(CubicBezierEasing.STANDARD_EASING);
        }
        
        showing = true;
        animate = 0f;
        installOptions();
        animator.start();
    }
    
    public void closeModal() {
        if (!showing) return;
        
        if (option.isAnimationEnabled()) {
            showing = false;
            animator.start();
        } else {
            removeModal();
        }
    }
    
    private void removeModal() {
        uninstallOptions();
        ModalManager.closeModal(id);
    }
    
    private void installOptions() {
        if (option.isCloseOnEscape()) {
            escapeAction = e -> closeModal();
            registerKeyboardAction(escapeAction, 
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        
        if (option.isCloseOnClickOutside()) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getSource() == ModalContainer.this) {
                        closeModal();
                    }
                }
            });
        }
    }
    
    private void uninstallOptions() {
        if (escapeAction != null) {
            unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        float opacity = option.getBackgroundOpacity() * animate;
        if (opacity > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(opacity));
            g2.setColor(option.getBackgroundColor());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
    
    public String getId() {
        return id;
    }
    
    public float getAnimate() {
        return animate;
    }
    
    /**
     * Layout interno para el contenedor modal
     */
    private static class ModalLayout implements LayoutManager {
        private final ModalOption option;
        
        public ModalLayout(ModalOption option) {
            this.option = option;
        }
        
        @Override
        public void addLayoutComponent(String name, Component comp) {}
        
        @Override
        public void removeLayoutComponent(Component comp) {}
        
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }
        
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }
        
        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0) return;
            
            Component comp = parent.getComponent(0);
            Dimension size = comp.getPreferredSize();
            
            int x = calculateX(parent, size);
            int y = calculateY(parent, size);
            
            comp.setBounds(x, y, size.width, size.height);
        }
        
        private int calculateX(Container parent, Dimension size) {
            switch (option.getHorizontalPosition()) {
                case LEFT:
                    return option.getMargin();
                case RIGHT:
                    return parent.getWidth() - size.width - option.getMargin();
                case CENTER:
                default:
                    return (parent.getWidth() - size.width) / 2;
            }
        }
        
        private int calculateY(Container parent, Dimension size) {
            switch (option.getVerticalPosition()) {
                case TOP:
                    return option.getMargin();
                case BOTTOM:
                    return parent.getHeight() - size.height - option.getMargin();
                case CENTER:
                default:
                    return (parent.getHeight() - size.height) / 2;
            }
        }
    }
}