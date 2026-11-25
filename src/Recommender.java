import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Best charger for you": a weighted score (the same weights as the React app).
 * speed 30%, distance 20%, availability 20%, price 15%, rating 10%, queue 5%.
 */
public final class Recommender {
    public static final double W_SPEED = 0.30, W_DISTANCE = 0.20, W_AVAILABLE = 0.20,
            W_PRICE = 0.15, W_RATING = 0.10, W_QUEUE = 0.05;

    private Recommender() {}

    /** A station with its score. Comparable: higher score sorts first. */
    public static final class Scored implements Comparable<Scored> {
        public final Models.Station station;
        public final double score;      // 0 to 100

        Scored(Models.Station station, double score) {
            this.station = station;
            this.score = score;
        }

        @Override
        public int compareTo(Scored other) {
            return Double.compare(other.score, this.score);
        }
    }

    public static List<Scored> top(List<Models.Station> stations, Models.Vehicle v, int n) {
        List<Scored> result = new ArrayList<>();
        if (stations.isEmpty()) return result;

        double maxSpeed = 0, maxDist = 0, minPrice = Double.MAX_VALUE, maxPrice = 0;
        int maxQueue = 0;
        for (Models.Station s : stations) {
            maxSpeed = Math.max(maxSpeed, Math.min(s.powerKw, v.maxKw));
            maxDist = Math.max(maxDist, s.distanceKm);
            minPrice = Math.min(minPrice, s.pricePerKwh);
            maxPrice = Math.max(maxPrice, s.pricePerKwh);
            maxQueue = Math.max(maxQueue, s.queue);
        }

        for (Models.Station s : stations) {
            double speed = maxSpeed > 0 ? Math.min(s.powerKw, v.maxKw) / maxSpeed : 0;
            double dist = maxDist > 0 ? 1.0 - s.distanceKm / maxDist : 1.0;
            double avail = s.available ? 1.0 : 0.0;
            double price = maxPrice > minPrice ? 1.0 - (s.pricePerKwh - minPrice) / (maxPrice - minPrice) : 1.0;
            double rating = s.rating / 5.0;
            double queue = maxQueue > 0 ? 1.0 - (double) s.queue / maxQueue : 1.0;
            double score = 100 * (W_SPEED * speed + W_DISTANCE * dist + W_AVAILABLE * avail
                    + W_PRICE * price + W_RATING * rating + W_QUEUE * queue);
            result.add(new Scored(s, score));
        }
        Collections.sort(result);
        return new ArrayList<>(result.subList(0, Math.min(n, result.size())));
    }
}
