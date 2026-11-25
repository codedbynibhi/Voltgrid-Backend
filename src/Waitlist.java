import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * People waiting for a free slot at a station: a HashMap (station id -> LinkedList of user ids).
 * First come, first served: add() at the end, remove(0) from the front. Methods are synchronized
 * because more than one thread could use them.
 */
public final class Waitlist {
    private static final Map<Integer, List<Integer>> LISTS = new HashMap<>();

    private Waitlist() {}

    /** Adds the user (once) and returns their position, starting at 1. */
    public static synchronized int join(int stationId, int userId) {
        List<Integer> list = LISTS.get(stationId);
        if (list == null) {
            list = new LinkedList<>();
            LISTS.put(stationId, list);
        }
        if (!list.contains(userId)) list.add(userId);
        return list.indexOf(userId) + 1;
    }

    /** Takes the first person off the list. Returns -1 if nobody is waiting. */
    public static synchronized int next(int stationId) {
        List<Integer> list = LISTS.get(stationId);
        if (list == null || list.isEmpty()) return -1;
        return list.remove(0);
    }

    public static synchronized int size(int stationId) {
        List<Integer> list = LISTS.get(stationId);
        return list == null ? 0 : list.size();
    }
}
