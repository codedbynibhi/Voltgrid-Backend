/** FR-23: simulate a drive. Uses battery energy at the vehicle's efficiency and updates its charge. */
public final class DriveSimulator {
    public static final double RESERVE_PERCENT = 5.0;   // the simulator will not drain below this
    public static final double LOW_BATTERY_PERCENT = 15.0;
    public static final String[] STYLES = {"Eco", "Normal", "Sport"};
    private static final double[] MULTIPLIER = {0.85, 1.00, 1.20};

    private DriveSimulator() {}

    /** What a successful drive produced. Problems are reported with exceptions instead. */
    public static final class Result {
        public final String message;
        public final double energyKwh, newSoc, rangeLeftKm;
        Result(String message, double energyKwh, double newSoc, double rangeLeftKm) {
            this.message = message; this.energyKwh = energyKwh; this.newSoc = newSoc; this.rangeLeftKm = rangeLeftKm;
        }
    }

    /** Distance the vehicle can still cover while keeping the reserve. */
    public static double rangeKm(Models.Vehicle v, double soc, int style) {
        double usableKwh = Math.max(0, soc - RESERVE_PERCENT) / 100.0 * v.batteryKwh;
        return usableKwh / (v.effKwhPerKm * MULTIPLIER[style]);
    }

    /**
     * Drives the vehicle. Each problem is its own exception type; the caller decides how to react.
     * ValidationException and InsufficientChargeException are subclasses of VoltGridException.
     */
    public static Result drive(Models.Vehicle v, double km, int style)
            throws ValidationException, InsufficientChargeException, VoltGridException {
        if (ChargingService.active(v.userId) != null)
            throw new ValidationException("The vehicle is charging. Stop the session before driving.");
        if (km <= 0 || km > 1000)
            throw new ValidationException("Distance must be between 0 and 1000 km.");

        double needKwh = km * v.effKwhPerKm * MULTIPLIER[style];
        double maxKm = rangeKm(v, v.soc, style);
        if (needKwh > Math.max(0, v.soc - RESERVE_PERCENT) / 100.0 * v.batteryKwh) {
            throw new InsufficientChargeException(String.format("Not enough charge for %.1f km (%s style). You can drive about "
                    + "%.1f km before hitting the %.0f%% reserve. Charge first.", km, STYLES[style], maxKm, RESERVE_PERCENT), maxKm);
        }
        double newSoc = v.soc - needKwh / v.batteryKwh * 100.0;
        try {
            Dao.updateSoc(v.id, newSoc);
        } catch (java.sql.SQLException e) {
            throw new VoltGridException("Could not save the new charge level.", e);   // chain the original error
        }
        double left = rangeKm(v, newSoc, style);
        if (newSoc < LOW_BATTERY_PERCENT) Notify.toast("warning", String.format("Low battery: %.1f%%.", newSoc));
        return new Result(String.format("Drove %.1f km (%s). Used %.2f kWh.", km, STYLES[style], needKwh),
                needKwh, newSoc, left);
    }
}
