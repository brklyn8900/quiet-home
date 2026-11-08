package local.tvhome.core;

final class InputPortsTest {
    static void run() {
        T.eq(InputPorts.port("com.sony.dtv.tvinput.external/.ExternalTvInputService/HDMI300004"), 3, "port CEC hdmi3");
        T.eq(InputPorts.port("com.sony.dtv.tvinput.external/.ExternalTvInputService/HDMI400008"), 4, "port CEC hdmi4");
        T.eq(InputPorts.port("com.sony.dtv.tvinput.external/.ExternalTvInputService/HW2"), 2, "port HW2");
        T.eq(InputPorts.port("com.sony.dtv.tvinput.tuner/.AtscTunerInputService/HW0"), 0, "port tuner");
        T.eq(InputPorts.port("com.sling/x.LiveTvInputService"), 0, "port app input");
        T.eq(InputPorts.port(null), 0, "port null");
        T.eq(InputPorts.key(3), "hdmi3", "key 3");
    }
}
