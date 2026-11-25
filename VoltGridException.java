/**
 * MEMBER 1 - our own CHECKED exception (extends Exception), so callers must catch it or declare "throws".
 * The second constructor keeps the original error as the "cause" (EXCEPTION CHAINING).
 */
public class VoltGridException extends Exception {
    public VoltGridException(String message) {
        super(message);
    }

    public VoltGridException(String message, Throwable cause) {
        super(message, cause);
    }
}
