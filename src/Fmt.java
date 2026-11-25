import java.util.Locale;

/** Small formatting helpers for terminal output. */
public final class Fmt {
    public static final String CURRENCY = "Rs. ";
    private Fmt() {}

    public static String money(double v) { return String.format(Locale.US, "%s%.2f", CURRENCY, v); }
    public static String kwh(double v)   { return String.format(Locale.US, "%.2f kWh", v); }
    public static String kw(double v)    { return String.format(Locale.US, "%.1f kW", v); }
    public static String pct(double v)   { return String.format(Locale.US, "%.1f%%", v); }

    public static String hms(double seconds) {
        long s = Math.round(seconds);
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    public static String bar(double percent, int width) {
        int filled = (int) Math.round(Math.max(0, Math.min(100, percent)) / 100.0 * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) sb.append(i < filled ? '#' : '-');
        return sb.append(']').toString();
    }
}
