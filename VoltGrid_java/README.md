# VoltGrid_java (Member 3: Palak)

Terminal-based Java backend with JDBC (MySQL). No frontend.

| Requirement | Where |
|---|---|
| FR-15 Reserve a slot | `ReservationService.reserve`, menu 1 |
| FR-16 Reservation validation | `ReservationService.check` (inside the reserve transaction) |
| FR-18 Select station and target | `ChargingService.select`, menu 3 |
| FR-19 Run a session | `ChargingService.start` / `ChargingSession.run`, menu 4 |
| FR-20 Live session metrics | `ChargingSession.snapshot`, menu 5 |
| FR-21 Background charging | session runs on its own thread; menu stays usable |
| FR-22 Auto-save session | `ChargingSession.finish` (on completion, stop, or exit/Ctrl+C) |
| FR-23 Simulate a drive | `DriveSimulator`, menu 7 |
| FR-24 Cost calculator | `CostCalculator`, menu 8 |

Also provided for the agreements: bookings list + cancel (`ReservationService.list/cancel`, menu 2),
`selectStation` (`ChargingService.select`), and saving sessions to `charging_sessions` (the `addSession` data).

## Setup (Windows, from D:\Course_Work\VoltGrid_java)

1. Put the MySQL Connector/J jar in `lib\` (same one you use for test.java).
2. Make sure MySQL is running and the `voltgrid` database exists.
3. Compile and run:

```
javac -cp "lib/*" -d out src/*.java
java -cp "out;lib/*" Main
```

DB settings default to `jdbc:mysql://localhost:3306/voltgrid`, user `root`, empty password (as in test.java).
Override with env vars `VOLTGRID_DB_URL`, `VOLTGRID_DB_USER`, `VOLTGRID_DB_PASS`.

Tables are created with `CREATE TABLE IF NOT EXISTS` on start (see `schema.sql`), so teammates' existing tables are not touched.
If the database has no users or stations, the app offers to load demo data.

## Assumptions to confirm with the team

- Column names: `users(id, name, email, phone)`, `stations(id, name, power_kw, type, price_per_kwh, distance_km, available, rating, queue)`,
  `vehicles(user_id, battery_capacity_kwh, max_charging_speed_kw, ...)`. If a teammate's table differs, change `Dao.java`.
- Login is by email only (stand-in for Lubhav's sign-in). Vehicle is asked once if missing (stand-in for Simran's profile).
- Rules in `ReservationService`: 2 slots per station, max 3 active reservations per user, 5 min to 7 days ahead, 15-180 min in 15 min steps, no overlapping own bookings.
- Charging: power = min(station, vehicle max), flat to 80% then tapering to 20% of power at 100%.
- Simulation speed: default 1 real second = 120 simulated seconds (changeable when starting a session).
- Drive: energy = km x kWh/km x style (Eco 0.85, Normal 1.0, Sport 1.2); a 5% reserve is never used.
- Currency is shown as "Rs." (`Fmt.CURRENCY`).
