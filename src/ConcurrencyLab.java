/**
 * Multithreading demos. Every method prints what happens so it can be explained in a viva.
 * Topics: Thread vs Runnable, start() vs run(), sleep, join, interrupt, thread states, daemon threads,
 * priorities, race condition, synchronized, wait/notify, producer-consumer, concurrency vs parallelism.
 */
public final class ConcurrencyLab {
    private ConcurrencyLab() {}

    private static String me() {
        return Thread.currentThread().getName();
    }

    // ---------- 1. start() vs run(), Thread vs Runnable ----------

    /** Way 1: extend Thread. */
    static class HelloThread extends Thread {
        HelloThread(String name) { super(name); }
        @Override public void run() { System.out.println("    run() executing on thread: " + me()); }
    }

    /** Way 2 (preferred): implement Runnable and hand it to a Thread. */
    static class HelloTask implements Runnable {
        @Override public void run() { System.out.println("    run() executing on thread: " + me()); }
    }

    public static void startVsRun() throws InterruptedException {
        System.out.println("Calling run() directly - NO new thread, runs on the caller:");
        new HelloThread("worker-A").run();
        System.out.println("Calling start() - JVM creates a new thread and then calls run() on it:");
        Thread a = new HelloThread("worker-A");
        Thread b = new Thread(new HelloTask(), "worker-B");
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Both finished. Main thread name: " + me());
    }

    // ---------- 2. lifecycle ----------

