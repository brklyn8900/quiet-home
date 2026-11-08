package local.tvhome.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** When each app or device was opened, kept for 30 days. Stored as lines of "key timestamp". */
public final class UsageLog {
    public static final long WINDOW_MS = 30L * 86_400_000L;

    private final Map<String, List<Long>> opens = new LinkedHashMap<>();

    private UsageLog() {}

    public void record(String key, long now) {
        opens.computeIfAbsent(key, k -> new ArrayList<>()).add(now);
        prune(now);
    }

    /** Opens per key within the last 30 days; keys with none are absent. */
    public Map<String, Integer> counts(long now) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> e : opens.entrySet()) {
            int n = 0;
            for (long t : e.getValue()) if (t > now - WINDOW_MS && t <= now) n++;
            if (n > 0) out.put(e.getKey(), n);
        }
        return out;
    }

    private void prune(long now) {
        for (Iterator<Map.Entry<String, List<Long>>> it = opens.entrySet().iterator(); it.hasNext(); ) {
            List<Long> times = it.next().getValue();
            times.removeIf(t -> t <= now - WINDOW_MS);
            if (times.isEmpty()) it.remove();
        }
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Long>> e : opens.entrySet()) {
            for (long t : e.getValue()) sb.append(e.getKey()).append(' ').append(t).append('\n');
        }
        return sb.toString();
    }

    /** Null or unreadable lines are skipped. */
    public static UsageLog decode(String text) {
        UsageLog log = new UsageLog();
        if (text == null) return log;
        for (String line : text.split("\n")) {
            String[] p = line.trim().split(" ");
            if (p.length != 2) continue;
            try {
                log.opens.computeIfAbsent(p[0], k -> new ArrayList<>()).add(Long.parseLong(p[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        return log;
    }
}
