import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * What the user did this run. A LinkedList of text lines, walked with an Iterator / ListIterator,
 * plus an ArrayList remembering reservation ids so the last one can be undone.
 */
public final class ActivityLog {
    private static final List<String> LOG = new LinkedList<>();
    private static final List<Integer> RESERVATIONS = new ArrayList<>();

    private ActivityLog() {}

    public static synchronized void add(String message) {
        LOG.add(LocalTime.now().withNano(0) + "  " + message);
    }

    /** ListIterator going BACKWARDS: newest entry first. */
    public static synchronized void printNewestFirst() {
        if (LOG.isEmpty()) {
            System.out.println("Nothing has happened yet.");
            return;
        }
        ListIterator<String> it = LOG.listIterator(LOG.size());
        while (it.hasPrevious()) System.out.println("  " + it.previous());
    }

    /** Iterator.remove(): the only safe way to delete while looping. Keeps the newest `keep` lines. */
    public static synchronized int trim(int keep) {
        int toRemove = LOG.size() - keep;
        int removed = 0;
        Iterator<String> it = LOG.iterator();
        while (it.hasNext() && removed < toRemove) {
            it.next();
            it.remove();
            removed++;
        }
        return removed;
    }

    public static synchronized int size() {
        return LOG.size();
    }

    public static synchronized void rememberReservation(int bookingId) {
        RESERVATIONS.add(bookingId);
    }

    /** Last reservation made (and forgets it). Returns -1 if there is none. */
    public static synchronized int popLastReservation() {
        if (RESERVATIONS.isEmpty()) return -1;
        return RESERVATIONS.remove(RESERVATIONS.size() - 1);
    }
}
