import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * VoltGrid - EV charging reservation and simulator (terminal + MySQL).
 * MEMBER 4: the menu. Every option calls the class of the member who owns that feature.
 */
public class Main {
    private static User user;

    public static void main(String[] args) {
        try {
            Db.createTables();
            if (Dao.countRows("users") == 0 && Input.yesNo("Database is empty. Load demo data (2 users, 6 stations, sample history)?")) {
                Db.seedDemo();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(ChargingSession::stopRunning));   // save a running session on Ctrl+C
            login();
            menu();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("Check: MySQL is running, database 'voltgrid' exists, connector jar is in lib/");
        }
    }

    private static void login() throws SQLException {
        System.out.println("=== VoltGrid: EV charging and reservations ===");
        while (user == null) {
            String email = Input.line("Email (demo: demo@voltgrid.com or aarav@voltgrid.com): ");
            if (!TextUtil.isValidEmail(email)) {
                System.out.println("  That does not look like an email address.");
                continue;
            }
            user = Dao.findUserByEmail(email);
            if (user == null) System.out.println("  No user with that email.");
        }
        System.out.println("Welcome, " + user.getName() + "! Car: " + user.getCarModel());
    }

    // ---------------------------------------------------------------- menu

    private static void menu() throws SQLException {
        while (true) {
            System.out.println();
            ChargingSession live = ChargingSession.getCurrent();
            if (live != null) System.out.println("Charging in background: " + live.statusLine());
            System.out.println(" 1) View stations             6) Cost estimate");
            System.out.println(" 2) Reserve a slot            7) Charging history");
            System.out.println(" 3) My reservations / cancel  8) Admin: manage stations");
            System.out.println(" 4) Start charging            9) Thread demos");
            System.out.println(" 5) Battery / stop charging   0) Exit");
            int choice = Input.integer("> ", 0, 9);
            switch (choice) {
                case 1: stations(); break;
                case 2: reserve(); break;
                case 3: myReservations(); break;
                case 4: startCharging(); break;
                case 5: batteryStatus(); break;
                case 6: costEstimate(); break;
                case 7: Dao.printHistory(user.getId()); break;
                case 8: admin(); break;
                case 9: threadDemos(); break;
                default:
                    ChargingSession.stopRunning();                       // saves a running session
                    System.out.println("Bye!");
                    return;
            }
        }
    }

    // ---------------------------------------------------------------- stations (collections)

    private static void printStations(List<Station> list) {
        System.out.println(String.format("%-3s %-22s %-4s %8s %-7s %10s %8s %-5s", "ID", "Name", "Type", "Power", "Plug", "Price/kWh", "Distance", "Free"));
        for (Station s : list) {
            Charger charger = Charger.of(s);               // POLYMORPHISM: AcCharger or DcCharger decides the plug
            System.out.println(String.format("%-3d %-22s %-4s %6.1fkW %-7s %10.2f %6.1fkm %-5s", s.getId(), s.getName(),
                    s.getType(), s.getPowerKw(), charger.connector(), s.getPricePerKwh(), s.getDistanceKm(), s.isAvailable() ? "yes" : "no"));
        }
    }

    private static void stations() throws SQLException {
        StationCatalog catalog = new StationCatalog(Dao.listStations());
        System.out.println("1) Nearest first  2) Furthest first  3) A-Z by name  4) Search  5) Quick facts");
        int c = Input.integer("> ", 1, 5);
        switch (c) {
            case 1: printStations(catalog.sortedByDistance()); break;
            case 2: printStations(catalog.furthestFirst()); break;
            case 3: printStations(catalog.sortedByName()); break;
            case 4:
                List<Station> found = catalog.search(Input.line("Search text (name or AC/DC): "));
                if (found.isEmpty()) System.out.println("Nothing found.");
                else printStations(found);
                break;
            default:
                System.out.println("Nearest station:  " + catalog.nearest());
                System.out.println("Farthest station: " + catalog.farthest());
                System.out.println("Types (HashSet, no order):  " + catalog.typesHash());
                System.out.println("Types (TreeSet, sorted):    " + catalog.typesSorted());
                System.out.println("Charger objects created so far (static counter): " + Charger.getCreated());
        }
    }

    /** Shows all stations and asks for an id. Returns null if the user goes back. */
    private static Station pickStation() throws SQLException {
        List<Station> list = Dao.listStations();
        if (list.isEmpty()) {
            System.out.println("No stations in the database.");
            return null;
        }
        printStations(list);
        int id = Input.integer("Station ID (0 = back): ", 0, Integer.MAX_VALUE);
        if (id == 0) return null;
        Station s = new StationCatalog(list).getById(id);              // HashMap lookup by id
        if (s == null) System.out.println("No station with that ID.");
        return s;
    }

    // ---------------------------------------------------------------- reservations (JDBC)

    private static void reserve() throws SQLException {
        Station s = pickStation();
        if (s == null) return;
        LocalDateTime start = Input.dateTime("Start (yyyy-MM-dd HH:mm, or HH:mm for today): ");
        int minutes = Input.integer("Duration in minutes (30-180, steps of 30): ", 0, 100000);
        try {
            int id = BookingService.reserve(user, s.getId(), start, minutes);
            System.out.println("[SUCCESS] Reservation #" + id + " confirmed at " + s.getName()
                    + " from " + start.format(Input.FORMAT) + " for " + minutes + " min.");
        } catch (VoltGridException e) {
            System.out.println("Reservation not made: " + e.getMessage());
            if (e.getCause() != null) System.out.println("  Cause: " + e.getCause().getMessage());   // exception chaining
        }
    }

