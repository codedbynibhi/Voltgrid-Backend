/** MEMBER 1 - same price all day. */
public class FlatTariff implements Tariff {
    @Override
    public String name() {
        return "Flat";
    }

    @Override
    public double priceFor(double basePrice, int hour) {
        return basePrice;
    }
}
