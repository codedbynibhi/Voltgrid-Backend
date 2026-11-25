import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-15 Reserve a slot, FR-16 Reservation validation.
 * Also provides the bookings list and cancelBooking(id) that Member 4's reservations tab uses.
 */
public final class ReservationService {
    public static final int SLOTS_PER_STATION = 2;     // simultaneous bookings a station can take
    public static final int MAX_ACTIVE_PER_USER = 3;
    public static final int MAX_ADVANCE_DAYS = 7;
    public static final int MIN_LEAD_MINUTES = 5;
    public static final int MIN_DURATION = 15, MAX_DURATION = 180, DURATION_STEP = 15;

    private ReservationService() {}

    /**
     * FR-16: all validation rules. Must run on a connection with autoCommit off; it locks the
     * station row so two people cannot take the last slot at the same moment.
     */
    private static List<String> check(Connection c, int userId, int stationId, LocalDateTime start, int minutes) throws SQLException {
        List<String> errs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (start == null) { errs.add("Start time is missing or not in the format yyyy-MM-dd HH:mm."); }
        else {
            if (start.isBefore(now.plusMinutes(MIN_LEAD_MINUTES)))
                errs.add("Start time must be at least " + MIN_LEAD_MINUTES + " minutes from now.");
            if (start.isAfter(now.plusDays(MAX_ADVANCE_DAYS)))
                errs.add("You can only reserve up to " + MAX_ADVANCE_DAYS + " days ahead.");
        }
        if (minutes < MIN_DURATION || minutes > MAX_DURATION || minutes % DURATION_STEP != 0)
            errs.add("Duration must be " + MIN_DURATION + " to " + MAX_DURATION + " minutes, in steps of " + DURATION_STEP + ".");

        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM stations WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, stationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) errs.add("Station " + stationId + " does not exist.");
            }
        }
        if (!errs.isEmpty()) return errs;   // no point querying bookings with bad input

        Timestamp s = Timestamp.valueOf(start), e = Timestamp.valueOf(start.plusMinutes(minutes));

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status = 'ACTIVE' AND end_time > ?")) {
            ps.setInt(1, userId); ps.setTimestamp(2, Timestamp.valueOf(now));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= MAX_ACTIVE_PER_USER)
                    errs.add("You already have " + MAX_ACTIVE_PER_USER + " active reservations. Cancel one first.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status = 'ACTIVE' AND start_time < ? AND end_time > ?")) {
            ps.setInt(1, userId); ps.setTimestamp(2, e); ps.setTimestamp(3, s);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) errs.add("You already have a reservation that overlaps this time.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM bookings WHERE station_id = ? AND status = 'ACTIVE' AND start_time < ? AND end_time > ?")) {
            ps.setInt(1, stationId); ps.setTimestamp(2, e); ps.setTimestamp(3, s);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= SLOTS_PER_STATION)
                    errs.add("No free slot at this station for that time (" + SLOTS_PER_STATION + " slots, all booked). Try another time.");
            }
        }
        return errs;
    }

    /** FR-15: validates (FR-16) and saves the reservation. Returns the new booking id. */
    public static int reserve(int userId, int stationId, LocalDateTime start, int minutes)
            throws SQLException, ValidationException {
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try {
                List<String> errs = check(c, userId, stationId, start, minutes);
                if (!errs.isEmpty()) { c.rollback(); throw new ValidationException(errs); }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO bookings (user_id, station_id, start_time, end_time, status) VALUES (?,?,?,?, 'ACTIVE')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId); ps.setInt(2, stationId);
                    ps.setTimestamp(3, Timestamp.valueOf(start));
                    ps.setTimestamp(4, Timestamp.valueOf(start.plusMinutes(minutes)));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        int id = keys.getInt(1);
                        c.commit();
                        return id;
                    }
                }
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Bookings list (state.bookings.items). Upcoming-and-active only when upcomingOnly is true. */
    public static List<Models.Booking> list(int userId, boolean upcomingOnly) throws SQLException {
        String sql = "SELECT b.id, b.station_id, s.name, b.start_time, b.end_time, b.status FROM bookings b "
                + "LEFT JOIN stations s ON s.id = b.station_id WHERE b.user_id = ? "
                + (upcomingOnly ? "AND b.status = 'ACTIVE' AND b.end_time > ? " : "")
                + "ORDER BY b.start_time";
        List<Models.Booking> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (upcomingOnly) ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Models.Booking(rs.getInt(1), rs.getInt(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime(), rs.getTimestamp(5).toLocalDateTime(), rs.getString(6)));
            }
        }
        return out;
    }

    /** cancelBooking(id): only your own, still-active, not-yet-started reservations. */
    public static boolean cancel(int userId, int bookingId) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE bookings SET status = 'CANCELLED' WHERE id = ? AND user_id = ? AND status = 'ACTIVE' AND start_time > ?")) {
            ps.setInt(1, bookingId); ps.setInt(2, userId); ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        }
    }
}