    private static void myReservations() throws SQLException {
        List<Booking> list = BookingService.upcoming(user.getId());
        if (list.isEmpty()) {
            System.out.println("You have no upcoming reservations.");
            return;
        }
        for (Booking b : list) {
            System.out.println(String.format("#%-4d %-22s %s -> %s", b.getId(), b.getStationName(),
                    b.getStart().format(Input.FORMAT), b.getEnd().format(Input.FORMAT)));
        }
        int id = Input.integer("Booking # to cancel (0 = back): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        if (BookingService.cancel(user.getId(), id)) {
            System.out.println("[SUCCESS] Reservation #" + id + " cancelled.");
        } else {
            System.out.println("Could not cancel: it must be your own active reservation that has not started yet.");
        }
    }

    // ---------------------------------------------------------------- charging (threads)

    private static void startCharging() throws SQLException {
        Station s = pickStation();
        if (s == null) return;
        double target = Input.decimal("Target charge % [80]: ", 1, 100, 80.0);
        try {
            ChargingSession.start(user, s, target);
            double energy = CostCalculator.energyKwh(user, user.getSoc(), target);
            System.out.println(String.format("[INFO] Charging started at %s. Estimate: %.2f kWh, %s, Rs.%.2f",
                    s.getName(), energy, TextUtil.hms(CostCalculator.hours(s, user, energy) * 3600), energy * s.getPricePerKwh()));
            System.out.println("It runs in the background: use option 5 to watch it or stop it.");
        } catch (VoltGridException | IllegalStateException e) {         // MULTI-CATCH: checked + unchecked
            System.out.println("Cannot start: " + e.getMessage());
        }
    }

    private static void batteryStatus() {
        ChargingSession s = ChargingSession.getCurrent();
        double soc = user.getSoc();
        System.out.println(String.format("%s | battery %.1f kWh | max charge %.1f kW", user.getCarModel(), user.getBatteryKwh(), user.getMaxKw()));
        if (s == null) {
            System.out.println(TextUtil.bar(soc, 30) + String.format(" %.1f%%", soc));
            System.out.println("No charging session is running.");
            return;
        }
        System.out.println(s.statusLine());
        if (Input.yesNo("Stop charging now?")) {
            s.stopAndWait();                                             // the session saves itself
        }
    }

    // ---------------------------------------------------------------- cost estimate (interface + 2D array)

    private static void costEstimate() throws SQLException {
        List<Station> list = Dao.listStations();
        if (list.isEmpty()) {
            System.out.println("No stations in the database.");
            return;
        }
        System.out.println("Pricing plan: 1) Flat   2) Peak / off-peak");
        Tariff plan = Input.integer("> ", 1, 2) == 1 ? new FlatTariff() : new PeakTariff();   // interface variable
        int hour = plan instanceof PeakTariff ? Input.integer("Start hour of the day (0-23): ", 0, 23) : 12;
        System.out.println("Using: " + plan.describe());
        System.out.println(String.format("Cost to charge from %.0f%% (%s):", user.getSoc(), user.getCarModel()));

        double[] targets = {50, 80, 100};
        double[][] table = CostCalculator.costTable(list, user, plan, hour, targets);   // 2D array
        System.out.println(String.format("%-3s %-22s %10s %10s %10s", "ID", "Station", "to 50%", "to 80%", "to 100%"));
        for (int row = 0; row < table.length; row++) {
            System.out.println(String.format("%-3d %-22s %10.2f %10.2f %10.2f", list.get(row).getId(),
                    list.get(row).getName(), table[row][0], table[row][1], table[row][2]));
        }
    }

    // ---------------------------------------------------------------- demos, admin

    private static void threadDemos() {
        System.out.println("1) start() vs run(), join, interrupt   2) Race condition and synchronized");
        int c = Input.integer("> ", 1, 2);
        try {
            if (c == 1) ThreadDemo.startVsRun();
            else ThreadDemo.raceCondition();
        } catch (InterruptedException e) {
            System.out.println("Demo interrupted.");
        }
    }

    private static void admin() throws SQLException {
        System.out.println("1) Add station  2) Change price / availability  3) Delete station");
        int c = Input.integer("> ", 1, 3);
        if (c == 1) {
            String name = TextUtil.titleCase(Input.line("Name: "));
            String type = Input.line("Type (AC or DC): ").toUpperCase();
            if (!type.equals("AC") && !type.equals("DC")) {
                System.out.println("Type must be AC or DC.");
                return;
            }
            double power = Input.decimal("Power kW: ", 1, 400, null);
            double price = Input.decimal("Price per kWh: ", 1, 200, null);
            double dist = Input.decimal("Distance km: ", 0, 500, null);
            int id = Dao.addStation(name, type, power, price, dist);
            System.out.println("Added station #" + id + " (" + name + ").");
        } else if (c == 2) {
            Station s = pickStation();
            if (s == null) return;
            double price = Input.decimal("New price per kWh [" + s.getPricePerKwh() + "]: ", 1, 200, s.getPricePerKwh());
            boolean available = Input.yesNo("Available?");
            System.out.println(Dao.updateStation(s.getId(), price, available) ? "Updated." : "Station not found.");
        } else {
            Station s = pickStation();
            if (s == null) return;
            System.out.println(Dao.deleteStation(s.getId()) ? "Deleted." : "Station not found.");
        }
    }
}
