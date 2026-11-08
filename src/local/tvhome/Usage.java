package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import local.tvhome.core.UsageLog;

/**
 * Counts opens on the TV only (never sent anywhere). Apps are recorded by the Home-button service, devices by the
 * home screen; each process writes its own file so they can't overwrite each other.
 */
final class Usage {
    static final String APPS = "usage-apps.txt";
    static final String INPUTS = "usage-inputs.txt";

    private Usage() {}

    static synchronized void record(Context ctx, String file, String key) {
        try {
            File f = new File(ctx.getFilesDir(), file);
            UsageLog log = UsageLog.decode(read(f));
            log.record(key, System.currentTimeMillis());
            File tmp = new File(ctx.getFilesDir(), file + ".tmp");
            Files.write(tmp.toPath(), log.encode().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
            // Losing one count is fine; crashing the home screen is not.
        }
    }

    /** Today's ranking counts: recomputed the first time the home screen opens each day, then kept fixed. */
    static Map<String, Integer> dailyCounts(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("rank", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (today.equals(prefs.getString("day", null))) return decodeCounts(prefs.getString("counts", ""));
        long now = System.currentTimeMillis();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String file : new String[] {APPS, INPUTS}) {
            counts.putAll(UsageLog.decode(read(new File(ctx.getFilesDir(), file))).counts(now));
        }
        prefs.edit().putString("day", today).putString("counts", encodeCounts(counts)).apply();
        return counts;
    }

    private static String read(File f) {
        try {
            return f.exists() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeCounts(Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) sb.append(e.getKey()).append(' ').append(e.getValue()).append('\n');
        return sb.toString();
    }

    private static Map<String, Integer> decodeCounts(String s) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String line : s.split("\n")) {
            String[] p = line.split(" ");
            if (p.length != 2) continue;
            try {
                out.put(p[0], Integer.parseInt(p[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
