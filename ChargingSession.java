import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * MEMBER 3 - charging runs on its OWN THREAD, so the menu stays usable while the car charges.
 *  - Runnable              : the job the thread runs (run method)
 *  - volatile stopRequested: the menu thread writes it, the charging thread must see the newest value
 *  - synchronized methods  : the charging thread updates the numbers, the menu thread reads them
 *  - when the session ends it saves itself to the database
 */
public class ChargingSession implements Runnable {
    public static final double SPEED = 120.0;                    // 1 real second = 120 simulated seconds

    private static ChargingSession current;                      // only one session at a time (one user at the keyboard)

    private final User user;
    private final Station station;
    private final double targetSoc, startSoc;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private final Thread thread;

    private double soc, energyKwh, cost;
    private volatile boolean stopRequested = false;
    private boolean saved = false;

    private ChargingSession(User user, Station station, double targetSoc) {
        this.user = user;
        this.station = station;
        this.targetSoc = targetSoc;
        this.startSoc = user.getSoc();
        this.soc = user.getSoc();
        this.thread = new Thread(this, "charging-thread");
        this.thread.setDaemon(true);                             // daemon: does not keep the program alive
    }

    // ---------------- static helpers (is someone charging?) ----------------

    /** Checks everything, then starts the background thread. */
    public static synchronized void start(User u, Station s, double target) throws VoltGridException {
        if (current != null) {
            throw new IllegalStateException("A charging session is already running.");     // unchecked
        }
        if (!s.isAvailable()) {
            throw new VoltGridException(s.getName() + " is not available right now.");     // checked
        }
        if (target > 100 || target <= u.getSoc()) {
            throw new VoltGridException("Target must be above the current charge ("
                    + String.format("%.1f", u.getSoc()) + "%) and at most 100%.");
        }
        current = new ChargingSession(u, s, target);
        current.thread.start();                                  // start() creates the new thread, which calls run()
    }

    /** The running session, or null. */
    public static synchronized ChargingSession getCurrent() {
        return current;
    }

    private static synchronized void clear() {
        current = null;
    }

    /** Called when the program exits, so a running session is saved instead of lost. */
    public static void stopRunning() {
        ChargingSession s = getCurrent();
        if (s != null) s.stopAndWait();
    }

    // ---------------- the thread's job ----------------

    @Override
    public void run() {
        double limitKw = Math.min(station.getPowerKw(), user.getMaxKw());   // the slower of car and charger wins
        boolean completed = false;
        try {
            while (!stopRequested) {
                completed = tick(limitKw);
                if (completed) break;
                Thread.sleep(1000);                              // wait one real second
            }
        } catch (InterruptedException e) {
            // the user asked to stop: sleep() was interrupted, so we just fall through
        } finally {
            finish(completed ? "COMPLETED" : "STOPPED");         // finally: always saves, whatever happened
        }
    }

    /** One step = SPEED simulated seconds of charging. Returns true when the target is reached. */
    private synchronized boolean tick(double limitKw) {
        double wanted = limitKw * SPEED / 3600.0;                // energy (kWh) in this step
        double remaining = (targetSoc - soc) / 100.0 * user.getBatteryKwh();
        double e = Math.min(wanted, remaining);                  // do not go past the target
        soc += e / user.getBatteryKwh() * 100.0;
        energyKwh += e;
        cost += e * station.getPricePerKwh();
        return soc >= targetSoc - 0.0001;
    }

    // ---------------- called by the menu thread ----------------

    public void stopAndWait() {
        stopRequested = true;
        thread.interrupt();                                      // wake the thread if it is sleeping
        try {
            thread.join(5000);                                   // wait (max 5 s) until it has finished and saved
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Live metrics as one line of text. synchronized, so we never read half-updated numbers. */
    public synchronized String statusLine() {
        return String.format("%s %5.1f%% -> %.0f%% | %.2f kWh | Rs.%.2f | at %s",
                TextUtil.bar(soc, 20), soc, targetSoc, energyKwh, cost, station.getName());
    }

    /** Save the session and the new battery level. Runs once, when the session ends. */
    private synchronized void finish(String status) {
        if (saved) return;
        saved = true;
        clear();
        if (energyKwh < 0.001) {
            System.out.println("\n[INFO] Charging stopped before any energy was delivered. Nothing saved.");
            return;
        }
        double costRounded = Math.round(cost * 100.0) / 100.0;
        try {
            Dao.saveSession(user.getId(), station.getId(), startedAt, startSoc, soc, energyKwh, costRounded, status);
            Dao.updateSoc(user.getId(), soc);
            user.setSoc(soc);
            System.out.println(String.format("\n[SAVED] Session %s at %s: %.2f kWh, battery now %.1f%%, cost Rs.%.2f",
                    status.toLowerCase(), station.getName(), energyKwh, soc, costRounded));
        } catch (SQLException e) {
            System.out.println("\n[ERROR] Could not save the session: " + e.getMessage());
        }
    }
}
