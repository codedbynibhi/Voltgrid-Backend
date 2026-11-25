/** MEMBER 1 - evening (18-22) costs 25% more, night (22-06) costs 20% less. */
public class PeakTariff implements Tariff {
    @Override
    public String name() {
        return "Peak / off-peak";
    }

    @Override
    public double priceFor(double basePrice, int hour) {
        if (hour >= 18 && hour < 22) return basePrice * 1.25;
        if (hour >= 22 || hour < 6) return basePrice * 0.80;
        return basePrice;
    }

    @Override
    public String describe() {                 // overrides the default method
        return name() + " (18-22 h x1.25, 22-06 h x0.80)";
    }
}
