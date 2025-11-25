package app.util.validator;

/**
 * Interfaz para validadores
 * Patrón: Strategy
 */
@FunctionalInterface
public interface Validator<T> {
    ValidationResult validate(T value);
    
    /**
     * Combina este validador con otro (Chain of Responsibility)
     */
    default Validator<T> and(Validator<T> other) {
        return value -> {
            ValidationResult first = this.validate(value);
            if (!first.isValid()) {
                return first;
            }
            return other.validate(value);
        };
    }
}