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
 * MEMBER 2 - reserve a slot. Main showcase of JDBC TRANSACTIONS:
 * setAutoCommit(false) -> check + insert -> commit, or rollback if anything is wrong.
 */
public class BookingService {
    public static final int MAX_DAYS_AHEAD = 7;
    public static final int MIN_MINUTES = 30, MAX_MINUTES = 180, STEP = 30;

    /** Validates, then saves. Returns the new booking id. Any problem is reported as a VoltGridException. */
    public static int reserve(User user, int stationId, LocalDateTime start, int minutes) throws VoltGridException {
        // 1) simple rules first (no database needed)
        if (start == null) throw new VoltGridException("Start time is not valid. Use yyyy-MM-dd HH:mm or HH:mm.");
        LocalDateTime now = LocalDateTime.now();
        if (start.isBefore(now.plusMinutes(5))) throw new VoltGridException("Start time must be at least 5 minutes from now.");
        if (start.isAfter(now.plusDays(MAX_DAYS_AHEAD))) throw new VoltGridException("You can only reserve up to " + MAX_DAYS_AHEAD + " days ahead.");
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES || minutes % STEP != 0) {
            throw new VoltGridException("Duration must be " + MIN_MINUTES + " to " + MAX_MINUTES + " minutes, in steps of " + STEP + ".");
        }
        Timestamp s = Timestamp.valueOf(start);
        Timestamp e = Timestamp.valueOf(start.plusMinutes(minutes));

        // 2) rules that need the database, inside a TRANSACTION
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);                              // start a TRANSACTION
            try {
                // lock the station row, so two users cannot book the same slot at the same moment
                try (PreparedStatement ps = c.prepareStatement("SELECT available FROM stations WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, stationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new VoltGridException("Station " + stationId + " does not exist.");
                        if (!rs.getBoolean(1)) throw new VoltGridException("This station is not available.");
                    }
                }
                // does this station or this user already have a booking at that time? (overlap check)
                String overlap = "SELECT COUNT(*) FROM bookings WHERE %s = ? AND status = 'ACTIVE' AND start_time < ? AND end_time > ?";
                if (count(c, String.format(overlap, "station_id"), stationId, e, s) > 0) {
                    throw new VoltGridException("That station is already booked at this time.");
                }
                if (count(c, String.format(overlap, "user_id"), user.getId(), e, s) > 0) {
                    throw new VoltGridException("You already have a reservation that overlaps this time.");
                }
                String sql = "INSERT INTO bookings (user_id, station_id, start_time, end_time) VALUES (?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, user.getId());
                    ps.setInt(2, stationId);
                    ps.setTimestamp(3, s);
                    ps.setTimestamp(4, e);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        int id = keys.getInt(1);
                        c.commit();                              // make it permanent
                        return id;
                    }
                }
            } catch (VoltGridException | SQLException ex) {
                c.rollback();                                    // undo everything
                throw ex;
            }
        } catch (SQLException ex) {
            // EXCEPTION CHAINING: keep the original SQLException as the "cause"
            throw new VoltGridException("Could not save the reservation (database problem).", ex);
        }
    }

    /** Runs a "SELECT COUNT(*)" with an int and two timestamps as parameters. */
    private static int count(Connection c, String sql, int id, Timestamp end, Timestamp start) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setTimestamp(2, end);
            ps.setTimestamp(3, start);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** The user's upcoming active bookings. */
    public static List<Booking> upcoming(int userId) throws SQLException {
        String sql = "SELECT b.id, s.name, b.start_time, b.end_time FROM bookings b"
                + " LEFT JOIN stations s ON s.id = b.station_id"
                + " WHERE b.user_id = ? AND b.status = 'ACTIVE' AND b.end_time > ? ORDER BY b.start_time";
        List<Booking> list = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Booking(rs.getInt(1), rs.getString(2),
                            rs.getTimestamp(3).toLocalDateTime(), rs.getTimestamp(4).toLocalDateTime()));
                }
            }
        }
        return list;
    }

    /** Cancels one of the user's own bookings that has not started yet. */
    public static boolean cancel(int userId, int bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ? AND user_id = ? AND status = 'ACTIVE' AND start_time > ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, userId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        }
    }
}
