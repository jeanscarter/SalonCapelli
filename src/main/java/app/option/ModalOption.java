package app.option;

import raven.modal.option.BorderOption;
import raven.modal.option.LayoutOption;
import raven.modal.option.Location;
import raven.modal.option.Option;

import java.awt.*;

/**
 * Clase de opciones para configurar modales
 * Patrón: Builder + Fluent Interface
 * 
 * Envuelve las opciones de la librería modal-dialog
 * proporcionando una interfaz más simple y específica
 * 
 * @author Sistema Capelli
 */
public class ModalOption {
    
    private final LayoutOption layoutOption;
    private final BorderOption borderOption;
    private Option.BackgroundClickType backgroundClickType;
    private boolean animationEnabled;
    private boolean animationOnClose;
    private boolean closeOnEscape;
    private float opacity;
    private int duration;
    private int sliderDuration;
    
    private ModalOption() {
        this.layoutOption = LayoutOption.getDefault();
        this.borderOption = BorderOption.getDefault();
        this.backgroundClickType = Option.BackgroundClickType.CLOSE_MODAL;
        this.animationEnabled = true;
        this.animationOnClose = true;
        this.closeOnEscape = true;
        this.opacity = 0.5f;
        this.duration = 300;
        this.sliderDuration = 400;
    }
    
    /**
     * Obtiene una instancia con valores por defecto
     * 
     * @return Nueva instancia de ModalOption
     */
    public static ModalOption getDefault() {
        return new ModalOption();
    }
    
    /**
     * Configura la posición horizontal del modal
     * 
     * @param horizontal Posición horizontal
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setHorizontalPosition(Position horizontal) {
        Location h = convertPosition(horizontal);
        layoutOption.setLocation(h, layoutOption.getLocation().getY() instanceof Float ? 
            Location.CENTER : Location.TOP);
        return this;
    }
    
    /**
     * Configura la posición vertical del modal
     * 
     * @param vertical Posición vertical
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setVerticalPosition(Position vertical) {
        Location v = convertPosition(vertical);
        Location h = layoutOption.getHorizontalLocation() != null ? 
            layoutOption.getHorizontalLocation() : Location.CENTER;
        layoutOption.setLocation(h, v);
        return this;
    }
    
    /**
     * Configura la dirección de la animación
     * 
     * @param direction Dirección de animación
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setAnimationDirection(AnimationDirection direction) {
        switch (direction) {
            case LEFT_TO_RIGHT:
                layoutOption.setAnimateDistance(-0.7f, 0);
                break;
            case RIGHT_TO_LEFT:
                layoutOption.setAnimateDistance(0.7f, 0);
                break;
            case TOP_TO_BOTTOM:
                layoutOption.setAnimateDistance(0, -0.7f);
                break;
            case BOTTOM_TO_TOP:
                layoutOption.setAnimateDistance(0, 0.7f);
                break;
        }
        return this;
    }
    
    /**
     * Habilita o deshabilita la animación
     * 
     * @param enabled true para habilitar
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
        return this;
    }
    
    /**
     * Habilita o deshabilita la animación al cerrar
     * 
     * @param enabled true para habilitar
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setAnimationOnClose(boolean enabled) {
        this.animationOnClose = enabled;
        return this;
    }
    
    /**
     * Configura la duración de la animación en milisegundos
     * 
     * @param duration Duración en ms
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setDuration(int duration) {
        this.duration = duration;
        return this;
    }
    
    /**
     * Configura la duración de la animación de slider
     * 
     * @param duration Duración en ms
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setSliderDuration(int duration) {
        this.sliderDuration = duration;
        return this;
    }
    
    /**
     * Configura si se cierra al presionar ESC
     * 
     * @param close true para cerrar con ESC
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setCloseOnEscape(boolean close) {
        this.closeOnEscape = close;
        return this;
    }
    
    /**
     * Configura el margen del modal
     * 
     * @param margin Margen en píxeles
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setMargin(int margin) {
        layoutOption.setMargin(margin);
        return this;
    }
    
    /**
     * Configura el redondeo de las esquinas
     * 
     * @param round Radio de redondeo
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setRoundness(int round) {
        borderOption.setRound(round);
        return this;
    }
    
    /**
     * Configura el padding interno del modal
     * 
     * @param padding Padding en píxeles
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setPadding(int padding) {
        layoutOption.setBackgroundPadding(padding);
        return this;
    }
    
    /**
     * Configura la opacidad del fondo
     * 
     * @param opacity Opacidad (0.0 a 1.0)
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setOpacity(float opacity) {
        this.opacity = opacity;
        return this;
    }
    
    /**
     * Configura el comportamiento al hacer clic en el fondo
     * 
     * @param clickOutside true para cerrar al hacer clic fuera
     * @return Esta instancia para encadenamiento
     */
    public ModalOption setCloseOnClickOutside(boolean clickOutside) {
        this.backgroundClickType = clickOutside ? 
            Option.BackgroundClickType.CLOSE_MODAL : 
            Option.BackgroundClickType.BLOCK;
        return this;
    }
    
    /**
     * Convierte esta configuración a la clase Option de la librería
     * 
     * @return Objeto Option configurado
     */
    public Option toOption() {
        Option option = new Option();
        option.setLayoutOption(layoutOption);
        option.setBackgroundClickType(backgroundClickType);
        option.setAnimationEnabled(animationEnabled);
        option.setAnimationOnClose(animationOnClose);
        option.setCloseOnPressedEscape(closeOnEscape);
        option.setOpacity(opacity);
        option.setDuration(duration);
        option.setSliderDuration(sliderDuration);
        
        // Aplicar opciones de borde
        BorderOption.Shadow shadow = BorderOption.Shadow.MEDIUM;
        borderOption.setShadow(shadow);
        
        return option;
    }
    
    /**
     * Convierte Position a Location
     */
    private Location convertPosition(Position position) {
        switch (position) {
            case LEFT:
                return Location.LEFT;
            case RIGHT:
                return Location.RIGHT;
            case TOP:
                return Location.TOP;
            case BOTTOM:
                return Location.BOTTOM;
            case CENTER:
            default:
                return Location.CENTER;
        }
    }
    
    /**
     * Enum para posiciones del modal
     */
    public enum Position {
        LEFT, CENTER, RIGHT, TOP, BOTTOM
    }
    
    /**
     * Enum para direcciones de animación
     */
    public enum AnimationDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP
    }
}