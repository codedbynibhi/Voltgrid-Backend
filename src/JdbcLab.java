import java.sql.*;
import java.util.Enumeration;

/**
 * JDBC demos: architecture/driver info, scrollable ResultSet, ResultSetMetaData,
 * Statement vs PreparedStatement (SQL injection), batch updates and transaction rollback.
 */
public final class JdbcLab {
    private JdbcLab() {}

    /** Application -> DriverManager -> Driver -> Database. Shows each piece. */
    public static void info() throws SQLException {
        System.out.println("JDBC architecture: Main -> DriverManager -> MySQL driver (jar in lib/) -> MySQL server");
        System.out.println("Drivers registered with DriverManager:");
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            System.out.println("  " + d.getClass().getName() + " v" + d.getMajorVersion() + "." + d.getMinorVersion());
        }
        try (Connection c = Db.get()) {
            DatabaseMetaData md = c.getMetaData();
            System.out.println("URL:        " + md.getURL());
            System.out.println("Database:   " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver:     " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("Auto-commit " + c.getAutoCommit() + " (we switch it off for transactions)");
        }
    }

    /** Scrollable ResultSet: the cursor can move forward AND backward and jump to a row. */
    public static void scrollDemo() throws SQLException {
        String sql = "SELECT id, name, power_kw, price_per_kwh, available FROM stations ORDER BY id";
        try (Connection c = Db.get();
             Statement st = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = st.executeQuery(sql)) {
            rs.last();
            int rows = rs.getRow();
            System.out.println("Rows in result: " + rows + " (found with last() + getRow())");
            if (rows == 0) return;

            System.out.println("Forward with next(), getters by column NAME:");
            rs.beforeFirst();
            while (rs.next()) {
                System.out.println("  " + rs.getInt("id") + "  " + rs.getString("name") + "  "
                        + rs.getDouble("power_kw") + " kW  Rs." + rs.getBigDecimal("price_per_kwh")
                        + "  available=" + rs.getBoolean("available"));
            }
            System.out.println("Backward with previous(), getters by column INDEX:");
            while (rs.previous()) {
                System.out.println("  " + rs.getInt(1) + "  " + rs.getString(2));
            }
            if (rows >= 2) {
                rs.absolute(2);
                System.out.println("absolute(2) jumps straight to row 2: " + rs.getString("name"));
            }
        }
    }

    /** Prints any table using ResultSetMetaData (column count, names and types). Only called with fixed SQL. */
    public static void dumpTable(String table) throws SQLException {
        String sql;
        switch (table) {
            case "stations": sql = "SELECT * FROM stations"; break;
            case "bookings": sql = "SELECT * FROM bookings"; break;
            case "charging_sessions": sql = "SELECT * FROM charging_sessions"; break;
            default: sql = "SELECT * FROM users"; break;
        }
        try (Connection c = Db.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder head = new StringBuilder();
            for (int i = 1; i <= cols; i++) head.append(md.getColumnLabel(i)).append('(').append(md.getColumnTypeName(i)).append(")  ");
            System.out.println(head);
            int n = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) row.append(rs.getString(i)).append(" | ");
                System.out.println(row);
                n++;
            }
            System.out.println(n + " row(s).");
        }
    }

    /** Same search done two ways. The Statement version can be tricked; the PreparedStatement cannot. */
    public static void injectionDemo(String text) throws SQLException {
        String unsafeSql = "SELECT id, name FROM stations WHERE name = '" + text + "'";
        System.out.println("1) Statement + string joining. SQL sent to the database:");
        System.out.println("   " + unsafeSql);
        try (Connection c = Db.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(unsafeSql)) {
            int n = 0;
            while (rs.next()) { System.out.println("   -> " + rs.getInt("id") + " " + rs.getString("name")); n++; }
            System.out.println("   " + n + " row(s)");
        } catch (SQLException e) {
            System.out.println("   SQL error caused by the input: " + e.getMessage());
        }

        System.out.println("2) PreparedStatement with ? and setString - the input is only ever DATA:");
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("SELECT id, name FROM stations WHERE name = ?")) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) { System.out.println("   -> " + rs.getInt("id") + " " + rs.getString("name")); n++; }
                System.out.println("   " + n + " row(s)");
            }
        }
        System.out.println("Try the text   x' OR '1'='1   to see the difference.");
    }

    private static int count(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Batch insert inside a transaction, then ROLLBACK so nothing is kept. */
    public static void batchAndRollback() throws SQLException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);                       // start a transaction
            try {
                System.out.println("Stations before:                 " + count(c, "stations"));
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO stations (name, power_kw, type, price_per_kwh, distance_km) VALUES (?,?,?,?,?)")) {
                    String[] names = {"Batch A", "Batch B", "Batch C"};
                    for (int i = 0; i < names.length; i++) {
                        ps.setString(1, names[i]);
                        ps.setDouble(2, 22 + i);
                        ps.setString(3, "AC");
                        ps.setDouble(4, 10);
                        ps.setDouble(5, 5);
                        ps.addBatch();                    // queue the statement
                    }
                    int[] results = ps.executeBatch();    // send all three in one go
                    System.out.println("executeBatch() sent " + results.length + " INSERT statements in one go");
                }
                System.out.println("Stations inside transaction:     " + count(c, "stations"));
                c.rollback();                             // undo everything since setAutoCommit(false)
                System.out.println("After rollback():                " + count(c, "stations"));
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
