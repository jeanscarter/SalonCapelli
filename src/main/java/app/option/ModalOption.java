package app.option;

import java.awt.*;

/**
 * Configuración del modal siguiendo el patrón Builder
 */
public class ModalOption {
    
    // Posicionamiento
    private Position horizontalPosition = Position.CENTER;
    private Position verticalPosition = Position.CENTER;
    private int margin = 20;
    private int padding = 20;
    
    // Apariencia
    private int roundness = 15;
    private Color backgroundColor = new Color(0, 0, 0, 128);
    private float backgroundOpacity = 0.5f;
    
    // Animación
    private boolean animationEnabled = true;
    private int duration = 300;
    private AnimationDirection animationDirection = AnimationDirection.RIGHT_TO_LEFT;
    
    // Comportamiento
    private boolean closeOnEscape = true;
    private boolean closeOnClickOutside = true;
    
    public static ModalOption getDefault() {
        return new ModalOption();
    }
    
    // Getters
    public Position getHorizontalPosition() {
        return horizontalPosition;
    }
    
    public Position getVerticalPosition() {
        return verticalPosition;
    }
    
    public int getMargin() {
        return margin;
    }
    
    public int getPadding() {
        return padding;
    }
    
    public int getRoundness() {
        return roundness;
    }
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    public float getBackgroundOpacity() {
        return backgroundOpacity;
    }
    
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public AnimationDirection getAnimationDirection() {
        return animationDirection;
    }
    
    public boolean isCloseOnEscape() {
        return closeOnEscape;
    }
    
    public boolean isCloseOnClickOutside() {
        return closeOnClickOutside;
    }
    
    // Setters con patrón Builder
    public ModalOption setHorizontalPosition(Position position) {
        this.horizontalPosition = position;
        return this;
    }
    
    public ModalOption setVerticalPosition(Position position) {
        this.verticalPosition = position;
        return this;
    }
    
    public ModalOption setMargin(int margin) {
        this.margin = margin;
        return this;
    }
    
    public ModalOption setPadding(int padding) {
        this.padding = padding;
        return this;
    }
    
    public ModalOption setRoundness(int roundness) {
        this.roundness = roundness;
        return this;
    }
    
    public ModalOption setBackgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }
    
    public ModalOption setBackgroundOpacity(float opacity) {
        this.backgroundOpacity = opacity;
        return this;
    }
    
    public ModalOption setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
        return this;
    }
    
    public ModalOption setDuration(int duration) {
        this.duration = duration;
        return this;
    }
    
    public ModalOption setAnimationDirection(AnimationDirection direction) {
        this.animationDirection = direction;
        return this;
    }
    
    public ModalOption setCloseOnEscape(boolean close) {
        this.closeOnEscape = close;
        return this;
    }
    
    public ModalOption setCloseOnClickOutside(boolean close) {
        this.closeOnClickOutside = close;
        return this;
    }
    
    /**
     * Copia las opciones
     */
    public ModalOption copy() {
        ModalOption option = new ModalOption();
        option.horizontalPosition = this.horizontalPosition;
        option.verticalPosition = this.verticalPosition;
        option.margin = this.margin;
        option.padding = this.padding;
        option.roundness = this.roundness;
        option.backgroundColor = new Color(this.backgroundColor.getRGB());
        option.backgroundOpacity = this.backgroundOpacity;
        option.animationEnabled = this.animationEnabled;
        option.duration = this.duration;
        option.animationDirection = this.animationDirection;
        option.closeOnEscape = this.closeOnEscape;
        option.closeOnClickOutside = this.closeOnClickOutside;
        return option;
    }
    
    /**
     * Enum para posiciones
     */
    public enum Position {
        LEFT, CENTER, RIGHT, TOP, BOTTOM
    }
    
    /**
     * Enum para dirección de animación
     */
    public enum AnimationDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP,
        FADE
    }
}