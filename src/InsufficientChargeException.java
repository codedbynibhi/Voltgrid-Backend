/** Thrown when the battery does not hold enough charge for the requested drive. */
public class InsufficientChargeException extends VoltGridException {
    private static final long serialVersionUID = 1L;

    private final double possibleKm;

    public InsufficientChargeException(String message, double possibleKm) {
        super(message);
        this.possibleKm = possibleKm;
    }

    public double getPossibleKm() {
        return possibleKm;
    }
}
