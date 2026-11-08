# Instructions for AI agents

These instructions are for an AI coding agent (Claude Code, Codex, Cursor, …) that is either **setting up Quiet Home on someone's TV** or **changing its code**. Read `README.md` first for what the app does.

You'll be driving a real TV in someone's home, often while they're nearby. Be careful with it.

## Ground rules

- **Ask before changing the TV.** Installing, enabling the accessibility service, disabling or uninstalling any app, and rebooting all need the person's OK. Propose a list, then act.
- **Don't interrupt what they're watching.** Before anything that changes the screen (opening apps, pressing keys, launching the home screen), check `adb shell dumpsys media_session | grep "state=3"`. A match means something is playing, so ask first.
- **Never disable the stock launcher** (`com.google.android.apps.tv.launcherx` or your TV's equivalent). It's the fallback when Quiet Home steps aside.
- **Never disable the apps the person uses.** Peacock, Sling, ESPN and the like should keep working after every change. Open one to check.
- **Keep `keystore/` safe.** Updates must be signed with the same key. `./build.sh` refuses to make a new key unless you pass `--new-key`, and a new key means uninstalling first, which wipes the person's list and usage counts.
- **On the computer, stop processes only by a PID you just verified**, never with `pkill -f` or `killall`. On macOS, `pkill` treats a misplaced option as a second pattern, and that once killed almost every app on someone's Mac.
- **Nothing leaves the TV.** The app has no permissions and no network code. Keep it that way: no analytics, no crash reporting, no fetching pictures or icons from the internet.

## Useful adb checks

```bash
adb connect <tv-ip>:5555                                    # the TV shows an "allow debugging" prompt the first time
adb -s $TV shell dumpsys window | grep mCurrentFocus        # which app is in front
adb -s $TV shell dumpsys media_session | grep "state=3"     # is something playing?
adb -s $TV exec-out screencap -p > shot.png                 # screenshot (DRM video and some apps capture black)
adb -s $TV shell dumpsys accessibility | grep "Bound services"   # is the Home-button service running?
adb -s $TV shell input keyevent KEYCODE_HOME                # press Home (DPAD_UP/DOWN/LEFT/RIGHT/CENTER, BACK)
```

## Setting up a TV

1. **Connect and identify.** Get make, model and Android version:

   ```bash
   adb -s $TV shell getprop ro.product.manufacturer
   adb -s $TV shell getprop ro.product.model
   adb -s $TV shell getprop ro.build.version.release
   ```

   Quiet Home was built on a Sony BRAVIA running Android TV 12. Say what's different.
2. **Check the three device-specific spots** (see README → Other TVs). Adapt the code if yours differ, with a test for any new pattern in `core/`.
   - **Stock home screen.** Press Home, then `dumpsys window | grep mCurrentFocus`. It must match `HomeGate.STOCK_HOME` / `STOCK_HOME_SCREEN`.
   - **HDMI input ids.** Run `dumpsys tv_input` and check the ids parse in `InputPorts.port()`.
   - **Input switching.** Try `am start -a android.intent.action.VIEW -d content://android.media.tv/passthrough/<url-encoded input id>`. Ask first: this switches the TV's picture.
3. **Make the list.** Copy `home.conf.example` to `home.conf`, and fill it from `pm list packages` with the apps the person actually uses, in the order they want. Name devices only if their reported name is wrong, and hide empty HDMI ports.
4. **Build and install:**

   ```bash
   ./build.sh --new-key            # first build only (tests, signing key, APK)
   ./install.sh                    # TV=<ip>:5555 set, or the address in .tv
   ```

5. **Switch on the Home-button takeover**, with permission:

   ```bash
   adb -s $TV shell settings put secure enabled_accessibility_services local.tvhome/local.tvhome.HomeAccessibilityService
   adb -s $TV shell settings put secure accessibility_enabled 1
   ```

   If another launcher's accessibility service is in that setting (Projectivy, for example), remove it so the two don't fight.
6. **Verify on the TV and show the person a screenshot.** Check each of these:
   - Home from inside an app lands on Quiet Home.
   - A quick double press of Home still lands on Quiet Home.
   - Long-pressing Home still opens the TV's own quick panel.
   - Picking a device switches the picture to it.
   - Settings → TV settings opens.
   - Waking the TV from standby goes back to what was on.
   - After a reboot, the TV comes up on Quiet Home. This one needs permission first.
7. **Tell the person how to undo it:** `adb uninstall local.tvhome`.

Later changes to the list or pictures: edit `home.conf` or `backgrounds/`, then `./install.sh conf`. There's no reinstall and nothing to restart.

## Changing the code

- **Layout:**
  - `src/local/tvhome/core/` holds every decision as plain Java with **no `android.*` imports**: config parsing, list building and sorting, HDMI port parsing, crash guard, wake handling, Home-press de-duplication, usage counts, background rotation.
  - `src/local/tvhome/` is a thin Android layer around it.
  - `test/` holds unit tests for `core/` with a tiny runner (`T.eq`, `T.check`, registered in `TestMain`).
- **Test first.** For a behavior change, write the failing test in `test/`, watch `./build.sh test` fail, then make it pass. A bug found on a real TV gets a test that reproduces it; several tests carry a comment saying which real-TV behavior they pin.
- **Build stays tool-only.** `build.sh` uses the Android SDK's `aapt2`, `d8`, `zipalign` and `apksigner` plus the JDK. Don't add Gradle, Kotlin or third-party libraries without a strong reason, and say why in the PR.
- **Things that bit before.** Keep them:
  - **Screens run in their own process** (`android:process=":ui"`). If an activity crash kills the accessibility service, Android waits about 30 minutes to restart it, and the crash guard never trips.
  - **The service's event handler never throws.** It's wrapped in try/catch for the same reason.
  - **Time uses `SystemClock.elapsedRealtime()`**, not the wall clock, which jumps when the TV corrects its time.
  - **One Home press can produce two stock-home events about 50 ms apart.** Repeats are ignored for 300 ms only, because a real second press 0.5 s later must still redirect.
  - **On wake, the TV resumes the last app by itself.** Only a stock home that appears within 3 s of screen-on counts as "woke to home".
  - **Match the stock home by class, not just package**, so the TV's own quick panel and settings stay usable.
  - **A device in standby reports `INPUT_STATE_CONNECTED_STANDBY`.** Treat it as plugged in, because selecting it is how you wake it.
  - **The TV's shell does 32-bit math.** `$(( $(date +%s) * 1000 ))` overflows, so append `000` as text.
- **Before calling a change done:**
  - `./build.sh` passes.
  - The app is installed on a real TV.
  - The affected behavior is checked there with `dumpsys` or a screenshot.

  Say plainly what you verified and what you didn't.

## Optional: blocking TV trackers

This isn't part of Quiet Home, but people often want it too. Many TVs ship with viewing trackers that can be switched off with `pm disable-user --user 0 <package>`; on Sony that's Samba TV (`tv.samba.ssm`). Router-level DNS blocking can catch analytics domains as well.

Go carefully:
- Propose a specific list and get approval.
- After any block, check every streaming app the person uses. Blocking `adobe.com` breaks ESPN's TV-provider login, for example.
- Everything is reversible with `pm enable` or by removing the router rule.
