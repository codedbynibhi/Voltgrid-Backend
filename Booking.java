import java.time.LocalDateTime;

/** MEMBER 2 - one row of the bookings table (joined with the station name). */
public class Booking {
    private final int id;
    private final String stationName;
    private final LocalDateTime start, end;

    public Booking(int id, String stationName, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.stationName = stationName;
        this.start = start;
        this.end = end;
    }

    public int getId() { return id; }
    public String getStationName() { return stationName; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
}
