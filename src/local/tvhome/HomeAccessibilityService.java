package local.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Set;

import local.tvhome.core.HomeGate;
import local.tvhome.core.WakePolicy;

/**
 * Google TV won't let another app be the default home, so this watches for the stock home's main screen
 * coming to the front and replaces it with ours. Right after the TV wakes it resumes what was on instead.
 */
public class HomeAccessibilityService extends AccessibilityService {
    static final String TAG = "TvHome";
    static final String SONY_TV = "com.sony.dtv.tvx";

    private final HomeGate gate = new HomeGate();
    private String lastTarget;
    private long screenOnAt; // uptime ms
    private boolean wakeHandled = true;
    private Set<String> apps;
    private long appsLoadedAt;

    private final BroadcastReceiver screenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            screenOnAt = SystemClock.elapsedRealtime();
            wakeHandled = false;
        }
    };

    @Override
    protected void onServiceConnected() {
        registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
        try {
            if (HomeGate.redirectOnConnect(SystemClock.elapsedRealtime(), App.redirectDisabled(this))) showHome();
        } catch (Exception e) {
            Log.w(TAG, "boot redirect failed", e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(screenOn);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // A crash here would switch the redirect off for ~30 minutes (Android's restart backoff), so never throw.
        try {
            handle(event);
        } catch (Exception e) {
            Log.w(TAG, "event failed", e);
        }
    }

    private void handle(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        String cls = event.getClassName() == null ? null : event.getClassName().toString();
        if (HomeGate.isMainHome(pkg, cls)) {
            onStockHome();
        } else if (pkg.equals(getPackageName())) {
            gate.onOurWindow();
            lastTarget = null; // the viewer was on our home screen; nothing to resume.
        } else if (pkg.equals(SONY_TV) || launchable().contains(pkg)) {
            // A new app on screen counts as one open (devices are counted by the home screen when picked).
            if (!pkg.equals(lastTarget) && !pkg.equals(SONY_TV)) Usage.record(this, Usage.APPS, pkg);
            lastTarget = pkg;
            wakeHandled = true; // something is already on screen after waking; nothing left to resume
        }
    }

    private void onStockHome() {
        if (App.redirectDisabled(this)) return;
        long now = SystemClock.elapsedRealtime();
        if (!gate.onStockHome(now)) return;
        WakePolicy.Action action = WakePolicy.onStockHome(now, screenOnAt, wakeHandled, lastTarget);
        wakeHandled = true;
        if (action == WakePolicy.Action.RESUME_LAST && Launcher.openApp(this, lastTarget)) return;
        showHome();
    }

    private void showHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    /** Installed launchable apps, refreshed at most once a minute. */
    private Set<String> launchable() {
        long now = SystemClock.elapsedRealtime();
        if (apps == null || now - appsLoadedAt > 60_000) {
            apps = Launcher.installedApps(this).keySet();
            appsLoadedAt = now;
        }
        return apps;
    }

    @Override
    public void onInterrupt() {}
}
