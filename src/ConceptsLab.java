import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Short, printable demos of the core Java topics, all using VoltGrid examples. */
public final class ConceptsLab {
    private ConceptsLab() {}

    // ================= OOP =================

    public static void oop() {
        System.out.println("Polymorphism: one Charger[] array, each object answers in its own way.");
        Charger[] chargers = {
                new AcCharger("Campus Slow", 7.4),
                new DcCharger("City Fast", 50),
                new DcCharger("Highway Ultra", 150)
        };
        for (Charger c : chargers) {
            System.out.println("  " + c.describe());
            if (c instanceof DcCharger) System.out.println("    (this one is a DcCharger, category " + c.category() + ")");
        }
        System.out.println("Chargers created so far (static counter): " + Charger.getCreated());

        System.out.println("Interface Tariff, two implementations, price of a Rs.20 base kWh:");
        Tariff[] plans = {new FlatTariff(), new PeakTariff()};
        int[] hours = {10, 19, 2};
        for (Tariff t : plans) {
            StringBuilder line = new StringBuilder("  " + TextUtil.padRight(t.name(), 16));
            for (int h : hours) line.append(String.format("at %02d:00 = %.2f   ", h, t.pricePerKwh(20.0, h)));
            System.out.println(line);
        }
    }

    // ================= arrays =================

    public static void arrays() {
        int[][] table = new int[3][4];                      // 3 rows, 4 columns
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[i].length; j++) table[i][j] = (i + 1) * (j + 1);
        System.out.println("2D array (multiplication table): " + Arrays.deepToString(table));

        int[][] jagged = new int[3][];                      // rows of different length
        jagged[0] = new int[]{1};
        jagged[1] = new int[]{1, 2};
        jagged[2] = new int[]{1, 2, 3};
        System.out.println("Jagged array: " + Arrays.deepToString(jagged));

