package local.tvhome.core;

import java.util.List;
import java.util.Map;

final class UsageLogTest {
    static final long DAY = 86_400_000L;

    static void run() {
        UsageLog log = UsageLog.decode(null);
        log.record("peacock", 1 * DAY);
        log.record("peacock", 2 * DAY);
        log.record("sling", 2 * DAY);
        Map<String, Integer> c = log.counts(3 * DAY);
        T.eq(c.get("peacock"), 2, "countsOpens peacock");
        T.eq(c.get("sling"), 1, "countsOpens sling");

        T.eq(log.counts(31 * DAY + DAY / 2).get("peacock"), 1, "opensOlderThan30DaysDropOut");
        T.eq(log.counts(40 * DAY).get("peacock"), null, "allOldOpensGone");

        UsageLog back = UsageLog.decode(log.encode());
        T.eq(back.counts(3 * DAY), c, "encodeDecodeRoundTrip");
        T.eq(UsageLog.decode("garbage\nx y z\npeacock notanumber").counts(0).size(), 0, "decodeGarbageIsEmpty");

        UsageLog pruned = UsageLog.decode(log.encode());
        pruned.record("x", 40 * DAY);
        T.check(!pruned.encode().contains("peacock"), "recordPrunesOldEntries");
    }
}
