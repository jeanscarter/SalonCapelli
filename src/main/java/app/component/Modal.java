package app.component;

import javax.swing.*;
import raven.modal.listener.ModalController;

/**
 * Clase base para todos los componentes modales
 * Patrón: Template Method
 */
public abstract class Modal extends JPanel {
    
    private String id;
    private boolean installed;
    private ModalController controller;
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public boolean isInstalled() {
        return installed;
    }
    
    public void setInstalled(boolean installed) {
        this.installed = installed;
    }
    
    public ModalController getController() {
        return controller;
    }
    
    public void setController(ModalController controller) {
        this.controller = controller;
    }
    
    /**
     * Método para instalar componentes - Template Method
     */
    public void installComponent() {
        // Implementación por defecto vacía
    }
    
    /**
     * Hook method - se ejecuta cuando el modal se abre
     */
    protected void modalOpened() {
        // Implementación por defecto vacía
    }
}