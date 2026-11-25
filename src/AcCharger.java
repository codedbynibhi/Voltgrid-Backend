/** Slow AC charger (inheritance: AcCharger IS-A Charger). */
public class AcCharger extends Charger {
    public AcCharger(int id, String name, double powerKw) {
        super(id, name, powerKw);
    }

    public AcCharger(String name, double powerKw) {
        super(name, powerKw);
    }

    @Override
    public String connector() {
        return "Type 2";
    }

    @Override
    public String category() {
        return "AC";
    }
}
