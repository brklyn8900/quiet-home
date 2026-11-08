package local.tvhome.core;

import java.util.ArrayList;
import java.util.List;

/** Three crashes within five minutes means the home screen steps aside for the stock Google TV home. */
public final class CrashGuard {
    public static final long WINDOW_MS = 300_000;
    public static final int LIMIT = 3;

    private CrashGuard() {}

    public static List<Long> record(List<Long> times, long now) {
        List<Long> out = new ArrayList<>();
        for (Long t : times) {
            if (t >= now - WINDOW_MS) out.add(t);
        }
        out.add(now);
        return out;
    }

    public static boolean shouldDisable(List<Long> times, long now) {
        int n = 0;
        for (Long t : times) {
            if (t >= now - WINDOW_MS && t <= now) n++;
        }
        return n >= LIMIT;
    }

    public static String encode(List<Long> times) {
        StringBuilder sb = new StringBuilder();
        for (Long t : times) {
            if (sb.length() > 0) sb.append(',');
            sb.append(t);
        }
        return sb.toString();
    }

    /** Null or anything unparseable gives an empty list. */
    public static List<Long> decode(String s) {
        List<Long> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        try {
            for (String part : s.split(",")) out.add(Long.parseLong(part.trim()));
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
        return out;
    }
}
