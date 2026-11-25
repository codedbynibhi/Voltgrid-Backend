/**
 * A meter several threads add to. Shows the RACE CONDITION and its fix.
 * count++ is really three steps (read, add, write); two threads can interleave and lose an update.
 */
public class EnergyMeter {
    private int unsafeTotal = 0;
    private int safeTotal = 0;
    private final Object lock = new Object();

    /** NOT thread-safe: this is a critical section with no protection. */
    public void addUnsafe() {
        unsafeTotal++;
    }

    /** Synchronized METHOD: locks on "this" (the intrinsic lock), so only one thread at a time runs it. */
    public synchronized void addSafe() {
        safeTotal++;
    }

    /** Synchronized BLOCK: same idea, but only the critical part is locked. */
    public void addSafeBlock() {
        // work that needs no lock can happen here
        synchronized (lock) {
            safeTotal++;
        }
    }

    public int getUnsafeTotal() {
        return unsafeTotal;
    }

    public synchronized int getSafeTotal() {
        return safeTotal;
    }
}
