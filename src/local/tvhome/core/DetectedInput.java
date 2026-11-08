package local.tvhome.core;

/** An HDMI port as the TV reports it right now. {@code label} is the device's own name, or "" if none. */
public final class DetectedInput {
    public final String key;
    public final String inputId;
    public final String label;
    public final boolean connected;

    public DetectedInput(String key, String inputId, String label, boolean connected) {
        this.key = key;
        this.inputId = inputId;
        this.label = label;
        this.connected = connected;
    }
}
