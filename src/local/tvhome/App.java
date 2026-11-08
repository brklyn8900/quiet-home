package local.tvhome;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.List;

import local.tvhome.core.CrashGuard;

/** Counts crashes; after three in five minutes the Home-button redirect switches itself off. */
public class App extends Application {
    static final String CRASH_FLAG = "disabled-after-crashes";

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                recordCrash(this, System.currentTimeMillis());
            } catch (Throwable ignored) {
                // Never let the crash bookkeeping hide the real crash.
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    static void recordCrash(Context ctx, long now) throws Exception {
        SharedPreferences prefs = ctx.getSharedPreferences("home", MODE_PRIVATE);
        List<Long> times = CrashGuard.record(CrashGuard.decode(prefs.getString("crashes", null)), now);
        prefs.edit().putString("crashes", CrashGuard.encode(times)).commit();
        if (CrashGuard.shouldDisable(times, now)) {
            new File(ctx.getExternalFilesDir(null), CRASH_FLAG).createNewFile();
        }
    }

    /** True once the crash guard has tripped; cleared only by deleting the file over adb. */
    static boolean redirectDisabled(Context ctx) {
        File dir = ctx.getExternalFilesDir(null);
        return dir != null && new File(dir, CRASH_FLAG).exists();
    }
}
