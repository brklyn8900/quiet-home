package local.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import local.tvhome.core.Backgrounds;
import local.tvhome.core.Config;
import local.tvhome.core.DetectedInput;
import local.tvhome.core.HomeModel;

/** The home screen: Watch column on the left, Devices on the right, a rotating background behind. */
public class HomeActivity extends Activity {
    private LinearLayout watch;
    private LinearLayout devices;
    private ImageView background;
    private String shownPicture;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable nextPicture = this::showBackground;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        watch = findViewById(R.id.watch_column);
        devices = findViewById(R.id.devices_column);
        background = findViewById(R.id.background);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-read every time so a pushed config shows without a restart.
        Config c = Launcher.loadConfig(this);
        Map<String, Integer> counts = Usage.dailyCounts(this);
        Rows.fill(watch, HomeModel.sortByUsage(HomeModel.watch(c, Launcher.installedApps(this)), counts), this);
        List<DetectedInput> detected = Inputs.detect(this);
        Rows.fill(devices, HomeModel.sortByUsage(HomeModel.devices(c, detected, rememberSeen(detected)), counts), this);
        if (watch.getChildCount() > 0) watch.getChildAt(0).requestFocus();
        showBackground();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(nextPicture);
        super.onPause();
    }

    /** Shows this half hour's picture from files/backgrounds/ (pushed by install.sh), then waits for the next slot. */
    private void showBackground() {
        handler.removeCallbacks(nextPicture);
        try {
            File dir = new File(getExternalFilesDir(null), "backgrounds");
            List<String> names = new ArrayList<>();
            String[] files = dir.list();
            if (files != null) {
                for (String n : files) {
                    String lower = n.toLowerCase();
                    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) names.add(n);
                }
            }
            long now = System.currentTimeMillis();
            // "./install.sh show <name>" writes show-now: the picture's file name, then the time it was asked for.
            String requested = null;
            long askedAt = 0;
            File show = new File(getExternalFilesDir(null), "show-now");
            if (show.exists()) {
                String[] lines = new String(java.nio.file.Files.readAllBytes(show.toPath())).trim().split("\n");
                if (lines.length == 2) {
                    requested = lines[0].trim();
                    askedAt = Long.parseLong(lines[1].trim());
                }
            }
            String pick = Backgrounds.pick(names, now, requested, askedAt);
            if (pick == null) {
                background.setImageDrawable(null);
                shownPicture = null;
            } else if (!pick.equals(shownPicture)) {
                Bitmap bmp = decode(new File(dir, pick));
                background.setImageBitmap(bmp);
                shownPicture = bmp == null ? null : pick;
            }
            handler.postDelayed(nextPicture, Backgrounds.msUntilChange(now, requested, askedAt) + 1000);
        } catch (Exception | OutOfMemoryError e) {
            background.setImageDrawable(null); // a bad picture just means a black background
            shownPicture = null;
        }
    }

    /** Loads a picture at roughly screen size, so a big camera photo can't run the TV out of memory. */
    private static Bitmap decode(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= 1920 && o.outHeight / (sample * 2) >= 1080) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    /** Ports that ever had a device stay listed after it goes to sleep. */
    private Set<String> rememberSeen(List<DetectedInput> detected) {
        SharedPreferences prefs = getSharedPreferences("home", MODE_PRIVATE);
        Set<String> seen = new HashSet<>(prefs.getStringSet("seenInputs", new HashSet<>()));
        for (DetectedInput d : detected) if (d.connected) seen.add(d.key);
        prefs.edit().putStringSet("seenInputs", seen).apply();
        return seen;
    }

    @Override
    public void onBackPressed() {
        // Back must never drop to the stock home screen.
    }
}
