import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FR-18 Select station and target, FR-19 Run a session, FR-21 Background charging.
 * Holds the charging model (taper above 80%) used by sessions, the cost calculator and previews.
 */
public final class ChargingService {
    public static final double TAPER_START = 80.0;       // % where charging power starts to drop
    public static final double MIN_TAPER_FRACTION = 0.2; // power at 100% as a fraction of full power

    /** Simulated seconds that pass per real second (default 1 real s = 2 simulated minutes). */
    public static volatile double simSpeed = 120.0;

    private static final Map<Integer, ChargingSession> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<Integer, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private ChargingService() {}

    public static final class Selection {
        public final Models.Station station;
        public final double targetSoc;
        Selection(Models.Station station, double targetSoc) { this.station = station; this.targetSoc = targetSoc; }
    }

    public static final class Estimate {
        public final double seconds, energyKwh, cost, avgKw;
        Estimate(double seconds, double energyKwh, double cost) {
            this.seconds = seconds; this.energyKwh = energyKwh; this.cost = cost;
            this.avgKw = seconds > 0 ? energyKwh / (seconds / 3600.0) : 0;
        }
    }

    /** Charging power at a given state of charge: flat up to 80%, then tapering down to 20% of full power. */
    public static double powerAt(double soc, double limitKw) {
        if (soc <= TAPER_START) return limitKw;
        double f = 1.0 - (soc - TAPER_START) / (100.0 - TAPER_START) * (1.0 - MIN_TAPER_FRACTION);
        return limitKw * Math.max(MIN_TAPER_FRACTION, f);
    }

    /** Time, energy and cost to go from one state of charge to another at a station. */
    public static Estimate estimate(Models.Station st, Models.Vehicle v, double fromSoc, double toSoc) {
        double limit = Math.min(st.powerKw, v.maxKw);
        double soc = fromSoc, kwh = 0, sec = 0;
        final double dt = 10.0;
        int guard = 0;
        while (soc < toSoc - 1e-9 && limit > 0 && guard++ < 2_000_000) {
            double p = powerAt(soc, limit);
            double remaining = (toSoc - soc) / 100.0 * v.batteryKwh;
            double e = p * dt / 3600.0;
            if (e >= remaining) { sec += remaining / p * 3600.0; kwh += remaining; break; }
            soc += e / v.batteryKwh * 100.0; kwh += e; sec += dt;
        }
        return new Estimate(sec, kwh, kwh * st.pricePerKwh);
    }

    /** FR-18: validates the choice and remembers it for the user. Returns error messages (empty = ok). */
    public static List<String> select(int userId, Models.Station st, Models.Vehicle v, double targetSoc) {
        List<String> errs = new ArrayList<>();
        if (st == null) { errs.add("Station not found."); return errs; }
        if (v == null) { errs.add("No vehicle profile found."); return errs; }
        if (!st.available) errs.add(st.name + " is not available right now.");
        if (Math.min(st.powerKw, v.maxKw) <= 0) errs.add("Charging power is zero for this station/vehicle pair.");
        if (targetSoc < 1 || targetSoc > 100) errs.add("Target must be between 1% and 100%.");
        else if (targetSoc <= v.soc) errs.add("Target (" + Fmt.pct(targetSoc) + ") must be above the current charge (" + Fmt.pct(v.soc) + ").");
        if (errs.isEmpty()) SELECTIONS.put(userId, new Selection(st, targetSoc));
        return errs;
    }

    /** selectStation(id) from the agreements: what the user picked last. */
    public static Selection selection(int userId) { return SELECTIONS.get(userId); }

    public static ChargingSession active(int userId) { return ACTIVE.get(userId); }

    /** FR-19 + FR-21: starts the session on its own thread so the menu stays usable while charging. */
    public static synchronized ChargingSession start(int userId, Models.Vehicle v) throws IllegalStateException {
        if (ACTIVE.containsKey(userId)) throw new IllegalStateException("A charging session is already running.");
        Selection sel = SELECTIONS.get(userId);
        if (sel == null) throw new IllegalStateException("Select a station and target first.");
        List<String> errs = select(userId, sel.station, v, sel.targetSoc);   // re-check: state may have changed
        if (!errs.isEmpty()) throw new IllegalStateException(String.join(" ", errs));

        ChargingSession s = new ChargingSession(userId, sel.station, v, sel.targetSoc, simSpeed);
        ACTIVE.put(userId, s);
        s.begin();
        return s;
    }

    static void release(int userId, ChargingSession s) { ACTIVE.remove(userId, s); }

    /** Called on JVM shutdown so a running session is auto-saved instead of lost. */
    public static void stopAll() {
        for (ChargingSession s : new ArrayList<>(ACTIVE.values())) s.stopAndWait(3000);
    }
}
