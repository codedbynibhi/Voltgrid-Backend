/**
 * MEMBER 3 - two small demos (menu option 9) that you can run and explain in the viva.
 */
public class ThreadDemo {

    // Way 1 to make a thread: extend Thread
    static class HelloThread extends Thread {
        HelloThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            System.out.println("  run() is executing on thread: " + Thread.currentThread().getName());
        }
    }

    /** A shared energy counter. One method is safe, the other is not. */
    static class EnergyMeter {
        private int unsafeTotal = 0, safeTotal = 0;

        void addUnsafe() {
            int temp = unsafeTotal;             // 1) read
            Thread.yield();                     // (lets the other thread run now, so the problem shows on every computer)
            unsafeTotal = temp + 1;             // 2) write: may overwrite what the other thread just wrote
        }

        synchronized void addSafe() {
            safeTotal++;                        // only one thread at a time may run this method
        }
    }

    /** Demo 1: start() vs run(), Thread vs Runnable, join(), sleep(), interrupt(), thread states. */
    public static void startVsRun() throws InterruptedException {
        System.out.println("A) Calling run() directly: NO new thread is created (it runs on main):");
        new HelloThread("worker-A").run();

        System.out.println("B) Calling start(): the JVM creates a new thread and calls run() on it:");
        Thread a = new HelloThread("worker-A");
        // Way 2 (preferred): give a Runnable (here a lambda) to a Thread
        Thread b = new Thread(() -> System.out.println("  run() (Runnable) is on thread: " + Thread.currentThread().getName()), "worker-B");
        System.out.println("  state before start(): " + a.getState());          // NEW
        a.start();
        b.start();
        a.join();                                                              // wait until a is finished
        b.join();
        System.out.println("  state after join():   " + a.getState());          // TERMINATED

        System.out.println("C) interrupt() wakes a sleeping thread (this is how 'Stop charging' works):");
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("  sleeper was interrupted");
            }
        }, "sleeper");
        sleeper.start();
        Thread.sleep(200);
        System.out.println("  state while in sleep(): " + sleeper.getState());  // TIMED_WAITING
        sleeper.interrupt();
        sleeper.join();
    }

    /** Demo 2: race condition - the unsafe total is usually wrong, the synchronized one is right. */
    public static void raceCondition() throws InterruptedException {
        EnergyMeter meter = new EnergyMeter();
        Runnable job = () -> {
            for (int i = 0; i < 20000; i++) {
                meter.addUnsafe();
                meter.addSafe();
            }
        };
        Thread t1 = new Thread(job);
        Thread t2 = new Thread(job);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("  Expected total:           40000");
        System.out.println("  Without synchronized:     " + meter.unsafeTotal + "   (updates were lost)");
        System.out.println("  With synchronized method: " + meter.safeTotal);
    }
}
