/**
 * MEMBER 4 - STRINGS.
 * A String never changes (IMMUTABLE): every method here that "changes" text gives back a NEW String.
 * StringBuilder is used when we build text in a loop (it changes itself, so it is faster).
 */
public class TextUtil {

    /** Simple e-mail check using only String methods: one '@', and a '.' after it. */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.length() < 5 || e.contains(" ")) return false;
        int at = e.indexOf('@');
        if (at < 1 || at != e.lastIndexOf('@')) return false;      // exactly one '@', not at the start
        int dot = e.lastIndexOf('.');
        return dot > at + 1 && dot < e.length() - 1;               // a dot after '@', not at the end
    }

    /** "city centre  hub" -> "City Centre Hub" */
    public static String titleCase(String text) {
        String[] words = text.trim().split("\\s+");                // split on one or more spaces
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static boolean containsIgnoreCase(String text, String part) {
        return text.toLowerCase().contains(part.toLowerCase());
    }

    /** Progress bar: bar(50, 10) -> [#####-----] */
    public static String bar(double percent, int width) {
        int filled = (int) Math.round(percent / 100.0 * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? '#' : '-');
        }
        return sb.append(']').toString();
    }

    /** 3725 seconds -> "01:02:05" */
    public static String hms(double seconds) {
        long s = Math.round(seconds);
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
