/** FR-23: simulate a drive. Uses battery energy at the vehicle's efficiency and updates its charge. */
public final class DriveSimulator {
    public static final double RESERVE_PERCENT = 5.0;   // the simulator will not drain below this
    public static final double LOW_BATTERY_PERCENT = 15.0;
    public static final String[] STYLES = {"Eco", "Normal", "Sport"};
    private static final double[] MULTIPLIER = {0.85, 1.00, 1.20};

    private DriveSimulator() {}

    public static final class Result {
        public final boolean ok;
        public final String message;
        public final double energyKwh, newSoc, rangeLeftKm;
        Result(boolean ok, String message, double energyKwh, double newSoc, double rangeLeftKm) {
            this.ok = ok; this.message = message; this.energyKwh = energyKwh; this.newSoc = newSoc; this.rangeLeftKm = rangeLeftKm;
        }
    }

    /** Distance the vehicle can still cover while keeping the reserve. */
    public static double rangeKm(Models.Vehicle v, double soc, int style) {
        double usableKwh = Math.max(0, soc - RESERVE_PERCENT) / 100.0 * v.batteryKwh;
        return usableKwh / (v.effKwhPerKm * MULTIPLIER[style]);
    }

    public static Result drive(Models.Vehicle v, double km, int style) {
        if (ChargingService.active(v.userId) != null)
            return new Result(false, "The vehicle is charging. Stop the session before driving.", 0, v.soc, 0);
        if (km <= 0 || km > 1000)
            return new Result(false, "Distance must be between 0 and 1000 km.", 0, v.soc, 0);

        double needKwh = km * v.effKwhPerKm * MULTIPLIER[style];
        double maxKm = rangeKm(v, v.soc, style);
        if (needKwh > Math.max(0, v.soc - RESERVE_PERCENT) / 100.0 * v.batteryKwh) {
            return new Result(false, String.format("Not enough charge for %.1f km (%s style). You can drive about %.1f km "
                    + "before hitting the %.0f%% reserve. Charge first.", km, STYLES[style], maxKm, RESERVE_PERCENT),
                    needKwh, v.soc, maxKm);
        }
        double newSoc = v.soc - needKwh / v.batteryKwh * 100.0;
        try {
            Dao.updateSoc(v.id, newSoc);
        } catch (java.sql.SQLException e) {
            return new Result(false, "Could not save the new charge level: " + e.getMessage(), needKwh, v.soc, maxKm);
        }
        double left = rangeKm(v, newSoc, style);
        if (newSoc < LOW_BATTERY_PERCENT) Notify.toast("warning", String.format("Low battery: %.1f%%.", newSoc));
        return new Result(true, String.format("Drove %.1f km (%s). Used %.2f kWh.", km, STYLES[style], needKwh),
                needKwh, newSoc, left);
    }
}
