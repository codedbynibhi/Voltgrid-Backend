/** MEMBER 1 - fast DC charger. Also overrides safetyNote() (METHOD OVERRIDING). */
public class DcCharger extends Charger {
    public DcCharger(Station s) {
        super(s);
    }

    @Override
    public String connector() {
        return "CCS2";
    }

    @Override
    public String safetyNote() {
        return "High power: do not unplug while charging.";
    }
}
