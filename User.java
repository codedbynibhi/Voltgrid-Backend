/**
 * MEMBER 1 - one row of the users table: the driver AND the driver's car.
 * Private fields + getters = ENCAPSULATION. "soc" = state of charge = battery percentage.
 */
public class User {
    private final int id;
    private final String name, email, carModel;
    private final double batteryKwh, maxKw;
    private double soc;                               // not final: it changes after charging

    public User(int id, String name, String email, String carModel, double batteryKwh, double maxKw, double soc) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.carModel = carModel;
        this.batteryKwh = batteryKwh;
        this.maxKw = maxKw;
        this.soc = soc;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCarModel() { return carModel; }
    public double getBatteryKwh() { return batteryKwh; }
    public double getMaxKw() { return maxKw; }
    public double getSoc() { return soc; }
    public void setSoc(double soc) { this.soc = soc; }
}
