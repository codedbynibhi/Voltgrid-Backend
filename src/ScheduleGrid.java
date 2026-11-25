import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Multi-dimensional arrays.
 * occupancy(): int[24][4]  = 24 hours x four 15-minute slots, each cell counts bookings.
 * weekdayUsage(): double[stations][7] = kWh delivered per station per weekday.
 */
public final class ScheduleGrid {
    private ScheduleGrid() {}

    public static int[][] occupancy(List<Models.Booking> bookings, LocalDate day) {
        int[][] grid = new int[24][4];
        for (int hour = 0; hour < 24; hour++) {
            for (int q = 0; q < 4; q++) {
                LocalDateTime from = day.atTime(hour, q * 15);
                LocalDateTime to = from.plusMinutes(15);
                for (Models.Booking b : bookings) {
                    if (b.start.isBefore(to) && b.end.isAfter(from)) grid[hour][q]++;
                }
            }
        }
        return grid;
    }

    /** '.' free, '+' some slots taken, '#' every slot taken. */
    public static void print(String stationName, int slots, int[][] grid) {
        System.out.println("Schedule for " + stationName + " (" + slots + " slots)   . free   + partly booked   # full");
        System.out.println("Hour   :00 :15 :30 :45");
        for (int hour = 0; hour < grid.length; hour++) {
            StringBuilder row = new StringBuilder(String.format("%02d:00 ", hour));
            for (int q = 0; q < grid[hour].length; q++) {
                char c = grid[hour][q] == 0 ? '.' : (grid[hour][q] >= slots ? '#' : '+');
                row.append("  ").append(c).append(' ');
            }
            System.out.println(row);
        }
    }

    /** Rows = stations (same order as the list), columns = Monday..Sunday. */
    public static double[][] weekdayUsage(List<Models.SessionRecord> records, List<Models.Station> stations) {
        double[][] kwh = new double[stations.size()][7];
        for (Models.SessionRecord r : records) {
            int row = -1;
            for (int i = 0; i < stations.size(); i++) {
                if (stations.get(i).id == r.stationId) row = i;
            }
            if (row >= 0) {
                int col = r.date.getDayOfWeek().getValue() - 1;   // Monday = 1 -> column 0
                kwh[row][col] += r.energyKwh;
            }
        }
        return kwh;
    }

    public static void printUsage(List<Models.Station> stations, double[][] kwh) {
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        StringBuilder head = new StringBuilder(TextUtil.padRight("Station", 24));
        for (String d : days) head.append(String.format("%7s", d));
        head.append(String.format("%9s", "Total"));
        System.out.println(head);

        double[] columnTotals = new double[7];
        double grand = 0;
        for (int i = 0; i < kwh.length; i++) {
            StringBuilder row = new StringBuilder(TextUtil.padRight(stations.get(i).name, 24));
            double rowTotal = 0;
            for (int j = 0; j < kwh[i].length; j++) {
                row.append(String.format("%7.1f", kwh[i][j]));
                rowTotal += kwh[i][j];
                columnTotals[j] += kwh[i][j];
            }
            grand += rowTotal;
            row.append(String.format("%9.1f", rowTotal));
            System.out.println(row);
        }
        StringBuilder total = new StringBuilder(TextUtil.padRight("Total (kWh)", 24));
        for (double t : columnTotals) total.append(String.format("%7.1f", t));
        total.append(String.format("%9.1f", grand));
        System.out.println(total);
    }
}
