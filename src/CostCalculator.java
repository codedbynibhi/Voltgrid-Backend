import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** FR-24: what a charge costs and how long it takes, for one station or ranked across all stations. */
public final class CostCalculator {
    private CostCalculator() {}

    /** Comparable: cheaper first, and for equal cost the faster charge first. */
    public static final class Row implements Comparable<Row> {
        public final Models.Station station;
        public final ChargingService.Estimate estimate;
        Row(Models.Station station, ChargingService.Estimate estimate) { this.station = station; this.estimate = estimate; }

        @Override public int compareTo(Row other) {
            int byCost = Double.compare(this.estimate.cost, other.estimate.cost);
            return byCost != 0 ? byCost : Double.compare(this.estimate.seconds, other.estimate.seconds);
        }
    }

    public static String validate(Models.Vehicle v, double from, double to) {
        if (from < 0 || from >= 100) return "Start charge must be from 0% to below 100%.";
        if (to <= from || to > 100) return "End charge must be above the start charge and at most 100%.";
        if (v.batteryKwh <= 0) return "Vehicle battery capacity is not set.";
        return null;
    }

    public static Row one(Models.Station s, Models.Vehicle v, double from, double to) {
        return new Row(s, ChargingService.estimate(s, v, from, to));
    }

    public static Row one(Models.Station s, Models.Vehicle v, double from, double to, Tariff tariff, int startHour) {
        return new Row(s, ChargingService.estimate(s, v, from, to, tariff, startHour));
    }

    /** Every station, cheapest first (ties broken by faster charge). */
    public static List<Row> rankAll(List<Models.Station> stations, Models.Vehicle v, double from, double to) {
        return rankAll(stations, v, from, to, new FlatTariff(), 0);
    }

    public static List<Row> rankAll(List<Models.Station> stations, Models.Vehicle v, double from, double to,
                                    Tariff tariff, int startHour) {
        List<Row> rows = new ArrayList<>();
        for (Models.Station s : stations) rows.add(one(s, v, from, to, tariff, startHour));
        Collections.sort(rows);
        return rows;
    }
}
