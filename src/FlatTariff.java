/** Same price all day. */
public class FlatTariff implements Tariff {
    @Override
    public String name() {
        return "Flat";
    }

    @Override
    public double pricePerKwh(double basePrice, int hour) {
        return basePrice;
    }
}
