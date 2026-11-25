/** DC fast charger. Overrides safetyNote() as well (method overriding). */
public class DcCharger extends Charger {
    public DcCharger(int id, String name, double powerKw) {
        super(id, name, powerKw);
    }

    public DcCharger(String name, double powerKw) {
        super(name, powerKw);
    }

    @Override
    public String connector() {
        return "CCS2";
    }

    @Override
    public String category() {
        return powerKw >= 100 ? "DC Ultra" : "DC Fast";
    }

    @Override
    public String safetyNote() {
        return "High power: do not unplug while charging.";
    }
}
