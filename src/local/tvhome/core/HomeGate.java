package local.tvhome.core;

/**
 * Decides when the stock Google TV home should be replaced by ours. Times are uptime milliseconds
 * (SystemClock.elapsedRealtime), so a TV clock correction can't freeze the redirect.
 */
public final class HomeGate {
    public static final String STOCK_HOME = "com.google.android.apps.tv.launcherx";
    public static final String STOCK_HOME_SCREEN = "com.google.android.apps.tv.launcherx.home.HomeActivity";
    // One Home press produces up to two events ~50 ms apart; a person can't press twice that fast.
    public static final long PENDING_MS = 300;
    public static final long BOOT_WINDOW_MS = 180_000;

    private boolean pending;
    private long pendingAt;

    /** Only the stock home's main screen; its dashboard, profile picker and settings are left alone. */
    public static boolean isMainHome(String pkg, String className) {
        return STOCK_HOME.equals(pkg) && STOCK_HOME_SCREEN.equals(className);
    }

    /** True when we should redirect now. Repeat events for the same Home press are ignored until ours shows. */
    public boolean onStockHome(long now) {
        if (pending && now - pendingAt < PENDING_MS) return false;
        pending = true;
        pendingAt = now;
        return true;
    }

    /** Our screen came up, so the next stock-home appearance is a new Home press. */
    public void onOurWindow() {
        pending = false;
    }

    /** Right after boot the stock home is already up before we start, so open ours once. */
    public static boolean redirectOnConnect(long uptime, boolean disabled) {
        return !disabled && uptime < BOOT_WINDOW_MS;
    }
}
