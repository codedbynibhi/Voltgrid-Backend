/** MEMBER 1 - one row of the stations table. Comparable: natural order = nearest first. */
public class Station implements Comparable<Station> {
    private final int id;
    private final String name, type;                  // type is "AC" or "DC"
    private final double powerKw, pricePerKwh, distanceKm;
    private final boolean available;

    public Station(int id, String name, String type, double powerKw, double pricePerKwh,
                   double distanceKm, boolean available) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.powerKw = powerKw;
        this.pricePerKwh = pricePerKwh;
        this.distanceKm = distanceKm;
        this.available = available;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getPowerKw() { return powerKw; }
    public double getPricePerKwh() { return pricePerKwh; }
    public double getDistanceKm() { return distanceKm; }
    public boolean isAvailable() { return available; }

    /** Used by Collections.sort(), Collections.min() and TreeSet. */
    @Override
    public int compareTo(Station other) {
        return Double.compare(this.distanceKm, other.distanceKm);
    }

    @Override
    public String toString() {
        return "#" + id + " " + name;
    }
}
