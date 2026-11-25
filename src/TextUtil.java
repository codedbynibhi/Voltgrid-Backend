/**
 * String helpers. Syllabus: String methods, immutability, StringBuilder, comparison, search, formatting.
 * A String never changes - every method below that "changes" text returns a NEW String.
 */
public final class TextUtil {
    private TextUtil() {}

    /** Simple e-mail check using String methods only (no regex). */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String e = email.trim();
        if (e.length() < 5 || e.contains(" ")) return false;
        int at = e.indexOf('@');
        if (at < 1 || at != e.lastIndexOf('@')) return false;   // exactly one '@', not first
        int dot = e.lastIndexOf('.');
        return dot > at + 1 && dot < e.length() - 1;            // a dot after '@', not last
    }

    /** "city centre  hub" -> "City Centre Hub". Uses StringBuilder because we append in a loop. */
    public static String titleCase(String text) {
        if (text == null) return "";
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static boolean containsIgnoreCase(String text, String part) {
        return text.toLowerCase().contains(part.toLowerCase());
    }

    public static String padRight(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    /** One CSV line; values containing a comma or quote are wrapped in quotes. */
    public static String csvLine(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i];
            if (v.contains(",") || v.contains("\"")) v = "\"" + v.replace("\"", "\"\"") + "\"";
            sb.append(v);
        }
        return sb.toString();
    }
}
