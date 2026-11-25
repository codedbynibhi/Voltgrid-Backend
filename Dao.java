import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MEMBER 2 - DAO = Data Access Object: the SQL for users, stations and charging history is here.
 * Topics: PreparedStatement, executeQuery vs executeUpdate, ResultSet, CRUD, try-with-resources.
 *
 * Why PreparedStatement and not Statement? The "?" placeholders are filled with setXxx(), so user text can
 * never change the SQL itself (this stops SQL injection) and the database can reuse the compiled query.
 */
public class Dao {

    public static int countRows(String table) throws SQLException {
        try (Connection c = Db.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---------------- users ----------------

    /** Returns the user, or null when the email is not in the table. */
    public static User findUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);                              // parameter numbers start at 1
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;                     // next() moves to the next row, false = no row
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                        rs.getString("car_model"), rs.getDouble("battery_kwh"),
                        rs.getDouble("max_kw"), rs.getDouble("soc"));
            }
        }
    }

    public static void updateSoc(int userId, double soc) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET soc = ? WHERE id = ?")) {
            ps.setDouble(1, Math.max(0, Math.min(100, soc)));    // keep between 0 and 100
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ---------------- stations: CRUD ----------------

    private static Station readStation(ResultSet rs) throws SQLException {
        return new Station(rs.getInt("id"), rs.getString("name"), rs.getString("type"),
                rs.getDouble("power_kw"), rs.getDouble("price_per_kwh"),
                rs.getDouble("distance_km"), rs.getBoolean("available"));
    }

    /** READ (all rows). */
    public static List<Station> listStations() throws SQLException {
        List<Station> list = new ArrayList<>();
        try (Connection c = Db.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM stations ORDER BY id")) {
            while (rs.next()) {
                list.add(readStation(rs));
            }
        }
        return list;
    }

    /** READ (one row). Returns null when the id does not exist. */
    public static Station getStation(int id) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM stations WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readStation(rs) : null;
            }
        }
    }

    /** CREATE. Returns the new id. */
    public static int addStation(String name, String type, double powerKw, double price, double distanceKm) throws SQLException {
        String sql = "INSERT INTO stations (name, type, power_kw, price_per_kwh, distance_km) VALUES (?,?,?,?,?)";
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, powerKw);
            ps.setDouble(4, price);
            ps.setDouble(5, distanceKm);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);                           // the auto-generated id
            }
        }
    }

    /** UPDATE. Returns false if no station has this id. */
    public static boolean updateStation(int id, double price, boolean available) throws SQLException {
        String sql = "UPDATE stations SET price_per_kwh = ?, available = ? WHERE id = ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, price);
            ps.setBoolean(2, available);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;                       // executeUpdate returns the number of rows changed
        }
    }

    /** DELETE. Returns false if no station has this id. */
    public static boolean deleteStation(int id) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM stations WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------------- charging sessions ----------------

    /** Called automatically when a charging session ends. */
    public static void saveSession(int userId, int stationId, LocalDateTime date, double startSoc,
                                   double endSoc, double energyKwh, double cost, String status) throws SQLException {
        String sql = "INSERT INTO charging_sessions (user_id, station_id, session_date, start_soc, end_soc,"
                + " energy_kwh, cost, status) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, stationId);
            ps.setTimestamp(3, Timestamp.valueOf(date));
            ps.setDouble(4, startSoc);
            ps.setDouble(5, endSoc);
            ps.setDouble(6, energyKwh);
            ps.setDouble(7, cost);
            ps.setString(8, status);
            ps.executeUpdate();
        }
    }

    /** Prints the charging history (a JOIN of two tables) and the totals. */
    public static void printHistory(int userId) throws SQLException {
        String sql = "SELECT s.session_date, st.name, s.start_soc, s.end_soc, s.energy_kwh, s.cost, s.status"
                + " FROM charging_sessions s LEFT JOIN stations st ON st.id = s.station_id"
                + " WHERE s.user_id = ? ORDER BY s.session_date";
        double totalKwh = 0, totalCost = 0;
        int count = 0;
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String station = rs.getString("name");
                    if (rs.wasNull()) station = "(deleted station)";   // wasNull(): was the last value SQL NULL?
                    double kwh = rs.getDouble("energy_kwh");
                    double cost = rs.getDouble("cost");
                    System.out.println(String.format("%s  %-22s %5.1f%% -> %5.1f%%  %7.2f kWh  Rs.%8.2f  %s",
                            rs.getTimestamp("session_date").toLocalDateTime().format(Input.FORMAT),
                            station, rs.getDouble("start_soc"), rs.getDouble("end_soc"), kwh, cost, rs.getString("status")));
                    totalKwh += kwh;
                    totalCost += cost;
                    count++;
                }
            }
        }
        if (count == 0) {
            System.out.println("No charging sessions yet.");
        } else {
            System.out.println(String.format("Total: %d sessions, %.2f kWh, Rs.%.2f", count, totalKwh, totalCost));
        }
    }
}
