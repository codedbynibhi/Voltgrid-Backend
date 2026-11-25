import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Sub-menus 10-14 of the main menu: stations, reports, concurrency demos, Java concepts lab, JDBC lab.
 * Kept separate from Main so the original menu stays small.
 */
public final class ExtraMenu {
    private ExtraMenu() {}

    // =================== 10) Stations and schedule ===================

    public static void stations(Models.User user) throws SQLException {
        while (true) {
            System.out.println();
            System.out.println("-- Stations --");
            System.out.println(" 1) List nearest first        7) Day schedule grid (2D array)");
            System.out.println(" 2) Search by name or type    8) Waiting list");
            System.out.println(" 3) Station details           9) Add a station");
            System.out.println(" 4) Recently viewed          10) Update price / availability");
            System.out.println(" 5) Best chargers for me     11) Delete a station");
            System.out.println(" 6) Types summary             0) Back");
            int choice = Input.integer("> ", 0, 11);
            StationCatalog catalog = new StationCatalog(Dao.listStations());
            switch (choice) {
                case 1: listNearest(catalog); break;
                case 2: search(catalog); break;
                case 3: details(catalog); break;
                case 4: recent(catalog); break;
                case 5: best(catalog, user); break;
                case 6: types(catalog); break;
                case 7: schedule(catalog); break;
                case 8: waitingList(catalog, user); break;
                case 9: addStation(); break;
                case 10: updateStation(); break;
                case 11: deleteStation(); break;
                default: return;
            }
        }
    }

    private static void listNearest(StationCatalog catalog) {
        List<Models.Station> list = catalog.sortedByDistance();
        Main.printStations(list);
        Models.Station near = catalog.nearestAvailable();
        System.out.println(near == null ? "No station is available." : "Nearest available: " + near.name);
        System.out.println("(" + catalog.size() + " stations; sorted with Collections.sort using Station.compareTo)");
    }

    private static void search(StationCatalog catalog) {
        String text = Input.line("Search text: ");
        if (text.isEmpty()) return;
        List<Models.Station> found = catalog.search(text);
        if (found.isEmpty()) System.out.println("Nothing matches '" + text + "'.");
        else Main.printStations(found);
    }

    private static void details(StationCatalog catalog) {
        int id = Input.integer("Station ID: ", 0, Integer.MAX_VALUE);
        Models.Station s = catalog.getById(id);
        if (s == null) { System.out.println("No such station."); return; }
        catalog.markViewed(s);
        Charger charger = Charger.of(s);           // returns AcCharger or DcCharger; we only see "Charger"
        System.out.println(charger.describe());
        System.out.println(String.format("Price %s/kWh | %.1f km away | rating %.1f | queue %d | %s",
                Fmt.money(s.pricePerKwh), s.distanceKm, s.rating, s.queue, s.available ? "available" : "not available"));
    }

    private static void recent(StationCatalog catalog) {
        List<Models.Station> list = catalog.recentlyViewed();
        if (list.isEmpty()) { System.out.println("You have not opened any station details yet (option 3)."); return; }
        System.out.println("Recently viewed, newest first (LinkedHashMap, keeps the last 5):");
        for (Models.Station s : list) System.out.println("  " + s);
    }

    private static void best(StationCatalog catalog, Models.User user) throws SQLException {
        Models.Vehicle v = Dao.getVehicle(user.id);
        if (v == null) { System.out.println("No vehicle profile."); return; }
        List<Recommender.Scored> top = Recommender.top(catalog.all(), v, 3);
        System.out.println("Best chargers for you (speed 30%, distance 20%, availability 20%, price 15%, rating 10%, queue 5%):");
        int rank = 1;
        for (Recommender.Scored sc : top)
            System.out.println(String.format("  %d. %-24s score %.1f", rank++, sc.station.name, sc.score));
    }

    private static void types(StationCatalog catalog) {
        System.out.println("Types in the order first seen (LinkedHashSet): " + catalog.typesInFirstSeenOrder());
        System.out.println("Types sorted (TreeSet):                        " + catalog.typesSorted());
        for (String t : catalog.typesSorted())
            System.out.println("  " + t + ": " + catalog.countOfType(t) + " station(s)  (Collections.frequency)");
        System.out.println("Stations A-Z (TreeMap): " + catalog.sortedByName());
    }

    private static void schedule(StationCatalog catalog) throws SQLException {
        Main.printStations(catalog.all());
        Models.Station s = catalog.getById(Input.integer("Station ID: ", 0, Integer.MAX_VALUE));
        if (s == null) { System.out.println("No such station."); return; }
        String text = Input.line("Date yyyy-MM-dd [today]: ");
        LocalDate day;
        try {
            day = text.isEmpty() ? LocalDate.now() : LocalDate.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("Use the format yyyy-MM-dd.");
            return;
        }
        int[][] grid = ScheduleGrid.occupancy(Dao.listBookingsForStation(s.id, day), day);
        ScheduleGrid.print(s.name, ReservationService.SLOTS_PER_STATION, grid);
    }

