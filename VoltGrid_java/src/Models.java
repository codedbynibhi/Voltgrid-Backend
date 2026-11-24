import java.time.LocalDateTime;

/** Plain data holders shared by the services. */
public final class Models {
    private Models() {}

    public static final class User {
        public final int id;
        public final String name, email;
        public User(int id, String name, String email) { this.id = id; this.name = name; this.email = email; }
    }

    /** Station object shape from the team agreements (id, name, power, type, price, distanceKm, available, rating, queue). */
    public static final class Station {
        public final int id;
        public final String name, type;
        public final double powerKw, pricePerKwh, distanceKm, rating;
        public final boolean available;
        public final int queue;
        public Station(int id, String name, double powerKw, String type, double pricePerKwh,
                       double distanceKm, boolean available, double rating, int queue) {
            this.id = id; this.name = name; this.powerKw = powerKw; this.type = type;
            this.pricePerKwh = pricePerKwh; this.distanceKm = distanceKm;
            this.available = available; this.rating = rating; this.queue = queue;
        }
    }

    /** Vehicle profile (batteryCapacityKwh, maxChargingSpeedKw) plus the simulator's state of charge. */
    public static final class Vehicle {
        public final int id, userId;
        public final String model;
        public final double batteryKwh, maxKw, effKwhPerKm, soc;
        public Vehicle(int id, int userId, String model, double batteryKwh, double maxKw,
                       double effKwhPerKm, double soc) {
            this.id = id; this.userId = userId; this.model = model; this.batteryKwh = batteryKwh;
            this.maxKw = maxKw; this.effKwhPerKm = effKwhPerKm; this.soc = soc;
        }
    }

    public static final class Booking {
        public final int id, stationId;
        public final String stationName, status;
        public final LocalDateTime start, end;
        public Booking(int id, int stationId, String stationName, LocalDateTime start,
                       LocalDateTime end, String status) {
            this.id = id; this.stationId = stationId; this.stationName = stationName;
            this.start = start; this.end = end; this.status = status;
        }
    }
}
