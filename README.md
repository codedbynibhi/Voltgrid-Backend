# VoltGrid_java 

Terminal-based Java backend with JDBC (MySQL). No frontend.

Menu options 1-9 are the original features. Options 10-14 were added to cover the Programming in Java syllabus.

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

## Syllabus coverage (where to find each topic)

New menu options 10-14 open the extra screens. The syllabus items are used in the real features and
also have printable demos (options 12, 13 and 14) that are useful for a viva.

| Syllabus topic | Where in the code | How to see it run |
|---|---|---|
| Classes, objects, constructors (overloading, `this(...)`), `this`, `static` | `Charger` (static counter, two constructors), `Models` | option 13 > 1 |
| Inheritance, polymorphism, abstract class | `Charger` (abstract) with `AcCharger`, `DcCharger` | station list "Plug" column, option 10 > 3 |
| Interfaces, default method | `Tariff` with `FlatTariff`, `PeakTariff` | option 8 (pricing plan), option 13 > 1 |
| Multi-dimensional arrays | `ScheduleGrid` (`int[24][4]` day grid, `double[stations][7]` usage table), jagged array demo | option 10 > 7, option 11 > 2, option 13 > 2 |
| Strings: immutability, pool, search, compare, format | `TextUtil` (email check, `titleCase`, CSV line), `ConceptsLab.strings` | login check, option 13 > 3 |
| `StringBuilder` / `StringBuffer` | `TextUtil`, `ScheduleGrid`, CSV export in `ExtraMenu` | option 13 > 3 |
| Wrapper classes, autoboxing | `ConceptsLab.wrappers`, `Input` (parseInt/parseDouble) | option 13 > 3 |
| Exceptions: checked/unchecked, try/catch/finally, multi-catch, throw/throws, propagation, custom exceptions, chaining, try-with-resources | `VoltGridException`, `ValidationException`, `StationUnavailableException`, `InsufficientChargeException`; `DriveSimulator`, `ChargingService.start`, `Main.drive`/`startSession`; `ConceptsLab.exceptions` | option 7 with a huge distance, option 13 > 4 |
| `ArrayList`, `LinkedList` | `StationCatalog`, `ActivityLog`, `Waitlist` | option 10, option 11 > 4 |
| `HashSet`, `LinkedHashSet`, `TreeSet` | `StationCatalog` (station types) | option 10 > 6 |
| `HashMap`, `LinkedHashMap`, `TreeMap` | `StationCatalog` (lookup by id, recently viewed, A-Z by name), `Waitlist` | option 10 > 3, 4, 6 |
| `Iterator`, `ListIterator` | `StationCatalog.search`, `ActivityLog` (backwards walk, `remove`) | option 10 > 2, option 11 > 4 |
| `Comparable` and Collections utilities (`sort`, `reverse`, `min`, `max`, `frequency`, `binarySearch`, `shuffle`, `unmodifiableList`) | `Models.Station`, `Recommender.Scored`, `CostCalculator.Row`, `StationCatalog`, `ConceptsLab.collections` | option 10 > 1, option 8, option 13 > 5 |
| Threads: `Thread`/`Runnable`, `start()` vs `run()`, `sleep`, `join`, `interrupt`, states, daemon, priority | `ChargingSession` (the real charging thread), `ConcurrencyLab` | options 4 and 6, option 12 > 1-4 |
| Race condition, critical section, `synchronized` method/block, intrinsic lock, `volatile` | `EnergyMeter`, `ChargingSession` (`synchronized`, `volatile stopRequested`) | option 12 > 5 |
| `wait()`, `notify()`, `notifyAll()`, guarded block | `ChargingBay` (vehicles sharing plugs) | option 12 > 6 |
| Producer-consumer | `BoundedBuffer` (meter and billing threads) | option 12 > 7 |
| Concurrency vs parallelism | `ConcurrencyLab.sequentialVsConcurrent` | option 12 > 8 |
| JDBC architecture, driver, URL, `DriverManager`, `Connection` | `Db`, `JdbcLab.info` | option 14 > 1 |
| `Statement` vs `PreparedStatement`, parameter binding, SQL injection | `Dao`, `ReservationService`, `JdbcLab.injectionDemo` | option 14 > 4 |
| `executeQuery`, `executeUpdate`, CRUD | `Dao.addStation` / `listStations` / `updateStation` / `deleteStation` | option 10 > 9, 10, 11 |
| `ResultSet`: cursor iteration, typed getters, scrollable, `wasNull`, metadata | `Dao.listSessions`, `JdbcLab.scrollDemo`, `JdbcLab.dumpTable` | option 14 > 2, 3, option 11 > 1 |
| Transactions, batch, rollback, resource management (try-with-resources) | `ReservationService.reserve`, `JdbcLab.batchAndRollback` | option 1, option 14 > 5 |

Not used on purpose: `Queue`, `Deque`, `PriorityQueue` and `Comparator` (kept out to keep the code simple).
Sorting uses `Comparable` (`compareTo`) instead.

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
