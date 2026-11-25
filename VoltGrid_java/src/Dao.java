import java.sql.*;
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
}
