/**
 * The plugs of one station. A vehicle must acquire() a plug before charging and release() it after.
 * If every plug is taken the thread WAITS (wait/notifyAll with a guarded block).
 */
public class ChargingBay {
    private int free;

    public ChargingBay(int plugs) {
        this.free = plugs;
    }

    public synchronized void acquire(String who) throws InterruptedException {
        while (free == 0) {                 // guarded block: always re-check the condition in a loop
            System.out.println("    " + who + " waits (all plugs busy)");
            wait();                         // gives up the lock and sleeps until notified
        }
        free--;
        System.out.println("    " + who + " plugs in   (free plugs: " + free + ")");
    }

    public synchronized void release(String who) {
        free++;
        System.out.println("    " + who + " unplugs    (free plugs: " + free + ")");
        notifyAll();                        // wake the waiting threads so they re-check
    }
}