    private static void waitingList(StationCatalog catalog, Models.User user) {
        Main.printStations(catalog.all());
        Models.Station s = catalog.getById(Input.integer("Station ID: ", 0, Integer.MAX_VALUE));
        if (s == null) { System.out.println("No such station."); return; }
        System.out.println(Waitlist.size(s.id) + " waiting at " + s.name + ".");
        if (Input.yesNo("Join the waiting list?")) {
            int pos = Waitlist.join(s.id, user.id);
            ActivityLog.add("Joined waiting list at " + s.name + " (position " + pos + ")");
            Notify.toast("info", "You are number " + pos + " on the waiting list for " + s.name + ".");
        }
    }

    private static void addStation() throws SQLException {
        String name = TextUtil.titleCase(Input.line("Name: "));
        if (name.isEmpty()) { System.out.println("Name is required."); return; }
        System.out.println("Type: 1) AC  2) DC Fast  3) DC Ultra");
        String[] types = {"AC", "DC Fast", "DC Ultra"};
        String type = types[Input.integer("> ", 1, 3) - 1];
        double power = Input.decimal("Power kW: ", 1, 500, null);
        double price = Input.decimal("Price per kWh: ", 0.1, 500, null);
        double dist = Input.decimal("Distance km [0]: ", 0, 1000, 0.0);
        int id = Dao.addStation(name, power, type, price, dist);
        ActivityLog.add("Added station #" + id + " " + name);
        Notify.toast("success", "Station #" + id + " '" + name + "' added.");
    }

    private static void updateStation() throws SQLException {
        int id = Input.integer("Station ID to update: ", 0, Integer.MAX_VALUE);
        Models.Station s = Dao.getStation(id);
        if (s == null) { System.out.println("No such station."); return; }
        double price = Input.decimal("New price per kWh [" + s.pricePerKwh + "]: ", 0.1, 500, s.pricePerKwh);
        boolean available = Input.yesNo("Available now?");
        if (Dao.updateStation(id, price, available)) {
            ActivityLog.add("Updated station #" + id);
            Notify.toast("success", s.name + " updated.");
        }
    }

    private static void deleteStation() throws SQLException {
        int id = Input.integer("Station ID to delete: ", 0, Integer.MAX_VALUE);
        Models.Station s = Dao.getStation(id);
        if (s == null) { System.out.println("No such station."); return; }
        if (!Input.yesNo("Delete " + s.name + "?")) return;
        try {
            if (Dao.deleteStation(id)) {
                ActivityLog.add("Deleted station #" + id + " " + s.name);
                Notify.toast("success", s.name + " deleted.");
            }
        } catch (ValidationException e) {
            System.out.println("Not deleted: " + e.getMessage());
        }
    }

    // =================== 11) Reports ===================

    public static void reports(Models.User user) throws SQLException {
        while (true) {
            System.out.println();
            System.out.println("-- Reports --");
            System.out.println(" 1) Charging history (newest first)    4) Activity log");
            System.out.println(" 2) kWh per station per weekday        5) Undo last reservation");
            System.out.println(" 3) Export history to CSV              0) Back");
            int choice = Input.integer("> ", 0, 5);
            switch (choice) {
                case 1: history(user); break;
                case 2: usage(user); break;
                case 3: exportCsv(user); break;
                case 4: activity(); break;
                case 5: undo(user); break;
                default: return;
            }
        }
    }

    private static void history(Models.User user) throws SQLException {
        List<Models.SessionRecord> list = Dao.listSessions(user.id);
        if (list.isEmpty()) { System.out.println("No saved charging sessions yet."); return; }
        Collections.reverse(list);                 // the query returns oldest first
        System.out.println(String.format("%-4s %-16s %-24s %13s %10s %10s %-9s", "ID", "Date", "Station", "Charge", "Energy", "Cost", "Status"));
        double totalKwh = 0, totalCost = 0;
        for (Models.SessionRecord r : list) {
            System.out.println(String.format("%-4d %-16s %-24s %5.1f->%5.1f%% %10s %10s %-9s", r.id, r.date.format(Input.DT),
                    r.stationName, r.startSoc, r.endSoc, Fmt.kwh(r.energyKwh), Fmt.money(r.cost), r.status));
            totalKwh += r.energyKwh;
            totalCost += r.cost;
        }
        System.out.println(list.size() + " session(s), " + Fmt.kwh(totalKwh) + ", " + Fmt.money(totalCost));
    }

    private static void usage(Models.User user) throws SQLException {
        List<Models.Station> stations = Dao.listStations();
        List<Models.SessionRecord> records = Dao.listSessions(user.id);
        if (stations.isEmpty()) { System.out.println("No stations."); return; }
        ScheduleGrid.printUsage(stations, ScheduleGrid.weekdayUsage(records, stations));
    }

