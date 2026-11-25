/**
 * OOP: an ABSTRACT class. It cannot be instantiated; AcCharger and DcCharger extend it (inheritance)
 * and give their own versions of the abstract methods (polymorphism).
 */
public abstract class Charger {
    private static int created = 0;          // static: one value shared by all chargers

    protected final int id;
    protected final String name;
    protected final double powerKw;

    /** Main constructor. */
    public Charger(int id, String name, double powerKw) {
        this.id = id;                         // this.x = the field, x = the parameter
        this.name = name;
        this.powerKw = powerKw;
        created++;
    }

    /** Overloaded constructor: calls the main one using this(...). */
    public Charger(String name, double powerKw) {
        this(0, name, powerKw);
    }

    // ---- abstract: every subclass MUST provide these ----
    public abstract String connector();
    public abstract String category();

    // ---- normal method that subclasses MAY override ----
    public String safetyNote() {
        return "Follow the instructions on the charger.";
    }

    public String describe() {
        return String.format("%s [%s, %s, %.1f kW] - %s", name, category(), connector(), powerKw, safetyNote());
    }

    @Override
    public String toString() {
        return describe();
    }

    public static int getCreated() {
        return created;
    }

    /** Factory: picks the right subclass for a station row. */
    public static Charger of(Models.Station s) {
        if (s.type.toUpperCase().startsWith("DC")) {
            return new DcCharger(s.id, s.name, s.powerKw);
        }
        return new AcCharger(s.id, s.name, s.powerKw);
    }
}
