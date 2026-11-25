import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * MEMBER 4 - COLLECTIONS FRAMEWORK. The same stations are stored in different collections,
 * because each collection is good at something different.
 *   ArrayList : ordered list, fast access by position
 *   HashMap   : very fast lookup by key (station id)
 *   TreeMap   : keys always kept SORTED (station name A-Z)
 *   HashSet   : no duplicates, no order      TreeSet : no duplicates, sorted
 */
public class StationCatalog {
    private final List<Station> all = new ArrayList<>();
    private final Map<Integer, Station> byId = new HashMap<>();          // int id is auto-boxed to Integer
    private final TreeMap<String, Station> byName = new TreeMap<>();
    private final Set<String> typesHash = new HashSet<>();
    private final Set<String> typesSorted = new TreeSet<>();

    public StationCatalog(List<Station> stations) {
        for (Station s : stations) {
            all.add(s);
            byId.put(s.getId(), s);
            byName.put(s.getName().toLowerCase(), s);
            typesHash.add(s.getType());                                  // a Set ignores duplicates
            typesSorted.add(s.getType());
        }
    }

    public Station getById(int id) {
        return byId.get(id);
    }

    /** Nearest first. Station implements Comparable, so Collections.sort knows how to order it. */
    public List<Station> sortedByDistance() {
        List<Station> copy = new ArrayList<>(all);
        Collections.sort(copy);
        return copy;
    }

    /** Furthest first: the same sorted list, turned around. */
    public List<Station> furthestFirst() {
        List<Station> copy = sortedByDistance();
        Collections.reverse(copy);
        return copy;
    }

    /** A-Z: a TreeMap keeps its keys sorted, so we only read the values. */
    public List<Station> sortedByName() {
        return new ArrayList<>(byName.values());
    }

    /** Case-insensitive search by name or type, using an ITERATOR. */
    public List<Station> search(String text) {
        List<Station> found = new ArrayList<>();
        Iterator<Station> it = all.iterator();
        while (it.hasNext()) {
            Station s = it.next();
            if (TextUtil.containsIgnoreCase(s.getName(), text) || TextUtil.containsIgnoreCase(s.getType(), text)) {
                found.add(s);
            }
        }
        return found;
    }

    public Station nearest() {
        return Collections.min(all);                                     // smallest by compareTo = nearest
    }

    public Station farthest() {
        return Collections.max(all);
    }

    public Set<String> typesHash() {
        return typesHash;
    }

    public Set<String> typesSorted() {
        return typesSorted;
    }
}
