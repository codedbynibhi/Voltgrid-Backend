/**
 * MEMBER 1 - INTERFACE: only says WHAT a pricing plan must do.
 * FlatTariff and PeakTariff say HOW. Code that holds a "Tariff" does not care which one it is.
 */
public interface Tariff {
    String name();

    double priceFor(double basePrice, int hour);      // hour = 0..23

    // DEFAULT METHOD: implementing classes get this for free
    default String describe() {
        return name();
    }
}
