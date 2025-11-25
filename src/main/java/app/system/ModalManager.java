package app.system;

import app.component.Modal;
import app.component.ModalContainer;
import app.option.ModalOption;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor principal de modales siguiendo el patrón Singleton
 * Inspirado en raven.modal.ModalDialog
 * 
 * @author Sistema Capelli
 */
public class ModalManager {
    
    private static ModalManager instance;
    private final Integer LAYER = JLayeredPane.MODAL_LAYER + 1;
    private final Map<RootPaneContainer, ModalContainerLayer> map;
    private ModalOption defaultOption;
    
    private ModalManager() {
        map = new HashMap<>();
        defaultOption = ModalOption.getDefault();
    }
    
    private static ModalManager getInstance() {
        if (instance == null) {
            instance = new ModalManager();
        }
        return instance;
    }
    
    /**
     * Muestra un modal con las opciones por defecto
     */
    public static void showModal(Component owner, Modal modal) {
        showModal(owner, modal, getDefaultOption());
    }
    
    /**
     * Muestra un modal con opciones personalizadas
     */
    public static void showModal(Component owner, Modal modal, ModalOption option) {
        showModal(owner, modal, option, null);
    }
    
    /**
     * Muestra un modal con ID específico
     */
    public static void showModal(Component owner, Modal modal, ModalOption option, String id) {
        if (getInstance().isIdExist(id)) {
            throw new IllegalArgumentException("El ID '" + id + "' ya existe");
        }
        
        SwingUtilities.invokeLater(() -> {
            ModalOption copyOption = option == getDefaultOption() ? option.copy() : option;
            getInstance().getModalContainer(owner).addModal(owner, modal, copyOption, id);
        });
    }
    
    /**
     * Cierra un modal por ID
     */
    public static void closeModal(String id) {
        getInstance().getModalContainerById(id).closeModal(id);
    }
    
    /**
     * Cierra todos los modales
     */
    public static void closeAllModal() {
        getInstance().map.values().forEach(ModalContainerLayer::closeAllModal);
    }
    /**
     * Verifica si existe un ID
     */
    
    /**
     * Establece las opciones por defecto
     */
    public static void setDefaultOption(ModalOption option) {
        getInstance().defaultOption = option;
    }
    
    /**
     * Obtiene las opciones por defecto
     */
    public static ModalOption getDefaultOption() {
        return getInstance().defaultOption;
    }
    
    /**
     * Crea una nueva instancia de opciones
     */
    public static ModalOption createOption() {
        return getInstance().defaultOption.copy();
    }
    
    /**
     * Obtiene el RootPaneContainer del componente
     */
    protected static RootPaneContainer getRootPaneContainer(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("El componente padre no puede ser null");
        }
        if (component instanceof JFrame || component instanceof JDialog) {
            return (RootPaneContainer) component;
        }
        return getRootPaneContainer(component.getParent());
    }
    
    /**
     * Verifica si existe un ID en el mapa
     */
    private boolean isIdExist(String id) {
        for (ModalContainerLayer con : map.values()) {
            if (con.checkId(id)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Obtiene el contenedor modal por ID
     */
    private ModalContainerLayer getModalContainerById(String id) {
        for (ModalContainerLayer con : map.values()) {
            if (con.checkId(id)) {
                return con;
            }
        }
        throw new IllegalArgumentException("ID '" + id + "' no encontrado");
    }
    
    /**
     * Obtiene o crea el contenedor modal para una ventana
     */
    private ModalContainerLayer getModalContainer(Component owner) {
        RootPaneContainer rootPaneContainer = getRootPaneContainer(owner);
        ModalContainerLayer modalContainerLayer;
        
        if (map.containsKey(rootPaneContainer)) {
            modalContainerLayer = map.get(rootPaneContainer);
        } else {
            JLayeredPane windowLayeredPane = rootPaneContainer.getLayeredPane();
            
            modalContainerLayer = new ModalContainerLayer(rootPaneContainer);
            windowLayeredPane.add(modalContainerLayer.getLayeredPane(), LAYER);
            
            // Layout personalizado
            LayoutManager oldLayout = windowLayeredPane.getLayout();
            FrameModalLayout frameModalLayout = new FrameModalLayout(
                modalContainerLayer.getLayeredPane(), 
                rootPaneContainer.getContentPane()
            );
            windowLayeredPane.setLayout(frameModalLayout);
            windowLayeredPane.doLayout();
            
            map.put(rootPaneContainer, modalContainerLayer);
        }
        
        return modalContainerLayer;
    }
    
    /**
     * Layout personalizado para el frame modal
     */
    private static class FrameModalLayout implements LayoutManager {
        private final Component modalLayer;
        private final Component contentPane;
        
        public FrameModalLayout(Component modalLayer, Component contentPane) {
            this.modalLayer = modalLayer;
            this.contentPane = contentPane;
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
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = contentPane.getY();
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = contentPane.getHeight();
                
                modalLayer.setBounds(x, y, width, height);
                contentPane.setBounds(0, y, width, height);
            }
        }
    }
    
    /**
     * Clase interna para el contenedor de capas modales
     */
    static class ModalContainerLayer {
        private final RootPaneContainer rootPaneContainer;
        private final JLayeredPane layeredPane;
        private final Map<String, ModalContainer> containers;
        
        public ModalContainerLayer(RootPaneContainer rootPaneContainer) {
            this.rootPaneContainer = rootPaneContainer;
            this.layeredPane = new JLayeredPane();
            this.containers = new HashMap<>();
            layeredPane.setLayout(new ModalContainerLayout());
        }
        
        public void addModal(Component owner, Modal modal, ModalOption option, String id) {
            ModalContainer modalContainer = new ModalContainer(this, owner, option, id);
            layeredPane.add(modalContainer, JLayeredPane.MODAL_LAYER, 0);
            modalContainer.initModal(modal);
            modal.setId(id);
            containers.put(id, modalContainer);
            modalContainer.showModal();
            layeredPane.setVisible(true);
        }
        
        public void closeModal(String id) {
            ModalContainer container = containers.get(id);
            if (container != null) {
                container.closeModal();
                layeredPane.remove(container);
                containers.remove(id);
                if (containers.isEmpty()) {
                    layeredPane.setVisible(false);
                }
            }
        }
        
        public void closeAllModal() {
            containers.values().forEach(ModalContainer::closeModal);
            layeredPane.removeAll();
            containers.clear();
            layeredPane.setVisible(false);
        }
        
        public boolean checkId(String id) {
            return containers.containsKey(id);
        }
        
        public JLayeredPane getLayeredPane() {
            return layeredPane;
        }
    }
    
    /**
     * Layout para el contenedor modal
     */
    private static class ModalContainerLayout implements LayoutManager {
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
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int x = insets.left;
                int y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int height = parent.getHeight() - (insets.top + insets.bottom);
                
                for (Component comp : parent.getComponents()) {
                    if (comp instanceof ModalContainer) {
                        comp.setBounds(x, y, width, height);
                    }
                }
            }
        }
    }
}