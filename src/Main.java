import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * VoltGrid - Member 3 (Palak): reservation and charging simulator, terminal edition.
 * FR-15, FR-16, FR-18, FR-19, FR-20, FR-21, FR-22, FR-23, FR-24.
 */
public class Main {
    private static Models.User user;

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        try {
            Schema.ensure();
            Runtime.getRuntime().addShutdownHook(new Thread(ChargingService::stopAll));   // FR-22 on Ctrl+C / exit
            if (Dao.countRows("users") == 0 && Input.yesNo("No users found. Load demo data (1 user, 5 stations)?"))
                Dao.seedDemo();
            else if (Dao.countRows("stations") == 0 && Input.yesNo("No stations found. Load demo stations?"))
                Dao.seedDemo();
            login();
            ensureVehicle();
            menuLoop();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("Check that MySQL is running, the 'voltgrid' database exists and the connector jar is in lib/.");
        }
    }

    // ---------- start-up ----------

    /** Stand-in until Member 1's sign-in is merged: identify the user by email. */
    private static void login() throws SQLException {
        System.out.println("=== VoltGrid: reservations and charging simulator ===");
        while (user == null) {
            String email = Input.line("Email: ");
            if (!TextUtil.isValidEmail(email)) { System.out.println("  That does not look like an email address."); continue; }
            user = Dao.findUserByEmail(email.trim());
            if (user == null) System.out.println("  No user with that email.");
        }
        System.out.println("Welcome, " + user.name + ".");
        ActivityLog.add("Logged in as " + user.email);
    }

    /** Vehicle profile normally comes from Member 4's profile; ask once if it is missing. */
    private static void ensureVehicle() throws SQLException {
        if (Dao.getVehicle(user.id) != null) return;
        System.out.println("No vehicle profile yet. Enter your vehicle (this is what Profile will provide later).");
        String model = Input.line("Model [My EV]: ");
        double bat = Input.decimal("Battery capacity kWh [40]: ", 5, 250, 40.0);
        double max = Input.decimal("Max charging speed kW [50]: ", 1, 350, 50.0);
        double soc = Input.decimal("Current charge % [20]: ", 0, 100, 20.0);
        Dao.createVehicle(user.id, model.isEmpty() ? "My EV" : model, bat, max, soc);
    }

    // ---------- menu ----------

    private static void menuLoop() throws SQLException {
        while (true) {
            System.out.println();
            ChargingSession live = ChargingService.active(user.id);
            if (live != null) System.out.println("Charging in background: " + live.snapshot().line());
            System.out.println("1) Reserve a slot            6) Stop charging");
            System.out.println("2) My reservations / cancel  7) Simulate a drive");
            System.out.println("3) Select station + target   8) Cost calculator");
            System.out.println("4) Start charging session    9) Vehicle status");
            System.out.println("5) Live session metrics     10) Stations and schedule");
            System.out.println("11) Reports and history     12) Multithreading demos");
            System.out.println("13) Java concepts lab       14) JDBC lab              0) Exit");
            int choice = Input.integer("> ", 0, 14);
            switch (choice) {
                case 1: reserve(); break;
                case 2: reservations(); break;
                case 3: selectStationAndTarget(); break;
                case 4: startSession(); break;
                case 5: liveMetrics(); break;
                case 6: stopSession(); break;
                case 7: drive(); break;
                case 8: costCalculator(); break;
                case 9: vehicleStatus(); break;
                case 10: ExtraMenu.stations(user); break;
                case 11: ExtraMenu.reports(user); break;
                case 12: ExtraMenu.concurrency(); break;
                case 13: ExtraMenu.concepts(); break;
                case 14: ExtraMenu.jdbc(); break;
                default:
                    if (ChargingService.active(user.id) != null && !Input.yesNo("A session is charging. Exit and save it now?")) break;
                    ChargingService.stopAll();
                    System.out.println("Bye.");
                    return;
            }
        }
    }

    // ---------- helpers ----------

    static void printStations(List<Models.Station> list) {
        System.out.println(String.format("%-3s %-24s %8s %-9s %-7s %10s %7s %-5s %5s", "ID", "Name", "Power", "Type",
                "Plug", "Price/kWh", "Dist", "Free", "Queue"));
        for (Models.Station s : list) {
            Charger charger = Charger.of(s);     // polymorphism: AcCharger or DcCharger decides the plug
            System.out.println(String.format("%-3d %-24s %8s %-9s %-7s %10s %5.1fkm %-5s %5d", s.id, s.name, Fmt.kw(s.powerKw),
                    s.type, charger.connector(), Fmt.money(s.pricePerKwh), s.distanceKm, s.available ? "yes" : "no", s.queue));
        }
    }

    private static Models.Station pickStation() throws SQLException {
        List<Models.Station> list = Dao.listStations();
        if (list.isEmpty()) { System.out.println("No stations in the database."); return null; }
        printStations(list);
        Models.Station s = Dao.getStation(Input.integer("Station ID (0 to go back): ", 0, Integer.MAX_VALUE));
        return s;
    }

    // ---------- FR-15 / FR-16 ----------

    private static void reserve() throws SQLException {
        System.out.println("-- Reserve a slot --");
        Models.Station s = pickStation();
        if (s == null) return;
        LocalDateTime start = Input.dateTime("Start (yyyy-MM-dd HH:mm, or HH:mm for today): ");
        int minutes = Input.integer("Duration in minutes (" + ReservationService.MIN_DURATION + "-"
                + ReservationService.MAX_DURATION + ", steps of " + ReservationService.DURATION_STEP + "): ", 0, 100000);
        try {
            int id = ReservationService.reserve(user.id, s.id, start, minutes);
            ActivityLog.rememberReservation(id);
            ActivityLog.add("Reserved #" + id + " at " + s.name);
            Notify.toast("success", "Reservation #" + id + " confirmed at " + s.name + " from "
                    + start.format(Input.DT) + " for " + minutes + " min.");
        } catch (ValidationException e) {
            System.out.println("Reservation not made:");
            boolean full = false;
            for (String err : e.errors) {
                System.out.println("  - " + err);
                if (err.startsWith("No free slot")) full = true;
            }
            if (full && Input.yesNo("Join the waiting list for " + s.name + "?")) {
                int pos = Waitlist.join(s.id, user.id);
                ActivityLog.add("Joined waiting list at " + s.name);
                Notify.toast("info", "You are number " + pos + " on the waiting list.");
            }
        }
    }

    private static void reservations() throws SQLException {
        System.out.println("-- Your upcoming reservations --");
        List<Models.Booking> list = ReservationService.list(user.id, true);
        if (list.isEmpty()) { System.out.println("None."); return; }
        for (Models.Booking b : list)
            System.out.println(String.format("#%-4d %-24s %s -> %s", b.id, b.stationName, b.start.format(Input.DT), b.end.format(Input.DT)));
        int id = Input.integer("Booking # to cancel (0 to go back): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        Models.Booking chosen = null;
        for (Models.Booking b : list) if (b.id == id) chosen = b;
        if (ReservationService.cancel(user.id, id)) {
            Notify.toast("success", "Reservation #" + id + " cancelled.");
            ActivityLog.add("Cancelled reservation #" + id);
            if (chosen != null) {
                int next = Waitlist.next(chosen.stationId);
                if (next != -1) Notify.toast("info", "A slot at " + chosen.stationName + " is free: user #" + next
                        + " is first on the waiting list and would be notified.");
            }
        }
        else System.out.println("Could not cancel: it must be your own active reservation that has not started yet.");
    }

    // ---------- FR-18 ----------

    private static void selectStationAndTarget() throws SQLException {
        System.out.println("-- Select station and target --");
        Models.Vehicle v = Dao.getVehicle(user.id);
        Models.Station s = pickStation();
        if (s == null) return;
        double target = Input.decimal("Target charge % [80]: ", 0, 100, 80.0);
        List<String> errs = ChargingService.select(user.id, s, v, target);
        if (!errs.isEmpty()) { for (String e : errs) System.out.println("  - " + e); return; }
        ChargingService.Estimate est = ChargingService.estimate(s, v, v.soc, target);
        ActivityLog.add("Selected " + s.name + " target " + target + "%");
        System.out.println(String.format("Selected %s, %s -> %s. Estimate: %s, %s, %s (avg %s).",
                s.name, Fmt.pct(v.soc), Fmt.pct(target), Fmt.hms(est.seconds), Fmt.kwh(est.energyKwh),
                Fmt.money(est.cost), Fmt.kw(est.avgKw)));
        System.out.println("Choose 'Start charging session' to begin.");
    }

    // ---------- FR-19 / FR-21 ----------

    private static void startSession() throws SQLException {
        Models.Vehicle v = Dao.getVehicle(user.id);
        if (ChargingService.selection(user.id) == null) { System.out.println("Select a station and target first (option 3)."); return; }
        double speed = Input.decimal("Simulation speed, simulated seconds per real second [" + (int) ChargingService.simSpeed + "]: ",
                1, 3600, ChargingService.simSpeed);
        ChargingService.simSpeed = speed;
        try {
            ChargingSession s = ChargingService.start(user.id, v);
            ActivityLog.add("Started charging at " + s.station().name);
            Notify.toast("info", "Charging started at " + s.station().name + ". It keeps running in the background; "
                    + "use option 5 for live metrics.");
        } catch (StationUnavailableException e) {
            System.out.println("Cannot start: " + e.getMessage() + " Pick another station (option 3).");
        } catch (VoltGridException | IllegalStateException e) {       // multi-catch: checked and unchecked together
            System.out.println("Cannot start: " + e.getMessage());
        }
    }

    private static void stopSession() {
        ChargingSession s = ChargingService.active(user.id);
        if (s == null) { System.out.println("No charging session is running."); return; }
        s.stopAndWait(5000);   // auto-saves (FR-22)
    }

    // ---------- FR-20 ----------

    private static void liveMetrics() {
        ChargingSession s = ChargingService.active(user.id);
        if (s == null) { System.out.println("No charging session is running."); return; }
        System.out.println("Live metrics. Press Enter to go back (charging keeps running).");
        while (s.isRunning()) {
            System.out.println(s.snapshot().line());
            for (int i = 0; i < 10; i++) {
                try { Thread.sleep(100); } catch (InterruptedException e) { return; }
                if (Input.enterPressed()) return;
            }
        }
    }

    // ---------- FR-23 ----------

    private static void drive() throws SQLException {
        System.out.println("-- Simulate a drive --");
        Models.Vehicle v = Dao.getVehicle(user.id);
        System.out.println(String.format("Charge %s, about %.0f km of range (normal style, %.0f%% reserve kept).",
                Fmt.pct(v.soc), DriveSimulator.rangeKm(v, v.soc, 1), DriveSimulator.RESERVE_PERCENT));
        double km = Input.decimal("Distance km: ", 0.1, 1000, null);
        System.out.println("Style: 1) Eco  2) Normal  3) Sport");
        int style = Input.integer("> ", 1, 3) - 1;
        try {
            DriveSimulator.Result r = DriveSimulator.drive(v, km, style);
            System.out.println(r.message);
            System.out.println(String.format("Charge now %s, about %.0f km of range left.", Fmt.pct(r.newSoc), r.rangeLeftKm));
            ActivityLog.add(String.format("Drove %.1f km", km));
        } catch (InsufficientChargeException e) {
            System.out.println(e.getMessage());
            System.out.println(String.format("(Try at most %.0f km, or charge first.)", e.getPossibleKm()));
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        } catch (VoltGridException e) {
            System.out.println(e.getMessage() + " Cause: " + e.getCause());
        } finally {
            System.out.println("-- drive request finished --");     // finally always runs
        }
    }

    // ---------- FR-24 ----------

    private static void costCalculator() throws SQLException {
        System.out.println("-- Cost calculator --");
        Models.Vehicle v = Dao.getVehicle(user.id);
        List<Models.Station> stations = Dao.listStations();
        if (stations.isEmpty()) { System.out.println("No stations in the database."); return; }
        double from = Input.decimal(String.format("From charge %% [%.0f]: ", v.soc), 0, 100, v.soc);
        double to = Input.decimal("To charge % [80]: ", 0, 100, 80.0);
        String err = CostCalculator.validate(v, from, to);
        if (err != null) { System.out.println(err); return; }
        System.out.println("Pricing plan: 1) Flat   2) Peak / off-peak (evening x1.25, night x0.80)");
        Tariff plan = Input.integer("> ", 1, 2) == 1 ? new FlatTariff() : new PeakTariff();
        int hour = plan instanceof FlatTariff ? 0 : Input.integer("Start hour of day (0-23): ", 0, 23);
        System.out.println("Using: " + plan.describe());
        System.out.println(String.format("%-3s %-24s %9s %10s %10s %10s", "ID", "Station", "Energy", "Cost", "Time", "Avg power"));
        for (CostCalculator.Row r : CostCalculator.rankAll(stations, v, from, to, plan, hour))
            System.out.println(String.format("%-3d %-24s %9s %10s %10s %10s%s", r.station.id, r.station.name,
                    Fmt.kwh(r.estimate.energyKwh), Fmt.money(r.estimate.cost), Fmt.hms(r.estimate.seconds),
                    Fmt.kw(r.estimate.avgKw), r.station.available ? "" : "  (not available now)"));
        System.out.println("Cheapest first. Time uses the lower of station power and your vehicle's max speed, tapering above "
                + (int) ChargingService.TAPER_START + "%.");
    }

    // ---------- status ----------

    private static void vehicleStatus() throws SQLException {
        Models.Vehicle v = Dao.getVehicle(user.id);
        ChargingSession s = ChargingService.active(user.id);
        double soc = s != null ? s.snapshot().soc : v.soc;
        System.out.println(String.format("%s | battery %.1f kWh | max charge %s | efficiency %.3f kWh/km",
                v.model, v.batteryKwh, Fmt.kw(v.maxKw), v.effKwhPerKm));
        System.out.println(Fmt.bar(soc, 30) + " " + Fmt.pct(soc) + (s != null ? " (charging)" : "")
                + String.format(" | range about %.0f km", DriveSimulator.rangeKm(v, soc, 1)));
    }
}
