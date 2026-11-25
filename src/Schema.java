import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the tables this module needs. Every statement is CREATE TABLE IF NOT EXISTS,
 * so tables your teammates already created are left untouched.
 */
public final class Schema {
    private Schema() {}

    private static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS users ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(100) NOT NULL,"
            + " email VARCHAR(120) NOT NULL UNIQUE,"
            + " phone VARCHAR(20))",

        "CREATE TABLE IF NOT EXISTS stations ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(100) NOT NULL,"
            + " power_kw DECIMAL(6,1) NOT NULL,"
            + " type VARCHAR(20) NOT NULL,"
            + " price_per_kwh DECIMAL(6,2) NOT NULL,"
            + " distance_km DECIMAL(6,1) NOT NULL DEFAULT 0,"
            + " available TINYINT(1) NOT NULL DEFAULT 1,"
            + " rating DECIMAL(2,1) NOT NULL DEFAULT 0,"
            + " queue INT NOT NULL DEFAULT 0)",

        "CREATE TABLE IF NOT EXISTS vehicles ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " user_id INT NOT NULL UNIQUE,"
            + " model VARCHAR(80) NOT NULL,"
            + " battery_capacity_kwh DECIMAL(6,1) NOT NULL,"
            + " max_charging_speed_kw DECIMAL(6,1) NOT NULL,"
            + " efficiency_kwh_per_km DECIMAL(5,3) NOT NULL DEFAULT 0.160,"
            + " current_soc DECIMAL(5,2) NOT NULL DEFAULT 20.00)",

        "CREATE TABLE IF NOT EXISTS bookings ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " user_id INT NOT NULL,"
            + " station_id INT NOT NULL,"
            + " start_time DATETIME NOT NULL,"
            + " end_time DATETIME NOT NULL,"
            + " status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + " INDEX idx_station_time (station_id, start_time),"
            + " INDEX idx_user (user_id))",

        "CREATE TABLE IF NOT EXISTS charging_sessions ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " user_id INT NOT NULL,"
            + " station_id INT NOT NULL,"
            + " session_date DATETIME NOT NULL,"
            + " start_soc DECIMAL(5,2) NOT NULL,"
            + " end_soc DECIMAL(5,2) NOT NULL,"
            + " target_soc DECIMAL(5,2) NOT NULL,"
            + " energy_kwh DECIMAL(8,3) NOT NULL,"
            + " cost DECIMAL(10,2) NOT NULL,"
            + " duration_sec INT NOT NULL,"
            + " status VARCHAR(12) NOT NULL,"
            + " INDEX idx_user_date (user_id, session_date))"
    };

    public static void ensure() throws SQLException {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            for (String sql : DDL) st.executeUpdate(sql);
        }
    }
}
