package local.tvhome.core;

/** One app in the Watch column. {@code name} is null unless the config overrides the app's own label. */
public final class AppEntry {
    public final String pkg;
    public final String name;

    public AppEntry(String pkg, String name) {
        this.pkg = pkg;
        this.name = name;
    }
}
