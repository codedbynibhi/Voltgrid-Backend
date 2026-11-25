import java.util.ArrayList;
import java.util.List;

/** Thrown when user input breaks one or more rules. Carries every message, not just the first. */
public class ValidationException extends VoltGridException {
    private static final long serialVersionUID = 1L;

    public final ArrayList<String> errors = new ArrayList<>();

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors.addAll(errors);
    }

    public ValidationException(String error) {
        super(error);
        this.errors.add(error);
    }
}
