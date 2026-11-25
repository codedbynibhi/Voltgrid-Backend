import java.sql.*;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * One charging session. FR-19 runs it, FR-20 exposes live metrics via snapshot(),
 * FR-21 runs it on a background thread, FR-22 auto-saves it when it ends.
 */
public final class ChargingSession implements Runnable {
    public static final class Snapshot {
        public final double soc, targetSoc, powerKw, energyKwh, cost, elapsedSec, etaSec;
        public final String status, stationName;
        Snapshot(double soc, double targetSoc, double powerKw, double energyKwh, double cost,
                 double elapsedSec, double etaSec, String status, String stationName) {
            this.soc = soc; this.targetSoc = targetSoc; this.powerKw = powerKw; this.energyKwh = energyKwh;
            this.cost = cost; this.elapsedSec = elapsedSec; this.etaSec = etaSec; this.status = status; this.stationName = stationName;
        }
        public String line() {
            return String.format(Locale.US, "%s %5.1f%% -> %.0f%% | %s | %s | %s | elapsed %s | ETA %s",
                    Fmt.bar(soc, 20), soc, targetSoc, Fmt.kw(powerKw), Fmt.kwh(energyKwh), Fmt.money(cost),
                    Fmt.hms(elapsedSec), Fmt.hms(etaSec));
        }
    }

    private final int userId;
    private final Models.Station station;
    private final Models.Vehicle vehicle;
    private final double targetSoc, startSoc, speed;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private final Thread thread;

    private double soc, energyKwh, cost, simSeconds, powerKw;
    private String status = "RUNNING";
    private volatile boolean stopRequested;
    private boolean saved;

    ChargingSession(int userId, Models.Station station, Models.Vehicle vehicle, double targetSoc, double speed) {
        this.userId = userId; this.station = station; this.vehicle = vehicle;
        this.targetSoc = targetSoc; this.speed = speed;
        this.startSoc = vehicle.soc; this.soc = vehicle.soc;
        this.thread = new Thread(this, "charging-" + userId);
        this.thread.setDaemon(true);
    }

    void begin() { thread.start(); }
    public boolean isRunning() { return thread.isAlive(); }
    public Models.Station station() { return station; }

    /** Stop charging (user pressed stop). The session is auto-saved with what was delivered so far. */
    public void stopAndWait(long millis) {
        stopRequested = true;
        thread.interrupt();
        try { thread.join(millis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    @Override public void run() {
        boolean completed = false;
        try {
            double limit = Math.min(station.powerKw, vehicle.maxKw);
            while (!stopRequested) {
                completed = advance(limit);
                if (completed) break;
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
        } finally {
            finish(completed ? "COMPLETED" : "STOPPED");
        }
    }

    /** One tick = `speed` simulated seconds. Returns true when the target has been reached. */
    private synchronized boolean advance(double limitKw) {
        double p = ChargingService.powerAt(soc, limitKw);
        double e = p * speed / 3600.0;
        double dt = speed;
        double remaining = (targetSoc - soc) / 100.0 * vehicle.batteryKwh;
        if (e >= remaining) { dt = remaining / p * 3600.0; e = remaining; }
        soc = Math.min(100, soc + e / vehicle.batteryKwh * 100.0);
        energyKwh += e;
        cost += e * station.pricePerKwh;
        simSeconds += dt;
        powerKw = p;
        return soc >= targetSoc - 1e-9;
    }

    /** FR-20: live metrics. */
    public synchronized Snapshot snapshot() {
        double eta = 0;
        if (status.equals("RUNNING") && soc < targetSoc) {
            Models.Vehicle now = new Models.Vehicle(vehicle.id, userId, vehicle.model, vehicle.batteryKwh,
                    vehicle.maxKw, vehicle.effKwhPerKm, soc);
            eta = ChargingService.estimate(station, now, soc, targetSoc).seconds;
        }
        return new Snapshot(soc, targetSoc, powerKw, energyKwh, cost, simSeconds, eta, status, station.name);
    }

    /** FR-22: auto-save to charging_sessions and update the vehicle's charge, then toast. */
    private synchronized void finish(String finalStatus) {
        if (saved) return;
        saved = true;
        status = finalStatus;
        ChargingService.release(userId, this);

        if (energyKwh < 0.001) {
            Notify.toast("info", "Charging stopped before any energy was delivered. Nothing saved.");
            return;
        }
        double costRounded = Math.round(cost * 100.0) / 100.0;
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO charging_sessions (user_id, station_id, session_date, start_soc, end_soc, target_soc,"
                        + " energy_kwh, cost, duration_sec, status) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, userId); ps.setInt(2, station.id); ps.setTimestamp(3, Timestamp.valueOf(startedAt));
            ps.setDouble(4, startSoc); ps.setDouble(5, soc); ps.setDouble(6, targetSoc);
            ps.setDouble(7, energyKwh); ps.setDouble(8, costRounded); ps.setInt(9, (int) Math.round(simSeconds));
            ps.setString(10, finalStatus);
            ps.executeUpdate();
            Dao.updateSoc(vehicle.id, soc);
            Notify.toast("success", String.format(Locale.US, "Session saved (%s): %s at %s, battery now %s, cost %s.",
                    finalStatus.toLowerCase(), Fmt.kwh(energyKwh), station.name, Fmt.pct(soc), Fmt.money(costRounded)));
        } catch (SQLException ex) {
            Notify.toast("error", "Could not auto-save the session: " + ex.getMessage());
        }
    }
}
