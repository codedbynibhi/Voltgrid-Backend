/** Thrown when a station cannot be used right now (marked unavailable). */
public class StationUnavailableException extends VoltGridException {
    private static final long serialVersionUID = 1L;

    public StationUnavailableException(String stationName) {
        super(stationName + " is not available right now.");
    }
}