    public static void lifecycle() throws InterruptedException {
        // NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED
        Thread t = new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { /* ignore */ }
        }, "sleeper");
        System.out.println("Before start():        " + t.getState());
        t.start();
        Thread.sleep(100);
        System.out.println("While in sleep(400):   " + t.getState());
        t.join();
        System.out.println("After join():          " + t.getState());

        // BLOCKED: wants a lock somebody else holds.  WAITING: called wait() and needs notify().
        final Object lock = new Object();
        Thread holder = new Thread(() -> {
            synchronized (lock) {
                try { Thread.sleep(500); } catch (InterruptedException e) { /* ignore */ }
            }
        }, "holder");
        Thread blocked = new Thread(() -> {
            synchronized (lock) { System.out.println("    blocked thread finally got the lock"); }
        }, "blocked");
        holder.start();
        Thread.sleep(100);
        blocked.start();
        Thread.sleep(100);
        System.out.println("Thread wanting a held lock: " + blocked.getState());
        holder.join();
        blocked.join();

        final Object monitor = new Object();
        Thread waiter = new Thread(() -> {
            synchronized (monitor) {
                try { monitor.wait(); } catch (InterruptedException e) { /* ignore */ }
            }
        }, "waiter");
        waiter.start();
        Thread.sleep(100);
        System.out.println("Thread inside wait():       " + waiter.getState());
        synchronized (monitor) { monitor.notify(); }
        waiter.join();
        System.out.println("After notify() and finish:  " + waiter.getState());
    }

    // ---------- 3. interrupt ----------

    public static void interruptDemo() throws InterruptedException {
        Thread charger = new Thread(() -> {
            try {
                System.out.println("    charging... (sleeping up to 10 s)");
                Thread.sleep(10000);
                System.out.println("    finished normally");
            } catch (InterruptedException e) {
                System.out.println("    interrupted! Stopping early and saving what was delivered.");
            }
        }, "charging-thread");
        charger.start();
        Thread.sleep(500);
        System.out.println("Main calls interrupt()");
        charger.interrupt();
        charger.join();
        System.out.println("Thread alive after join? " + charger.isAlive());
    }

    // ---------- 4. daemon threads and priorities ----------

    public static void daemonAndPriority() throws InterruptedException {
        Thread logger = new Thread(() -> {
            while (true) {
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
            }
        }, "background-logger");
        logger.setDaemon(true);       // must be set BEFORE start()
        System.out.println("background-logger isDaemon = " + logger.isDaemon()
                + "  (a daemon thread does not stop the JVM from exiting)");
        logger.start();

        Thread low = new Thread(new HelloTask(), "low-priority");
        Thread high = new Thread(new HelloTask(), "high-priority");
        low.setPriority(Thread.MIN_PRIORITY);
        high.setPriority(Thread.MAX_PRIORITY);
        System.out.println("Priorities: low=" + low.getPriority() + ", normal=" + Thread.NORM_PRIORITY
                + ", high=" + high.getPriority() + "  (only a hint; the OS scheduler decides the real order)");
        low.start();
        high.start();
        low.join();
        high.join();
    }

    // ---------- 5. race condition ----------

    static class Adder extends Thread {
        private final EnergyMeter meter;
        private final int times;
        Adder(EnergyMeter meter, int times) { this.meter = meter; this.times = times; }
        @Override public void run() {
            for (int i = 0; i < times; i++) {
                meter.addUnsafe();
                meter.addSafe();
            }
        }
    }

    public static void raceCondition() throws InterruptedException {
        EnergyMeter meter = new EnergyMeter();
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Adder(meter, 50000);
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        System.out.println("Expected total:           200000");
        System.out.println("Without synchronized:     " + meter.getUnsafeTotal() + "   (updates were lost - race condition)");
        System.out.println("With synchronized method: " + meter.getSafeTotal());
    }

    // ---------- 6. wait / notify: vehicles share the plugs of one station ----------

    static class VehicleTask implements Runnable {
        private final String name;
        private final ChargingBay bay;
        private final EnergyMeter meter;
        private final int chargeMillis;
        VehicleTask(String name, ChargingBay bay, EnergyMeter meter, int chargeMillis) {
            this.name = name; this.bay = bay; this.meter = meter; this.chargeMillis = chargeMillis;
        }
        @Override public void run() {
            try {
                bay.acquire(name);
                try {
                    Thread.sleep(chargeMillis);        // "charging"
                    for (int i = 0; i < 10; i++) meter.addSafeBlock();   // each vehicle adds 10 kWh
                } finally {
                    bay.release(name);                 // always give the plug back
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void fleet() throws InterruptedException {
        int vehicles = 5, plugs = 2;
        System.out.println(vehicles + " vehicles arrive at a station with " + plugs + " plugs:");
        ChargingBay bay = new ChargingBay(plugs);
        EnergyMeter meter = new EnergyMeter();
        Thread[] threads = new Thread[vehicles];
        for (int i = 0; i < vehicles; i++) {
            String name = "Car-" + (i + 1);
            threads[i] = new Thread(new VehicleTask(name, bay, meter, 400 + i * 100), name);
            threads[i].start();
            Thread.sleep(50);
        }
        for (Thread t : threads) t.join();
        System.out.println("All done. Total energy recorded: " + meter.getSafeTotal() + " kWh (expected " + vehicles * 10 + ")");
    }

    // ---------- 7. producer-consumer ----------

    public static void producerConsumer() throws InterruptedException {
        final BoundedBuffer buffer = new BoundedBuffer(3);
        final int readings = 8;
        final int[] billed = new int[1];

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= readings; i++) {
                    buffer.put(i * 5);                      // meter reading in Wh
                    System.out.println("    meter produced reading " + (i * 5) + " Wh   (buffer size " + buffer.size() + ")");
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) { /* stop */ }
        }, "meter");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < readings; i++) {
                    int value = buffer.take();
                    billed[0] += value;
                    System.out.println("    billing consumed reading " + value + " Wh");
                    Thread.sleep(150);                       // slower than the producer, so the buffer fills up
                }
            } catch (InterruptedException e) { /* stop */ }
        }, "billing");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Billing total: " + billed[0] + " Wh (expected 180)");
    }

    // ---------- 8. concurrency vs parallelism ----------

    public static void sequentialVsConcurrent() throws InterruptedException {
        int tasks = 4, millis = 300;
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < tasks; i++) Thread.sleep(millis);
        long sequential = System.currentTimeMillis() - t0;

        Thread[] threads = new Thread[tasks];
        t0 = System.currentTimeMillis();
        for (int i = 0; i < tasks; i++) {
            threads[i] = new Thread(() -> {
                try { Thread.sleep(millis); } catch (InterruptedException e) { /* ignore */ }
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        long concurrent = System.currentTimeMillis() - t0;

        System.out.println(tasks + " tasks of " + millis + " ms each:");
        System.out.println("  one after another (sequential): about " + sequential + " ms");
        System.out.println("  all at once (multithreaded):    about " + concurrent + " ms");
        System.out.println("CPU cores here: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Concurrency = many tasks in progress at the same time (can be one core switching).");
        System.out.println("Parallelism = tasks really running at the same instant on different cores.");
    }
}
