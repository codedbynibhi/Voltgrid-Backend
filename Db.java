import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MEMBER 2 - JDBC basics: driver, URL, DriverManager, Connection.
 * Also creates the 4 tables and loads the demo data (using BATCH inserts).
 */
public class Db {
    // JDBC URL = jdbc:<database type>://<host>:<port>/<database name>
    private static final String URL  = "jdbc:mysql://localhost:3306/voltgrid";
    private static final String USER = "root";
    private static final String PASS = "";
    /** Opens a new connection. Callers close it using try-with-resources. */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void createTables() throws SQLException {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS users ("
                + " id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(120) NOT NULL UNIQUE,"
                + " car_model VARCHAR(80) NOT NULL, battery_kwh DECIMAL(6,1) NOT NULL,"
                + " max_kw DECIMAL(6,1) NOT NULL, soc DECIMAL(5,2) NOT NULL)",

            "CREATE TABLE IF NOT EXISTS stations ("
                + " id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, type VARCHAR(10) NOT NULL,"
                + " power_kw DECIMAL(6,1) NOT NULL, price_per_kwh DECIMAL(6,2) NOT NULL,"
                + " distance_km DECIMAL(6,1) NOT NULL, available TINYINT(1) NOT NULL DEFAULT 1)",

            "CREATE TABLE IF NOT EXISTS bookings ("
                + " id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, station_id INT NOT NULL,"
                + " start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,"
                + " status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE')",

            "CREATE TABLE IF NOT EXISTS charging_sessions ("
                + " id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, station_id INT NOT NULL,"
                + " session_date DATETIME NOT NULL, start_soc DECIMAL(5,2) NOT NULL, end_soc DECIMAL(5,2) NOT NULL,"
                + " energy_kwh DECIMAL(8,3) NOT NULL, cost DECIMAL(10,2) NOT NULL, status VARCHAR(12) NOT NULL)"
        };
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            for (String sql : tables) {
                st.executeUpdate(sql);               // CREATE/INSERT/UPDATE/DELETE use executeUpdate
            }
        }
    }

    /** BATCH: add many rows, then send them to the database together. setObject picks the right type. */
    private static void batchInsert(Connection c, String sql, Object[][] rows) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    ps.setObject(i + 1, row[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Demo data: 2 users with cars, 6 stations, 3 past sessions, 1 booking. Only used when the database is empty. */
    public static void seedDemo() throws SQLException {
        try (Connection c = get()) {
            batchInsert(c, "INSERT INTO users (name, email, car_model, battery_kwh, max_kw, soc) VALUES (?,?,?,?,?,?)",
                new Object[][] {
                    {"Demo Driver", "demo@voltgrid.com", "Tata Nexon EV", 40.0, 50.0, 20.0},
                    {"Aarav Sharma", "aarav@voltgrid.com", "MG ZS EV", 50.0, 75.0, 35.0}
                });
            batchInsert(c, "INSERT INTO stations (name, type, power_kw, price_per_kwh, distance_km, available) VALUES (?,?,?,?,?,?)",
                new Object[][] {
                    {"City Centre Fast Hub", "DC", 50.0, 18.0, 2.5, 1},
                    {"Highway Ultra Charge", "DC", 150.0, 24.0, 12.0, 1},
                    {"Mall Parking AC", "AC", 22.0, 12.0, 4.0, 1},
                    {"Campus Slow Charge", "AC", 7.4, 8.0, 1.2, 1},
                    {"Railway Station AC", "AC", 11.0, 10.0, 3.2, 1},
                    {"Airport Express", "DC", 120.0, 26.0, 18.5, 0}
                });
        }
        // The ids are auto-generated, so read them back instead of guessing 1, 2, 3...
        List<Station> st = Dao.listStations();
        int demo = Dao.findUserByEmail("demo@voltgrid.com").getId();
        int aarav = Dao.findUserByEmail("aarav@voltgrid.com").getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow10 = now.plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        try (Connection c = get()) {
            batchInsert(c, "INSERT INTO charging_sessions (user_id, station_id, session_date, start_soc, end_soc,"
                    + " energy_kwh, cost, status) VALUES (?,?,?,?,?,?,?,?)",
                new Object[][] {
                    {demo, st.get(0).getId(), Timestamp.valueOf(now.minusDays(9)), 15.0, 80.0, 26.0, 468.0, "COMPLETED"},
                    {demo, st.get(2).getId(), Timestamp.valueOf(now.minusDays(5)), 30.0, 80.0, 20.0, 240.0, "COMPLETED"},
                    {demo, st.get(3).getId(), Timestamp.valueOf(now.minusDays(2)), 40.0, 60.0, 8.0, 64.0, "STOPPED"}
                });
            // Aarav has already booked City Centre tomorrow 10:00-11:00, so you can test a clash with the demo user
            batchInsert(c, "INSERT INTO bookings (user_id, station_id, start_time, end_time) VALUES (?,?,?,?)",
                new Object[][] {
                    {aarav, st.get(0).getId(), Timestamp.valueOf(tomorrow10), Timestamp.valueOf(tomorrow10.plusHours(1))}
                });
        }
    }
}
