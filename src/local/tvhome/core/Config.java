package local.tvhome.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The home screen's list, read from home.conf. One directive per line:
 * <pre>
 * app &lt;package&gt; [display name...]
 * input &lt;hdmiN&gt; &lt;icon&gt; &lt;name...&gt;
 * hide &lt;hdmiN&gt;
 * </pre>
 * Never throws: bad lines are skipped and missing sections fall back to the defaults.
 */
public final class Config {
    public final List<AppEntry> apps;
    public final LinkedHashMap<String, InputEntry> inputs;
    public final Set<String> hidden;

    private Config(List<AppEntry> apps, LinkedHashMap<String, InputEntry> inputs, Set<String> hidden) {
        this.apps = Collections.unmodifiableList(apps);
        this.inputs = inputs;
        this.hidden = Collections.unmodifiableSet(hidden);
    }

    /** Used when home.conf is missing or has no app lines: common streaming apps (uninstalled ones are skipped). */
    public static Config defaults() {
        List<AppEntry> apps = new ArrayList<>();
        for (String p : new String[] {
                "com.google.android.youtube.tv", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom",
                "com.disney.disneyplus", "com.wbd.stream", "com.hulu.livingroomplus", "com.peacocktv.peacockandroid",
                "com.cbs.ott", "com.plexapp.android", "org.jellyfin.androidtv"}) {
            apps.add(new AppEntry(p, null));
        }
        // No device names: plugged-in devices show up under the names they report over HDMI.
        return new Config(apps, new LinkedHashMap<>(), new LinkedHashSet<>());
    }

    public static Config parse(String text) {
        List<AppEntry> apps = new ArrayList<>();
        LinkedHashMap<String, InputEntry> inputs = new LinkedHashMap<>();
        Set<String> hidden = new LinkedHashSet<>();
        if (text != null) {
            for (String raw : text.split("\\r?\\n")) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+", 4);
                String cmd = p[0].toLowerCase(Locale.ROOT);
                if (cmd.equals("app") && p.length >= 2) {
                    String name = p.length >= 3 ? line.split("\\s+", 3)[2] : null;
                    apps.add(new AppEntry(p[1], name));
                } else if (cmd.equals("input") && p.length == 4 && isPortKey(p[1])) {
                    String key = p[1].toLowerCase(Locale.ROOT);
                    inputs.put(key, new InputEntry(key, p[2].toLowerCase(Locale.ROOT), p[3]));
                } else if (cmd.equals("hide") && p.length == 2 && isPortKey(p[1])) {
                    hidden.add(p[1].toLowerCase(Locale.ROOT));
                }
            }
        }
        Config def = defaults();
        if (apps.isEmpty()) apps = new ArrayList<>(def.apps);
        if (inputs.isEmpty() && hidden.isEmpty()) {
            inputs = def.inputs;
            hidden = new LinkedHashSet<>(def.hidden);
        }
        return new Config(apps, inputs, hidden);
    }

    private static boolean isPortKey(String s) {
        return s.toLowerCase(Locale.ROOT).matches("hdmi[1-9]");
    }
}
