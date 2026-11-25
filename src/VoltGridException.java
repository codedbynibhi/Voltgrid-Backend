/**
 * Base class of all our own CHECKED exceptions (extends Exception, so callers must catch or declare it).
 * Syllabus: custom exceptions, checked vs unchecked, exception chaining (the "cause").
 */
public class VoltGridException extends Exception {
    private static final long serialVersionUID = 1L;

    public VoltGridException(String message) {
        super(message);
    }

    public VoltGridException(String message, Throwable cause) {
        super(message, cause);
    }
}