    private static void exportCsv(Models.User user) throws SQLException {
        List<Models.SessionRecord> list = Dao.listSessions(user.id);
        StringBuilder csv = new StringBuilder(TextUtil.csvLine("id", "date", "station", "start_soc", "end_soc",
                "energy_kwh", "cost", "status")).append('\n');
        for (Models.SessionRecord r : list) {
            csv.append(TextUtil.csvLine(String.valueOf(r.id), r.date.format(Input.DT), r.stationName,
                    String.valueOf(r.startSoc), String.valueOf(r.endSoc), String.valueOf(r.energyKwh),
                    String.valueOf(r.cost), r.status)).append('\n');
        }
        String file = "sessions_user" + user.id + ".csv";
        try (FileWriter w = new FileWriter(file)) {       // closed automatically
            w.write(csv.toString());
            Notify.toast("success", list.size() + " session(s) written to " + file);
            ActivityLog.add("Exported " + list.size() + " sessions to " + file);
        } catch (IOException e) {
            System.out.println("Could not write the file: " + e.getMessage());
        }
    }

    private static void activity() {
        System.out.println("Activity this run, newest first (LinkedList + ListIterator going backwards):");
        ActivityLog.printNewestFirst();
        if (ActivityLog.size() > 10 && Input.yesNo("Keep only the newest 10 entries?"))
            System.out.println(ActivityLog.trim(10) + " old entries removed (Iterator.remove).");
    }

    private static void undo(Models.User user) throws SQLException {
        int id = ActivityLog.popLastReservation();
        if (id < 0) { System.out.println("No reservation made in this run to undo."); return; }
        if (ReservationService.cancel(user.id, id)) {
            ActivityLog.add("Undid reservation #" + id);
            Notify.toast("success", "Reservation #" + id + " cancelled.");
        } else {
            System.out.println("Reservation #" + id + " could not be cancelled (already started or cancelled).");
        }
    }

    // =================== 12) Concurrency demos ===================

    public static void concurrency() {
        while (true) {
            System.out.println();
            System.out.println("-- Multithreading demos --");
            System.out.println(" 1) start() vs run(), Thread vs Runnable   5) Race condition + synchronized");
            System.out.println(" 2) Thread states (lifecycle)              6) Vehicles sharing plugs (wait/notify)");
            System.out.println(" 3) sleep, join, interrupt                 7) Producer-consumer meter buffer");
            System.out.println(" 4) Daemon threads and priorities          8) Sequential vs concurrent timing");
            System.out.println(" 0) Back");
            int choice = Input.integer("> ", 0, 8);
            try {
                switch (choice) {
                    case 1: ConcurrencyLab.startVsRun(); break;
                    case 2: ConcurrencyLab.lifecycle(); break;
                    case 3: ConcurrencyLab.interruptDemo(); break;
                    case 4: ConcurrencyLab.daemonAndPriority(); break;
                    case 5: ConcurrencyLab.raceCondition(); break;
                    case 6: ConcurrencyLab.fleet(); break;
                    case 7: ConcurrencyLab.producerConsumer(); break;
                    case 8: ConcurrencyLab.sequentialVsConcurrent(); break;
                    default: return;
                }
            } catch (InterruptedException e) {
                System.out.println("The demo was interrupted.");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // =================== 13) Java concepts lab ===================

    public static void concepts() {
        while (true) {
            System.out.println();
            System.out.println("-- Java concepts lab --");
            System.out.println(" 1) OOP: inheritance, interface, polymorphism   4) Exceptions");
            System.out.println(" 2) Arrays (1D, 2D, jagged)                      5) Collections");
            System.out.println(" 3) Strings, StringBuilder, wrapper classes      0) Back");
            int choice = Input.integer("> ", 0, 5);
            switch (choice) {
                case 1: ConceptsLab.oop(); break;
                case 2: ConceptsLab.arrays(); break;
                case 3: ConceptsLab.strings(); System.out.println(); ConceptsLab.wrappers(); break;
                case 4: ConceptsLab.exceptions(); break;
                case 5: ConceptsLab.collections(); break;
                default: return;
            }
        }
    }

    // =================== 14) JDBC lab ===================

    public static void jdbc() throws SQLException {
        while (true) {
            System.out.println();
            System.out.println("-- JDBC lab --");
            System.out.println(" 1) Driver and connection info      4) SQL injection: Statement vs PreparedStatement");
            System.out.println(" 2) Scrollable ResultSet            5) Batch insert + transaction rollback");
            System.out.println(" 3) Show a table (metadata)         0) Back");
            int choice = Input.integer("> ", 0, 5);
            switch (choice) {
                case 1: JdbcLab.info(); break;
                case 2: JdbcLab.scrollDemo(); break;
                case 3:
                    System.out.println("1) users  2) stations  3) bookings  4) charging_sessions");
                    String[] tables = {"users", "stations", "bookings", "charging_sessions"};
                    JdbcLab.dumpTable(tables[Input.integer("> ", 1, 4) - 1]);
                    break;
                case 4: JdbcLab.injectionDemo(Input.line("Station name to search: ")); break;
                case 5: JdbcLab.batchAndRollback(); break;
                default: return;
            }
        }
    }
}
