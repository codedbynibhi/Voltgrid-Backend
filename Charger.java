/**
 * MEMBER 1 - OOP
 * ABSTRACT CLASS: cannot be created with "new". AcCharger and DcCharger extend it (INHERITANCE)
 * and give their own version of connector() (POLYMORPHISM).
 */
public abstract class Charger {
    private static int created = 0;            // STATIC: one copy shared by all chargers

    protected final String name;
    protected final double powerKw;

    // Constructor 1
    public Charger(String name, double powerKw) {
        this.name = name;                      // this.name = the field, name = the parameter
        this.powerKw = powerKw;
        created++;
    }

    // Constructor 2 (CONSTRUCTOR OVERLOADING): calls constructor 1 with this(...)
    public Charger(Station s) {
        this(s.getName(), s.getPowerKw());
    }

    public abstract String connector();        // every subclass MUST write this

    public String safetyNote() {               // subclasses MAY override this
        return "Follow the instructions on the charger.";
    }

    public String describe() {
        return name + " | " + connector() + " | " + powerKw + " kW | " + safetyNote();
    }

    public static int getCreated() {
        return created;
    }

    /** Factory method: gives back the right child class for a station. */
    public static Charger of(Station s) {
        if (s.getType().equals("DC")) return new DcCharger(s);
        return new AcCharger(s);
    }
}
