import java.util.List;

/**
 * MEMBER 1 - cost estimates. Uses the Tariff INTERFACE (polymorphism) and a 2D ARRAY.
 */
public class CostCalculator {

    /** Energy needed to go from one battery % to another. */
    public static double energyKwh(User u, double fromPct, double toPct) {
        return Math.max(0, toPct - fromPct) / 100.0 * u.getBatteryKwh();
    }

    /** Hours needed: the slower of the car and the charger decides the power. */
    public static double hours(Station s, User u, double energyKwh) {
        double kw = Math.min(s.getPowerKw(), u.getMaxKw());
        return energyKwh / kw;
    }

    /**
     * 2D ARRAY: table[row][column] = cost at station 'row' when charging up to targets[column].
     * 'plan' may be a FlatTariff or a PeakTariff: we only know it is a Tariff (POLYMORPHISM).
     */
    public static double[][] costTable(List<Station> stations, User u, Tariff plan, int hour, double[] targets) {
        double[][] table = new double[stations.size()][targets.length];
        for (int row = 0; row < stations.size(); row++) {
            Station s = stations.get(row);
            double price = plan.priceFor(s.getPricePerKwh(), hour);
            for (int col = 0; col < targets.length; col++) {
                table[row][col] = energyKwh(u, u.getSoc(), targets[col]) * price;
            }
        }
        return table;
    }
}
