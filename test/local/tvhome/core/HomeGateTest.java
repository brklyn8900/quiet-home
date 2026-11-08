package local.tvhome.core;

final class HomeGateTest {
    static final String MAIN = "com.google.android.apps.tv.launcherx.home.HomeActivity";

    static void run() {
        HomeGate g = new HomeGate();
        T.check(g.onStockHome(10_000), "firstStockHomeRedirects");
        // On the TV one Home press gives up to two main-screen events ~50 ms apart.
        T.check(!g.onStockHome(10_050), "duplicateEventForSamePressIgnored");
        g.onOurWindow();
        T.check(g.onStockHome(10_500), "doublePressRedirectsAgain");
        // Seen on the TV: the second press landed 0.5 s later, before our screen had appeared.
        T.check(g.onStockHome(11_000), "secondPressBeforeOurScreenAppearsRedirects");

        T.check(HomeGate.isMainHome(HomeGate.STOCK_HOME, MAIN), "mainHomeScreenCounts");
        T.check(!HomeGate.isMainHome(HomeGate.STOCK_HOME, "com.google.android.apps.tv.launcherx.dashboard.DashboardActivity"),
                "stockHomeDashboardIsLeftAlone");
        T.check(!HomeGate.isMainHome("com.other", MAIN), "otherPackageIgnored");
        T.check(!HomeGate.isMainHome(HomeGate.STOCK_HOME, null), "missingClassIgnored");

        T.check(HomeGate.redirectOnConnect(60_000, false), "justBootedOpensHome");
        T.check(!HomeGate.redirectOnConnect(600_000, false), "lateReconnectLeavesScreenAlone");
        T.check(!HomeGate.redirectOnConnect(60_000, true), "crashFlagBlocksBootRedirect");
    }
}
