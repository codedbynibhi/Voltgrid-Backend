/**
 * OOP: an INTERFACE. It only says WHAT a pricing plan must do; FlatTariff and PeakTariff say HOW.
 * Code that uses a Tariff variable does not care which plan is behind it (polymorphism).
 */
public interface Tariff {
    String name();

    /** Price of one kWh at the given hour of the day (0-23), starting from the station's base price. */
    double pricePerKwh(double basePrice, int hour);

    /** default method: shared behaviour that implementing classes get for free. */
    default String describe() {
        return name();
    }
}
