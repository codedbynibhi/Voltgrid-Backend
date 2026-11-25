/** Evening peak costs more (18:00-22:00 x1.25), night costs less (22:00-06:00 x0.80). */
public class PeakTariff implements Tariff {
    @Override
    public String name() {
        return "Peak / off-peak";
    }

    @Override
    public double pricePerKwh(double basePrice, int hour) {
        if (hour >= 18 && hour < 22) return basePrice * 1.25;
        if (hour >= 22 || hour < 6) return basePrice * 0.80;
        return basePrice;
    }

    @Override
    public String describe() {
        return name() + " (18-22 h x1.25, 22-06 h x0.80)";
    }
}
