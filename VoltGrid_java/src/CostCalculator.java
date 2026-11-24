import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** FR-24: what a charge costs and how long it takes, for one station or ranked across all stations. */
public final class CostCalculator {
    private CostCalculator() {}

    public static final class Row {
        public final Models.Station station;
        public final ChargingService.Estimate estimate;
        Row(Models.Station station, ChargingService.Estimate estimate) { this.station = station; this.estimate = estimate; }
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

    /** Every station, cheapest first (ties broken by faster charge). */
    public static List<Row> rankAll(List<Models.Station> stations, Models.Vehicle v, double from, double to) {
        List<Row> rows = new ArrayList<>();
        for (Models.Station s : stations) rows.add(one(s, v, from, to));
        rows.sort(Comparator.<Row>comparingDouble(r -> r.estimate.cost).thenComparingDouble(r -> r.estimate.seconds));
        return rows;
    }
}