        double[] prices = {18.0, 24.0, 12.0, 8.0, 26.0};
        double[] copy = Arrays.copyOf(prices, prices.length);
        Arrays.sort(copy);
        System.out.println("Prices:        " + Arrays.toString(prices));
        System.out.println("Sorted copy:   " + Arrays.toString(copy));
        System.out.println("The real project use is ScheduleGrid (int[24][4] day grid and double[stations][7] usage table).");
    }

    // ================= strings and wrappers =================

    public static void strings() {
        String a = "volt";
        String b = "volt";
        String c = new String("volt");
        System.out.println("String pool:   a == b ? " + (a == b) + "   (same literal, one object in the pool)");
        System.out.println("               a == c ? " + (a == c) + "  (new String makes a separate object)");
        System.out.println("               a.equals(c) ? " + a.equals(c) + "  (equals compares the characters)");
        System.out.println("               a == c.intern() ? " + (a == c.intern()) + "  (intern() returns the pooled one)");

        String s = "grid";
        s.toUpperCase();                                    // result thrown away: Strings are immutable
        System.out.println("Immutability: s.toUpperCase() alone leaves s = " + s);
        s = s.toUpperCase();
        System.out.println("              s = s.toUpperCase() gives  " + s);

        String text = "  City Centre Fast Hub  ";
        System.out.println("trim/length:   '" + text.trim() + "' has " + text.trim().length() + " characters");
        System.out.println("indexOf/charAt: 'Fast' starts at " + text.indexOf("Fast") + ", char there = " + text.charAt(text.indexOf("Fast")));
        System.out.println("replace/split:  " + Arrays.toString(text.trim().replace("Fast", "Ultra").split(" ")));
        System.out.println("compareTo:      \"apple\".compareTo(\"banana\") = " + "apple".compareTo("banana")
                + ",  equalsIgnoreCase(\"VOLT\") = " + a.equalsIgnoreCase("VOLT"));
        System.out.println("format:         " + String.format("%-10s|%8.2f|%05d", "Rs.", 18.5, 42));
        System.out.println("titleCase:      " + TextUtil.titleCase("highway  ULTRA charge"));

        // StringBuilder is mutable and fast; StringBuffer is the same but synchronized (thread-safe).
        StringBuilder sb = new StringBuilder("VoltGrid");
        sb.append(" v").append(2).insert(0, ">> ").reverse();
        System.out.println("StringBuilder:  append/insert/reverse -> " + sb);
        sb.reverse().setLength(8);
        System.out.println("                setLength(8) -> " + sb + ", deleteCharAt(0) -> " + sb.deleteCharAt(0));
        StringBuffer shared = new StringBuffer("log:");
        shared.append(" thread-safe");
        System.out.println("StringBuffer:   " + shared);

        long t0 = System.nanoTime();
        String slow = "";
        for (int i = 0; i < 5000; i++) slow += i;           // creates 5000 new Strings
        long slowTime = System.nanoTime() - t0;
        t0 = System.nanoTime();
        StringBuilder fast = new StringBuilder();
        for (int i = 0; i < 5000; i++) fast.append(i);      // one object, changed in place
        long fastTime = System.nanoTime() - t0;
        System.out.println("Joining 5000 numbers: String += took " + slowTime / 1000 + " us, StringBuilder took " + fastTime / 1000 + " us");
    }

    public static void wrappers() {
        int prim = 42;
        Integer boxed = prim;                               // autoboxing
        int back = boxed;                                   // unboxing
        System.out.println("Autoboxing/unboxing: " + prim + " -> " + boxed + " -> " + back);

        Integer x = 127, y = 127, p = 1000, q = 1000;
        System.out.println("Integer cache:  127 == 127 ? " + (x == y) + "   1000 == 1000 ? " + (p == q)
                + "   1000 equals 1000 ? " + p.equals(q));

        System.out.println("parseInt(\"350\") + 1 = " + (Integer.parseInt("350") + 1)
                + ",  Double.parseDouble(\"18.5\") = " + Double.parseDouble("18.5")
                + ",  Boolean.parseBoolean(\"TRUE\") = " + Boolean.parseBoolean("TRUE"));
        System.out.println("valueOf / toString / toBinaryString: " + Integer.valueOf("12") + ", " + Integer.toString(12) + ", " + Integer.toBinaryString(12));
        System.out.println("Limits: Integer.MAX_VALUE = " + Integer.MAX_VALUE + ", Double.MAX_VALUE = " + Double.MAX_VALUE);
        System.out.println("Character: isDigit('7')=" + Character.isDigit('7') + ", isLetter('7')=" + Character.isLetter('7')
                + ", toUpperCase('v')=" + Character.toUpperCase('v'));
        System.out.println("compare: Integer.compare(5, 9) = " + Integer.compare(5, 9) + ", Double.compare(2.5, 2.5) = " + Double.compare(2.5, 2.5));
        Integer missing = null;
        try {
            int boom = missing;                             // unboxing null
            System.out.println(boom);
        } catch (NullPointerException e) {
            System.out.println("Unboxing a null Integer throws NullPointerException.");
        }
    }

    // ================= exceptions =================

    /** Three methods in a chain: the exception travels UP the call stack until somebody catches it. */
    private static void level3(int litres) throws ValidationException {
        if (litres < 0) throw new ValidationException("Energy cannot be negative: " + litres);   // throw = create and raise
        System.out.println("    level3 ok");
    }

    private static void level2(int litres) throws ValidationException {      // throws = "I do not handle it, caller must"
        System.out.println("    level2 calls level3");
        level3(litres);
        System.out.println("    level2 finished");               // skipped when level3 throws
    }

    private static void level1(int litres) {
        try {
            System.out.println("    level1 calls level2");
            level2(litres);
        } catch (ValidationException e) {
            System.out.println("    level1 caught: " + e.getMessage());
        } finally {
            System.out.println("    level1 finally: always runs");
        }
    }

    /** AutoCloseable: used by try-with-resources, closed automatically even if an exception happens. */
    static class Plug implements AutoCloseable {
        private final String name;
        Plug(String name) { this.name = name; System.out.println("    " + name + " opened"); }
        @Override public void close() { System.out.println("    " + name + " closed automatically"); }
    }

    public static void exceptions() {
        System.out.println("Propagation (throw -> throws -> catch), good value then bad value:");
        level1(5);
        level1(-3);

        System.out.println("Unchecked exceptions (RuntimeException family, compiler does not force a catch):");
        try {
            int[] data = new int[2];
            data[Integer.parseInt("5")] = 1;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {      // multi-catch
            System.out.println("    multi-catch caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        try {
            System.out.println(10 / Integer.parseInt("0"));
        } catch (ArithmeticException e) {
            System.out.println("    ArithmeticException: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("    other runtime problem");
        }

        System.out.println("Checked exception (compiler forces try/catch or throws): ValidationException, StationUnavailableException");
        try {
            throw new StationUnavailableException("Airport Express");
        } catch (VoltGridException e) {                      // catches any of our own checked exceptions
            System.out.println("    caught as parent type VoltGridException: " + e.getMessage());
        }

        System.out.println("Exception chaining (wrap a low level error, keep the cause):");
        try {
            try {
                Integer.parseInt("abc");
            } catch (NumberFormatException low) {
                throw new VoltGridException("Could not read the battery size", low);
            }
        } catch (VoltGridException e) {
            System.out.println("    " + e.getMessage() + "  <-  cause: " + e.getCause().getClass().getSimpleName());
        }

        System.out.println("try-with-resources:");
        try (Plug p1 = new Plug("plug-1"); Plug p2 = new Plug("plug-2")) {
            System.out.println("    using both plugs");
            throw new IllegalStateException("cable fault");
        } catch (IllegalStateException e) {
            System.out.println("    caught: " + e.getMessage() + " (plugs were already closed, last opened first)");
        }
    }

    // ================= collections =================

    public static void collections() {
        System.out.println("List: ArrayList (fast get by index) vs LinkedList (fast add/remove at the ends)");
        List<String> arr = new ArrayList<>(Arrays.asList("AC", "DC Fast", "DC Ultra", "AC"));
        List<String> linked = new LinkedList<>(arr);
        arr.add(1, "Type2");
        linked.remove(0);
        System.out.println("  ArrayList  " + arr + ", get(2) = " + arr.get(2));
        System.out.println("  LinkedList " + linked + ", indexOf(\"AC\") = " + linked.indexOf("AC"));

        System.out.println("Set: duplicates removed. Order differs:");
        Set<String> hash = new HashSet<>(arr);
        Set<String> linkedHash = new LinkedHashSet<>(arr);
        Set<String> tree = new TreeSet<>(arr);
        System.out.println("  HashSet       " + hash + "  (no guaranteed order)");
        System.out.println("  LinkedHashSet " + linkedHash + "  (insertion order)");
        System.out.println("  TreeSet       " + tree + "  (sorted)");

        System.out.println("Map: same keys, three orders:");
        Map<String, Double> h = new HashMap<>();
        Map<String, Double> lh = new LinkedHashMap<>();
        Map<String, Double> t = new TreeMap<>();
        String[] names = {"Mall", "Airport", "Campus", "City"};
        double[] kw = {22, 120, 7.4, 50};
        for (int i = 0; i < names.length; i++) {
            h.put(names[i], kw[i]);
            lh.put(names[i], kw[i]);
            t.put(names[i], kw[i]);
        }
        System.out.println("  HashMap       " + h);
        System.out.println("  LinkedHashMap " + lh);
        System.out.println("  TreeMap       " + t);
        System.out.println("  get(\"City\") = " + h.get("City") + ", containsKey(\"Mall\") = " + h.containsKey("Mall")
                + ", getOrDefault(\"None\", 0.0) = " + h.getOrDefault("None", 0.0));
        for (Map.Entry<String, Double> e : t.entrySet()) System.out.println("    entry " + e.getKey() + " -> " + e.getValue() + " kW");

        System.out.println("Iterator / ListIterator:");
        List<Integer> soc = new ArrayList<>(Arrays.asList(5, 40, 12, 80, 3));
        Iterator<Integer> it = soc.iterator();
        while (it.hasNext()) if (it.next() < 10) it.remove();      // safe removal while looping
        System.out.println("  after removing values below 10: " + soc);
        ListIterator<Integer> li = soc.listIterator();
        while (li.hasNext()) { int v = li.next(); li.set(v + 5); }  // change elements in place
        StringBuilder back = new StringBuilder("  backwards: ");
        while (li.hasPrevious()) back.append(li.previous()).append(' ');
        System.out.println(back);

        System.out.println("Collections utility methods:");
        List<Integer> nums = new ArrayList<>(Arrays.asList(30, 10, 50, 20, 10));
        Collections.sort(nums);
        System.out.println("  sort " + nums + ", binarySearch(30) = " + Collections.binarySearch(nums, 30));
        Collections.reverse(nums);
        System.out.println("  reverse " + nums + ", max = " + Collections.max(nums) + ", min = " + Collections.min(nums)
                + ", frequency(10) = " + Collections.frequency(nums, 10));
        Collections.swap(nums, 0, nums.size() - 1);
        Collections.shuffle(nums, new Random(1));
        System.out.println("  swap + shuffle(seed 1) " + nums);
        List<Integer> readOnly = Collections.unmodifiableList(nums);
        try {
            readOnly.add(99);
        } catch (UnsupportedOperationException e) {
            System.out.println("  unmodifiableList: add() throws UnsupportedOperationException");
        }
        System.out.println("The real uses are StationCatalog, Recommender, Waitlist and ActivityLog.");
    }
}
