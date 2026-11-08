package local.tvhome.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HomeModelTest {
    static void run() {
        appsSkipsUninstalled();
        appNameOverrideWins();
        configuredDeviceShownWhenDisconnected();
        newConnectedDeviceAppearsWithItsName();
        hiddenAndNeverSeenStayHidden();
        settingsMenuHasTwoEntries();
        allAppsSortedCaseInsensitive();
        mostUsedFirstSettingsLast();
        devicesSortedByPort();
    }

    static void mostUsedFirstSettingsLast() {
        List<Row> rows = HomeModel.watch(Config.parse("app a\napp b\napp c\napp d"), map("a", "A", "b", "B", "c", "C", "d", "D"));
        List<Row> sorted = HomeModel.sortByUsage(rows, Map.of("c", 5, "b", 2, "d", 2));
        T.eq(labels(sorted), List.of("C", "B", "D", "A", "Settings"), "mostUsedFirstTiesKeepListOrderSettingsLast");
        T.eq(labels(HomeModel.sortByUsage(rows, Map.of())), List.of("A", "B", "C", "D", "Settings"), "noUsageKeepsListOrder");
    }

    static void devicesSortedByPort() {
        Config c = Config.parse("input hdmi3 game PlayStation 5\ninput hdmi4 box Apple TV");
        List<Row> rows = HomeModel.devices(c, List.of(
                new DetectedInput("hdmi3", "x/.ExternalTvInputService/HDMI300004", "", false),
                new DetectedInput("hdmi4", "x/.ExternalTvInputService/HW4", "", true)), Set.of());
        // Usage is keyed by port so it survives the input id switching between the device and the bare port.
        T.eq(labels(HomeModel.sortByUsage(rows, Map.of("hdmi4", 3))), List.of("Apple TV", "PlayStation 5"), "devicesUseCountsByPort");
        T.eq(HomeModel.usageKey(rows.get(1)), "hdmi4", "usageKeyForInputIsPort");
        T.eq(HomeModel.usageKey(new Row(Row.Kind.APP, "com.x", "X", null)), "com.x", "usageKeyForAppIsPackage");
    }

    static List<String> labels(List<Row> rows) {
        List<String> out = new ArrayList<>();
        for (Row r : rows) out.add(r.label);
        return out;
    }

    static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    static void appsSkipsUninstalled() {
        Config c = Config.parse("app a\napp b\napp c");
        List<Row> rows = HomeModel.watch(c, map("a", "A", "c", "C"));
        T.eq(labels(rows), List.of("A", "C", "Settings"), "appsSkipsUninstalled labels");
        Row last = rows.get(rows.size() - 1);
        T.eq(last.kind, Row.Kind.ACTION, "appsSkipsUninstalled settings kind");
        T.eq(last.id, "settings", "appsSkipsUninstalled settings id");
        T.eq(rows.get(0).kind, Row.Kind.APP, "appsSkipsUninstalled app kind");
        T.eq(rows.get(0).id, "a", "appsSkipsUninstalled app id");
    }

    static void appNameOverrideWins() {
        Config c = Config.parse("app a Apple TV app");
        T.eq(HomeModel.watch(c, map("a", "Apple TV")).get(0).label, "Apple TV app", "appNameOverrideWins");
    }

    static void configuredDeviceShownWhenDisconnected() {
        Config c = Config.parse("input hdmi3 game PlayStation 5");
        List<Row> rows = HomeModel.devices(c, List.of(new DetectedInput("hdmi3", "id3", "", false)), Collections.emptySet());
        T.eq(rows.size(), 1, "configuredDeviceShownWhenDisconnected size");
        if (rows.isEmpty()) return;
        T.eq(rows.get(0).label, "PlayStation 5", "configuredDeviceShownWhenDisconnected label");
        T.eq(rows.get(0).id, "id3", "configuredDeviceShownWhenDisconnected id");
        T.eq(rows.get(0).icon, "game", "configuredDeviceShownWhenDisconnected icon");
        T.eq(rows.get(0).kind, Row.Kind.INPUT, "configuredDeviceShownWhenDisconnected kind");
    }

    static void newConnectedDeviceAppearsWithItsName() {
        Config c = Config.parse("hide hdmi1");
        List<Row> rows = HomeModel.devices(c, List.of(new DetectedInput("hdmi2", "id2", "Xbox", true)), Collections.emptySet());
        T.eq(labels(rows), List.of("Xbox"), "newConnectedDevice label");
        if (!rows.isEmpty()) T.eq(rows.get(0).icon, "box", "newConnectedDevice icon");
    }

    static void hiddenAndNeverSeenStayHidden() {
        Config c = Config.parse("hide hdmi1");
        T.eq(HomeModel.devices(c, List.of(new DetectedInput("hdmi1", "id1", "", true)), Collections.emptySet()).size(), 0,
                "hidden connected port stays hidden");
        List<DetectedInput> off = List.of(new DetectedInput("hdmi2", "id2", "", false));
        T.eq(HomeModel.devices(c, off, Collections.emptySet()).size(), 0, "never-seen disconnected port hidden");
        T.eq(labels(HomeModel.devices(c, off, Set.of("hdmi2"))), List.of("HDMI 2"), "seen disconnected port shown");
    }

    static void settingsMenuHasTwoEntries() {
        List<Row> rows = HomeModel.settingsMenu();
        T.eq(labels(rows), List.of("TV settings", "All apps"), "settingsMenu labels");
        T.eq(rows.get(0).id, "tv_settings", "settingsMenu id 0");
        T.eq(rows.get(1).id, "all_apps", "settingsMenu id 1");
    }

    static void allAppsSortedCaseInsensitive() {
        T.eq(labels(HomeModel.allApps(map("x", "beta", "y", "Alpha"))), List.of("Alpha", "beta"), "allAppsSorted");
        T.eq(labels(HomeModel.allApps(map("com.z.app", " ", "a", "Alpha"))), List.of("Alpha", "com.z.app"), "allAppsBlankLabelUsesPackage");
        T.eq(labels(HomeModel.watch(Config.parse("app com.z.app"), map("com.z.app", ""))), List.of("com.z.app", "Settings"), "watchBlankLabelUsesPackage");
    }
}
