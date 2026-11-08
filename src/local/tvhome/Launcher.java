package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.provider.Settings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import local.tvhome.core.Config;
import local.tvhome.core.HomeModel;
import local.tvhome.core.Row;

/** Reads the config, lists installed apps, and opens what a row points at. */
final class Launcher {
    private Launcher() {}

    static Config loadConfig(Context ctx) {
        try {
            File f = new File(ctx.getExternalFilesDir(null), "home.conf");
            return Config.parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Config.parse(null);
        }
    }

    /** Package name to label for every launchable app; TV (leanback) entries win over phone ones. */
    static Map<String, String> installedApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Map<String, String> out = new LinkedHashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(ctx.getPackageName()) || out.containsKey(pkg)) continue;
                out.put(pkg, String.valueOf(ri.loadLabel(pm)));
            }
        }
        return out;
    }

    static void open(Context ctx, Row row) {
        try {
            switch (row.kind) {
                case APP:
                    openApp(ctx, row.id);
                    break;
                case INPUT:
                    Usage.record(ctx, Usage.INPUTS, HomeModel.usageKey(row));
                    // Handled by Sony's TV app (com.sony.dtv.tvx), which switches to that HDMI port.
                    ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                            TvContract.buildChannelUriForPassthroughInput(row.id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                case ACTION:
                    if (row.id.equals("tv_settings")) {
                        ctx.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } else {
                        ctx.startActivity(new Intent(ctx, ListActivity.class)
                                .putExtra(ListActivity.MODE, row.id.equals("all_apps") ? "apps" : "settings"));
                    }
                    break;
            }
        } catch (Exception ignored) {
            // A missing app or a refused intent must never crash the home screen.
        }
    }

    static boolean openApp(Context ctx, String pkg) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return true;
    }
}
