package local.tvhome.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Turns the config plus what's on the TV into the rows the screens show. */
public final class HomeModel {
    private HomeModel() {}

    /** Watch column: config order, uninstalled apps skipped, then the Settings entry. */
    public static List<Row> watch(Config c, Map<String, String> installedLabels) {
        List<Row> rows = new ArrayList<>();
        for (AppEntry a : c.apps) {
            String label = installedLabels.get(a.pkg);
            if (label == null) continue;
            rows.add(new Row(Row.Kind.APP, a.pkg, a.name != null ? a.name : nameOr(label, a.pkg), null));
        }
        rows.add(new Row(Row.Kind.ACTION, "settings", "Settings", null));
        return rows;
    }

    /**
     * Devices column. Configured devices always show (asleep devices report "disconnected"),
     * then unconfigured ports that are connected now or were seen before. Hidden ports never show.
     */
    public static List<Row> devices(Config c, List<DetectedInput> detected, Set<String> seen) {
        Map<String, DetectedInput> byKey = new LinkedHashMap<>();
        for (DetectedInput d : detected) byKey.put(d.key, d);
        List<Row> rows = new ArrayList<>();
        for (InputEntry e : c.inputs.values()) {
            DetectedInput d = byKey.get(e.key);
            if (d == null || c.hidden.contains(e.key)) continue;
            rows.add(new Row(Row.Kind.INPUT, d.inputId, e.name, e.icon));
        }
        for (DetectedInput d : detected) {
            if (c.inputs.containsKey(d.key) || c.hidden.contains(d.key)) continue;
            if (!d.connected && !seen.contains(d.key)) continue;
            String label = d.label == null || d.label.trim().isEmpty()
                    ? "HDMI " + d.key.substring("hdmi".length()) : d.label;
            rows.add(new Row(Row.Kind.INPUT, d.inputId, label, "box"));
        }
        return rows;
    }

    /** Most-opened first; ties keep their current order; the Settings entry always stays last. */
    public static List<Row> sortByUsage(List<Row> rows, Map<String, Integer> counts) {
        List<Row> out = new ArrayList<>();
        Row settings = null;
        for (Row r : rows) {
            if (r.kind == Row.Kind.ACTION && r.id.equals("settings")) settings = r;
            else out.add(r);
        }
        out.sort((a, b) -> Integer.compare(counts.getOrDefault(usageKey(b), 0), counts.getOrDefault(usageKey(a), 0)));
        if (settings != null) out.add(settings);
        return out;
    }

    /** Apps are counted by package; devices by port, since a device's input id changes when it sleeps. */
    public static String usageKey(Row r) {
        return r.kind == Row.Kind.INPUT ? InputPorts.key(InputPorts.port(r.id)) : r.id;
    }

    public static List<Row> settingsMenu() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(Row.Kind.ACTION, "tv_settings", "TV settings", null));
        rows.add(new Row(Row.Kind.ACTION, "all_apps", "All apps", null));
        return rows;
    }

    public static List<Row> allApps(Map<String, String> installedLabels) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : installedLabels.entrySet()) {
            rows.add(new Row(Row.Kind.APP, e.getKey(), nameOr(e.getValue(), e.getKey()), null));
        }
        rows.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return rows;
    }

    /** Some system apps have a blank label; show the package name rather than an empty line. */
    private static String nameOr(String label, String pkg) {
        return label == null || label.trim().isEmpty() ? pkg : label;
    }
}
