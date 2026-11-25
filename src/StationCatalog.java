import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Station collections in one place (Collections Framework):
 * ArrayList (ordered list), HashMap (fast lookup by id), TreeMap (sorted by name),
 * LinkedHashSet (unique, keeps first-seen order), TreeSet (unique, sorted),
 * LinkedHashMap (access order = "recently viewed" list), Iterator, and Collections utility methods.
 */
public class StationCatalog {
    private final List<Models.Station> all = new ArrayList<>();
    private final Map<Integer, Models.Station> byId = new HashMap<>();
    private final TreeMap<String, Models.Station> byName = new TreeMap<>();
    private final Set<String> typesInOrder = new LinkedHashSet<>();
    private final Set<String> typesSorted = new TreeSet<>();
    private final List<String> typeOfEachStation = new ArrayList<>();

    /** LinkedHashMap with accessOrder=true: every get/put moves the entry to the end; keeps only 5. */
    private static final Map<Integer, Models.Station> RECENT =
            new LinkedHashMap<Integer, Models.Station>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Models.Station> eldest) {
                    return size() > 5;
                }
            };

    public StationCatalog(List<Models.Station> stations) {
        for (Models.Station s : stations) {
            all.add(s);
            byId.put(s.id, s);
            byName.put(s.name.toLowerCase(), s);
            typesInOrder.add(s.type);        // a Set ignores duplicates
            typesSorted.add(s.type);
            typeOfEachStation.add(s.type);
        }
    }

    public int size() {
        return all.size();
    }

    public List<Models.Station> all() {
        return Collections.unmodifiableList(all);        // read-only view
    }

    public Models.Station getById(int id) {
        return byId.get(id);
    }

    /** Nearest first: Station implements Comparable, so Collections.sort knows the order. */
    public List<Models.Station> sortedByDistance() {
        List<Models.Station> copy = new ArrayList<>(all);
        Collections.sort(copy);
        return copy;
    }

    public List<Models.Station> furthestFirst() {
        List<Models.Station> copy = sortedByDistance();
        Collections.reverse(copy);
        return copy;
    }

    /** A-Z by name: a TreeMap keeps its keys sorted. */
    public List<Models.Station> sortedByName() {
        return new ArrayList<>(byName.values());
    }

    /** Case-insensitive "contains" search, using an Iterator. */
    public List<Models.Station> search(String text) {
        List<Models.Station> found = new ArrayList<>();
        Iterator<Models.Station> it = all.iterator();
        while (it.hasNext()) {
            Models.Station s = it.next();
            if (TextUtil.containsIgnoreCase(s.name, text) || TextUtil.containsIgnoreCase(s.type, text)) found.add(s);
        }
        return found;
    }

    public Models.Station nearestAvailable() {
        for (Models.Station s : sortedByDistance()) {
            if (s.available) return s;
        }
        return null;
    }

    // ---- recently viewed (LinkedHashMap) ----

    public void markViewed(Models.Station s) {
        RECENT.put(s.id, s);
    }

    public List<Models.Station> recentlyViewed() {
        List<Models.Station> list = new ArrayList<>(RECENT.values());
        Collections.reverse(list);                        // newest first
        return list;
    }

    // ---- sets ----

    public Set<String> typesInFirstSeenOrder() {
        return typesInOrder;
    }

    public Set<String> typesSorted() {
        return typesSorted;
    }

    /** Collections.frequency counts equal elements in a list. */
    public int countOfType(String type) {
        return Collections.frequency(typeOfEachStation, type);
    }
}
