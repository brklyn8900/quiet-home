package local.tvhome.core;

/**
 * Decides what to do when the stock Google TV home comes to the front. Right after the TV wakes,
 * go back to what was being watched; any other time (or the second time after waking), show our home.
 */
public final class WakePolicy {
    // A home screen the TV puts up by itself on wake appears within a second or two; a later one is someone pressing Home.
    public static final long WAKE_WINDOW_MS = 3_000;

    public enum Action { SHOW_HOME, RESUME_LAST }

    private WakePolicy() {}

    /** The caller sets wakeHandled = true after the first call that follows a screen-on, whatever the result. */
    public static Action onStockHome(long now, long screenOnAt, boolean wakeHandled, String lastTarget) {
        boolean justWoke = screenOnAt > 0 && now - screenOnAt <= WAKE_WINDOW_MS;
        return !wakeHandled && justWoke && lastTarget != null ? Action.RESUME_LAST : Action.SHOW_HOME;
    }
}
