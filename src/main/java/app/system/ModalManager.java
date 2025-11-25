package app.system;

import app.component.Modal;
import app.option.ModalOption;
import raven.modal.ModalDialog;
import raven.modal.component.ModalBorderAction;
import raven.modal.option.Option;

import javax.swing.*;
import java.awt.*;

/**
 * Gestor centralizado para mostrar modales en la aplicación
 * Patrón: Facade + Singleton
 * 
 * Esta clase simplifica el uso de la librería modal-dialog
 * proporcionando métodos convenientes para mostrar modales
 * 
 * @author Sistema Capelli
 */
public class ModalManager {
    
    private static ModalManager instance;
    
    private ModalManager() {
    }
    
    /**
     * Obtiene la instancia única del ModalManager
     * 
     * @return Instancia del ModalManager
     */
    public static ModalManager getInstance() {
        if (instance == null) {
            instance = new ModalManager();
        }
        return instance;
    }
    
    /**
     * Muestra un modal con las opciones por defecto
     * 
     * @param parent Componente padre
     * @param modal Modal a mostrar
     */
    public static void showModal(Component parent, Modal modal) {
        showModal(parent, modal, ModalOption.getDefault());
    }
    
    /**
     * Muestra un modal con opciones personalizadas
     * 
     * @param parent Componente padre
     * @param modal Modal a mostrar
     * @param modalOption Opciones del modal
     */
    public static void showModal(Component parent, Modal modal, ModalOption modalOption) {
        showModal(parent, modal, modalOption, null);
    }
    
    /**
     * Muestra un modal con opciones personalizadas y un ID
     * 
     * @param parent Componente padre
     * @param modal Modal a mostrar
     * @param modalOption Opciones del modal
     * @param id Identificador único del modal (opcional)
     */
    public static void showModal(Component parent, Modal modal, ModalOption modalOption, String id) {
        Option option = modalOption.toOption();
        
        if (id != null) {
            ModalDialog.showModal(parent, modal, option, id);
        } else {
            ModalDialog.showModal(parent, modal, option);
        }
    }
    
    /**
     * Cierra un modal por su ID
     * 
     * @param id Identificador del modal
     */
    public static void closeModal(String id) {
        if (id != null && ModalDialog.isIdExist(id)) {
            ModalDialog.closeModal(id);
        }
    }
    
    /**
     * Cierra todos los modales abiertos
     */
    public static void closeAllModals() {
        ModalDialog.closeAllModal();
    }
    
    /**
     * Cierra inmediatamente un modal sin animación
     * 
     * @param id Identificador del modal
     */
    public static void closeModalImmediately(String id) {
        if (id != null && ModalDialog.isIdExist(id)) {
            ModalDialog.closeModalImmediately(id);
        }
    }
    
    /**
     * Verifica si existe un modal con el ID especificado
     * 
     * @param id Identificador del modal
     * @return true si existe, false en caso contrario
     */
    public static boolean isModalOpen(String id) {
        return id != null && ModalDialog.isIdExist(id);
    }
    
    /**
     * Muestra un diálogo de confirmación simple
     * 
     * @param parent Componente padre
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     * @param onConfirm Acción a ejecutar si se confirma
     */
    public static void showConfirmDialog(Component parent, String title, String message, Runnable onConfirm) {
        showConfirmDialog(parent, title, message, onConfirm, null);
    }
    
    /**
     * Muestra un diálogo de confirmación con callback de cancelación
     * 
     * @param parent Componente padre
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     * @param onConfirm Acción a ejecutar si se confirma
     * @param onCancel Acción a ejecutar si se cancela
     */
    public static void showConfirmDialog(Component parent, String title, String message, 
                                        Runnable onConfirm, Runnable onCancel) {
        JLabel messageLabel = new JLabel("<html><div style='width:300px;'>" + message + "</div></html>");
        
        raven.modal.component.SimpleModalBorder modal = new raven.modal.component.SimpleModalBorder(
            messageLabel,
            title,
            raven.modal.component.SimpleModalBorder.YES_NO_OPTION,
            (controller, action) -> {
                if (action == raven.modal.component.SimpleModalBorder.YES_OPTION) {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                } else if (action == raven.modal.component.SimpleModalBorder.NO_OPTION || 
                          action == raven.modal.component.SimpleModalBorder.CLOSE_OPTION) {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                }
            }
        );
        
        ModalDialog.showModal(parent, modal, createDefaultOption());
    }
    
    /**
     * Muestra un diálogo informativo
     * 
     * @param parent Componente padre
     * @param title Título del diálogo
     * @param message Mensaje del diálogo
     */
    public static void showInfoDialog(Component parent, String title, String message) {
        JLabel messageLabel = new JLabel("<html><div style='width:300px;'>" + message + "</div></html>");
        
        raven.modal.component.SimpleModalBorder modal = new raven.modal.component.SimpleModalBorder(
            messageLabel,
            title,
            raven.modal.component.SimpleModalBorder.CLOSE_OPTION,
            null
        );
        
        ModalDialog.showModal(parent, modal, createDefaultOption());
    }
    
    /**
     * Crea las opciones por defecto para los modales
     * 
     * @return Opciones configuradas
     */
    private static Option createDefaultOption() {
        Option option = Option.getDefault();
        option.getLayoutOption()
            .setAnimateDistance(0, 0.7f);
        option.setDuration(300);
        return option;
    }
}