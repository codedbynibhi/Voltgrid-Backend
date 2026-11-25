/** Console stand-in for the team's notify({ type, message }) toast action. */
public final class Notify {
    private Notify() {}

    public static synchronized void toast(String type, String message) {
        System.out.println();
        System.out.println("[" + type.toUpperCase() + "] " + message);
    }
}
