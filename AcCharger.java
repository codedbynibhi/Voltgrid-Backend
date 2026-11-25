/** MEMBER 1 - slow AC charger. "extends" = AcCharger IS-A Charger (INHERITANCE). */
public class AcCharger extends Charger {
    public AcCharger(Station s) {
        super(s);                              // call the parent constructor
    }

    @Override
    public String connector() {
        return "Type 2";
    }
}
