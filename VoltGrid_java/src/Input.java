import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Console input helpers. Every helper re-asks until it gets a valid value. */
public final class Input {
    private static final BufferedReader IN = new BufferedReader(new InputStreamReader(System.in));
    public static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Input() {}

    public static String line(String prompt) {
        System.out.print(prompt);
        try {
            String s = IN.readLine();
            if (s == null) { System.out.println(); System.exit(0); }
            return s.trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns true (and consumes the line) if the user has pressed Enter since the last read. */
    public static boolean enterPressed() {
        try {
            if (IN.ready()) { IN.readLine(); return true; }
        } catch (IOException ignored) { }
        return false;
    }

    public static int integer(String prompt, int min, int max) {
        while (true) {
            String s = line(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a whole number from " + min + " to " + max + ".");
        }
    }

    /** Blank input returns the default. */
    public static double decimal(String prompt, double min, double max, Double def) {
        while (true) {
            String s = line(prompt);
            if (s.isEmpty() && def != null) return def;
            try {
                double v = Double.parseDouble(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a number from " + min + " to " + max + ".");
        }
    }

    public static boolean yesNo(String prompt) {
        while (true) {
            String s = line(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
        }
    }

    /** Accepts "yyyy-MM-dd HH:mm" or just "HH:mm" (today). Returns null if blank/invalid. */
    public static LocalDateTime dateTime(String prompt) {
        String s = line(prompt);
        try {
            if (s.matches("\\d{1,2}:\\d{2}")) return LocalDateTime.of(LocalDate.now(), LocalTime.parse(s.length() == 4 ? "0" + s : s));
            return LocalDateTime.parse(s, DT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
