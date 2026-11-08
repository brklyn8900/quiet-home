package local.tvhome.core;

final class WakePolicyTest {
    static final String PEACOCK = "com.peacocktv.peacockandroid";

    static void run() {
        T.eq(WakePolicy.onStockHome(3_000, 1_000, false, PEACOCK), WakePolicy.Action.RESUME_LAST, "wakeResumesLastApp");
        T.eq(WakePolicy.onStockHome(3_000, 1_000, true, PEACOCK), WakePolicy.Action.SHOW_HOME, "secondHomeAfterWakeShowsHome");
        T.eq(WakePolicy.onStockHome(3_000, 1_000, false, null), WakePolicy.Action.SHOW_HOME, "wakeWithNothingToResumeShowsHome");
        // Found on a real TV: pressing Home ~8 s after waking got Peacock back instead of the home list.
        T.eq(WakePolicy.onStockHome(9_000, 1_000, false, PEACOCK), WakePolicy.Action.SHOW_HOME, "homePressSecondsAfterWakeShowsHome");
        T.eq(WakePolicy.onStockHome(61_000, 1_000, false, PEACOCK), WakePolicy.Action.SHOW_HOME, "homeLongAfterWakeShowsHome");
        T.eq(WakePolicy.onStockHome(5_000, 0, false, PEACOCK), WakePolicy.Action.SHOW_HOME, "neverWokeShowsHome");
    }
}
