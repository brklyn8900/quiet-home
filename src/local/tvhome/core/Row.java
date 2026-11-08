package local.tvhome.core;

/**
 * One line on screen. APP: id = package, icon = null (use the app's own icon).
 * INPUT: id = TV input id, icon "game"/"box". ACTION: id "settings", "tv_settings" or "all_apps".
 */
public final class Row {
    public enum Kind { APP, INPUT, ACTION }

    public final Kind kind;
    public final String id;
    public final String label;
    public final String icon;

    public Row(Kind kind, String id, String label, String icon) {
        this.kind = kind;
        this.id = id;
        this.label = label;
        this.icon = icon;
    }
}
