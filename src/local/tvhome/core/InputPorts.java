package local.tvhome.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Works out the HDMI port number from a Sony TV input id. */
public final class InputPorts {
    // CEC device input, e.g. ".../HDMI300004": first digit of the physical address 0x3000 is the port.
    private static final Pattern CEC = Pattern.compile("ExternalTvInputService/HDMI(\\d)\\d{5}$");
    // Hardware input, e.g. ".../HW3".
    private static final Pattern HW = Pattern.compile("ExternalTvInputService/HW(\\d+)$");

    private InputPorts() {}

    public static int port(String inputId) {
        if (inputId == null) return 0;
        Matcher m = CEC.matcher(inputId);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = HW.matcher(inputId);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    public static String key(int port) {
        return "hdmi" + port;
    }
}
