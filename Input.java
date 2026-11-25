import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * MEMBER 4 - reading from the keyboard. Every method keeps asking until the answer is valid.
 * Uses WRAPPER CLASSES: Integer.parseInt, Double.parseDouble, and a Double parameter that may be null.
 */
public class Input {
    private static final Scanner SC = new Scanner(System.in);
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String line(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) {                  // keyboard closed (Ctrl+D / end of input)
            System.exit(0);
        }
        return SC.nextLine().trim();
    }

    public static int integer(String prompt, int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(line(prompt));          // String -> int
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException e) {
                // not a number: fall through and ask again
            }
            System.out.println("  Enter a whole number from " + min + " to " + max + ".");
        }
    }

    /** If the user just presses Enter, the default is used (pass null for "no default"). */
    public static double decimal(String prompt, double min, double max, Double defaultValue) {
        while (true) {
            String text = line(prompt);
            if (text.isEmpty() && defaultValue != null) return defaultValue;   // auto-unboxing Double -> double
            try {
                double value = Double.parseDouble(text);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException e) {
                // ask again
            }
            System.out.println("  Enter a number from " + min + " to " + max + ".");
        }
    }

    public static boolean yesNo(String prompt) {
        while (true) {
            String s = line(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y")) return true;
            if (s.equals("n")) return false;
        }
    }

    /** Accepts "yyyy-MM-dd HH:mm" or only "HH:mm" (means today). Returns null if it cannot be read. */
    public static LocalDateTime dateTime(String prompt) {
        String s = line(prompt);
        try {
            if (s.matches("\\d{1,2}:\\d{2}")) {
                return LocalDateTime.of(LocalDate.now(), LocalTime.parse(s.length() == 4 ? "0" + s : s));
            }
            return LocalDateTime.parse(s, FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
