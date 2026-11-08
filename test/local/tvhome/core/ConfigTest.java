package local.tvhome.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class ConfigTest {
    static void run() {
        parsesAppsInOrder();
        parsesInputsAndHidden();
        ignoresCommentsBlankAndUnknown();
        parseGarbageFallsBackToDefaults();
        noAppLinesKeepsDefaultAppsButUsesInputs();
        defaultsMatchShippedFile();
        defaultsNameNoDevices();
    }

    static List<String> pkgs(Config c) {
        List<String> out = new ArrayList<>();
        for (AppEntry a : c.apps) out.add(a.pkg);
        return out;
    }

    static void parsesAppsInOrder() {
        Config c = Config.parse("app a.b\napp c.d My Name");
        T.eq(c.apps.get(0).pkg, "a.b", "parsesAppsInOrder first pkg");
        T.eq(c.apps.get(0).name, null, "parsesAppsInOrder first name");
        T.eq(c.apps.get(1).name, "My Name", "parsesAppsInOrder second name");
    }

    static void parsesInputsAndHidden() {
        Config c = Config.parse("input HDMI3 game PlayStation 5\nhide hdmi1");
        T.eq(c.inputs.get("hdmi3").name, "PlayStation 5", "parsesInputsAndHidden name");
        T.eq(c.inputs.get("hdmi3").icon, "game", "parsesInputsAndHidden icon");
        T.check(c.hidden.contains("hdmi1"), "parsesInputsAndHidden hidden");
    }

    static void ignoresCommentsBlankAndUnknown() {
        Config c = Config.parse("# x\n\nfoo bar\napp a.b");
        T.eq(pkgs(c), List.of("a.b"), "ignoresCommentsBlankAndUnknown");
    }

    static void parseGarbageFallsBackToDefaults() {
        List<String> def = pkgs(Config.defaults());
        for (String s : new String[] {"", null, "\u0000%%%"}) {
            Config c = Config.parse(s);
            T.eq(pkgs(c), def, "parseGarbageFallsBackToDefaults apps for " + s);
            T.eq(new ArrayList<>(c.inputs.keySet()), List.of(), "parseGarbageFallsBackToDefaults inputs for " + s);
        }
    }

    static void noAppLinesKeepsDefaultAppsButUsesInputs() {
        Config c = Config.parse("input hdmi2 box Xbox");
        T.eq(pkgs(c), pkgs(Config.defaults()), "noAppLines default apps");
        T.eq(c.inputs.get("hdmi2").name, "Xbox", "noAppLines input name");
    }

    // Devices show up under the names they report over HDMI; the defaults must not assume anyone's setup.
    static void defaultsNameNoDevices() {
        T.eq(Config.defaults().inputs.size(), 0, "defaultsNameNoDevices inputs");
        T.eq(Config.defaults().hidden.size(), 0, "defaultsNameNoDevices hidden");
        T.check(ConfigTest.pkgs(Config.defaults()).contains("com.netflix.ninja"), "defaultsIncludeCommonApps");
    }

    static void defaultsMatchShippedFile() {
        String path = System.getProperty("conf");
        T.check(path != null, "defaultsMatchShippedFile needs -Dconf");
        if (path == null) return;
        try {
            String text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            Config shipped = Config.parse(text);
            T.eq(pkgs(shipped), pkgs(Config.defaults()), "defaultsMatchShippedFile apps");
            T.eq(new ArrayList<>(shipped.inputs.keySet()), new ArrayList<>(Config.defaults().inputs.keySet()), "defaultsMatchShippedFile inputs");
            T.eq(shipped.hidden, Config.defaults().hidden, "defaultsMatchShippedFile hidden");
        } catch (Exception e) {
            T.check(false, "defaultsMatchShippedFile read " + e);
        }
    }
}
