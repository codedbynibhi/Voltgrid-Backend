import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Reads of the shared tables (users, stations, vehicles) plus the demo seeder. */
public final class Dao {
    private Dao() {}

    public static int countRows(String table) throws SQLException {
        try (Connection c = Db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static Models.User findUserByEmail(String email) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT id, name, email FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Models.User(rs.getInt("id"), rs.getString("name"), rs.getString("email")) : null;
            }
        }
    }

    private static Models.Station station(ResultSet rs) throws SQLException {
        return new Models.Station(rs.getInt("id"), rs.getString("name"), rs.getDouble("power_kw"),
                rs.getString("type"), rs.getDouble("price_per_kwh"), rs.getDouble("distance_km"),
                rs.getBoolean("available"), rs.getDouble("rating"), rs.getInt("queue"));
    }

    public static List<Models.Station> listStations() throws SQLException {
        List<Models.Station> out = new ArrayList<>();
        try (Connection c = Db.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM stations ORDER BY id")) {
            while (rs.next()) out.add(station(rs));
        }
        return out;
    }

    public static Models.Station getStation(int id) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM stations WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? station(rs) : null;
            }
        }
    }

    public static Models.Vehicle getVehicle(int userId) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM vehicles WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Models.Vehicle(rs.getInt("id"), rs.getInt("user_id"), rs.getString("model"),
                        rs.getDouble("battery_capacity_kwh"), rs.getDouble("max_charging_speed_kw"),
                        rs.getDouble("efficiency_kwh_per_km"), rs.getDouble("current_soc"));
            }
        }
    }

    public static void createVehicle(int userId, String model, double batteryKwh, double maxKw, double soc) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO vehicles (user_id, model, battery_capacity_kwh, max_charging_speed_kw, current_soc) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, userId); ps.setString(2, model); ps.setDouble(3, batteryKwh);
            ps.setDouble(4, maxKw); ps.setDouble(5, soc);
            ps.executeUpdate();
        }
    }

    public static void updateSoc(int vehicleId, double soc) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("UPDATE vehicles SET current_soc = ? WHERE id = ?")) {
            ps.setDouble(1, Math.max(0, Math.min(100, soc)));
            ps.setInt(2, vehicleId);
            ps.executeUpdate();
        }
    }

    /** Demo data so the module can be tried before the other members' tables are filled. Inserts only into empty tables. */
    public static void seedDemo() throws SQLException {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            if (countRows("users") == 0) {
                st.executeUpdate("INSERT INTO users (name, email, phone) VALUES "
                        + "('Demo Driver', 'demo@voltgrid.test', '9000000001')");
            }
            if (countRows("stations") == 0) {
                st.executeUpdate("INSERT INTO stations (name, power_kw, type, price_per_kwh, distance_km, available, rating, queue) VALUES "
                        + "('City Centre Fast Hub', 50, 'DC Fast', 18.00, 2.5, 1, 4.5, 1),"
                        + "('Highway Ultra Charge', 150, 'DC Ultra', 24.00, 12.0, 1, 4.7, 0),"
                        + "('Mall Parking AC', 22, 'AC', 12.00, 4.0, 1, 4.1, 2),"
                        + "('Campus Slow Charge', 7.4, 'AC', 8.00, 1.2, 1, 3.9, 0),"
                        + "('Airport Express', 120, 'DC Ultra', 26.00, 18.5, 0, 4.4, 3)");
            }
        }
    }

    // ---------- CRUD on stations (Create, Read, Update, Delete) ----------

    /** CREATE: INSERT with a PreparedStatement; returns the new auto-generated id. */
    public static int addStation(String name, double powerKw, String type, double price, double distanceKm) throws SQLException {
        String sql = "INSERT INTO stations (name, power_kw, type, price_per_kwh, distance_km) VALUES (?,?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);          // parameter binding: index starts at 1
            ps.setDouble(2, powerKw);
            ps.setString(3, type);
            ps.setDouble(4, price);
            ps.setDouble(5, distanceKm);
            ps.executeUpdate();             // INSERT/UPDATE/DELETE use executeUpdate (returns rows changed)
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** UPDATE: change price and availability. Returns false if the id does not exist. */
    public static boolean updateStation(int id, double price, boolean available) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE stations SET price_per_kwh = ?, available = ? WHERE id = ?")) {
            ps.setDouble(1, price);
            ps.setBoolean(2, available);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** DELETE: refuses when the station still has active upcoming reservations. */
    public static boolean deleteStation(int id) throws SQLException, ValidationException {
        try (Connection c = Db.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM bookings WHERE station_id = ? AND status = 'ACTIVE' AND end_time > ?")) {
                ps.setInt(1, id);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0)
                        throw new ValidationException("Station " + id + " still has " + rs.getInt(1) + " active reservation(s).");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM stations WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        }
    }

    // ---------- more reads (ResultSet iteration with typed getters) ----------

    /** Bookings of one station on one day (feeds the 2D schedule grid). */
    public static List<Models.Booking> listBookingsForStation(int stationId, LocalDate day) throws SQLException {
        List<Models.Booking> out = new ArrayList<>();
        String sql = "SELECT id, station_id, start_time, end_time, status FROM bookings "
                + "WHERE station_id = ? AND status = 'ACTIVE' AND start_time < ? AND end_time > ? ORDER BY start_time";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, stationId);
            ps.setTimestamp(2, Timestamp.valueOf(day.plusDays(1).atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(day.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {        // next() moves the cursor to the next row; false when there are no more
                    out.add(new Models.Booking(rs.getInt("id"), rs.getInt("station_id"), "",
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(), rs.getString("status")));
                }
            }
        }
        return out;
    }

    /** All saved charging sessions of a user, oldest first. */
    public static List<Models.SessionRecord> listSessions(int userId) throws SQLException {
        List<Models.SessionRecord> out = new ArrayList<>();
        String sql = "SELECT s.id, s.station_id, st.name, s.session_date, s.start_soc, s.end_soc, s.energy_kwh, "
                + "s.cost, s.duration_sec, s.status FROM charging_sessions s "
                + "LEFT JOIN stations st ON st.id = s.station_id WHERE s.user_id = ? ORDER BY s.session_date";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String stationName = rs.getString(3);            // by column index
                    if (rs.wasNull()) stationName = "(deleted station)";
                    out.add(new Models.SessionRecord(rs.getInt("id"), rs.getInt("station_id"), stationName,
                            rs.getTimestamp("session_date").toLocalDateTime(),
                            rs.getDouble("start_soc"), rs.getDouble("end_soc"), rs.getDouble("energy_kwh"),
                            rs.getBigDecimal("cost").doubleValue(),   // DECIMAL column -> BigDecimal
                            rs.getInt("duration_sec"), rs.getString("status")));
                }
            }
        }
        return out;
    }
}
