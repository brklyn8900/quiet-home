package local.tvhome;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import local.tvhome.core.DetectedInput;
import local.tvhome.core.InputPorts;

/** Reads the TV's HDMI ports. One entry per port; the device's own (HDMI-CEC) input wins over the bare port. */
final class Inputs {
    private Inputs() {}

    static List<DetectedInput> detect(Context ctx) {
        try {
            TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
            Map<Integer, TvInputInfo> hw = new LinkedHashMap<>();
            Map<Integer, TvInputInfo> cec = new LinkedHashMap<>();
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.getType() != TvInputInfo.TYPE_HDMI) continue;
                int port = InputPorts.port(info.getId());
                if (port <= 0) continue;
                (info.getParentId() != null ? cec : hw).put(port, info);
            }
            List<DetectedInput> out = new ArrayList<>();
            for (int port = 1; port <= 9; port++) {
                TvInputInfo dev = cec.get(port);
                TvInputInfo bare = hw.get(port);
                if (dev == null && bare == null) continue;
                boolean connected = plugged(tim, dev) || plugged(tim, bare);
                String label = dev != null ? String.valueOf(dev.loadLabel(ctx)) : "";
                String id = (dev != null ? dev : bare).getId();
                out.add(new DetectedInput(InputPorts.key(port), id, label, connected));
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Standby counts as plugged in: a sleeping PlayStation is still there. */
    private static boolean plugged(TvInputManager tim, TvInputInfo info) {
        return info != null && tim.getInputState(info.getId()) != TvInputManager.INPUT_STATE_DISCONNECTED;
    }
}
