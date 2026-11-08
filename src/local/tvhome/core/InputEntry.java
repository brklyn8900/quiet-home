package local.tvhome.core;

/** A configured HDMI device: {@code key} like "hdmi3", {@code icon} "game" or "box". */
public final class InputEntry {
    public final String key;
    public final String icon;
    public final String name;

    public InputEntry(String key, String icon, String name) {
        this.key = key;
        this.icon = icon;
        this.name = name;
    }
}
