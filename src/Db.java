import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC connection helper. Same settings as test.java; override with env vars if needed. */
public final class Db {
    private static final String URL  = env("VOLTGRID_DB_URL",  "jdbc:mysql://localhost:3306/voltgrid");
    private static final String USER = env("VOLTGRID_DB_USER", "root");
    private static final String PASS = env("VOLTGRID_DB_PASS", "");

    private Db() {}

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    /** A new connection per operation: simple, and safe for the background charging thread. */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
