package local.tvhome.core;

import java.util.ArrayList;
import java.util.List;

final class CrashGuardTest {
    static void run() {
        List<Long> t = new ArrayList<>();
        t = CrashGuard.record(t, 0);
        t = CrashGuard.record(t, 60_000);
        T.check(!CrashGuard.shouldDisable(t, 60_000), "twoCrashesDoNotDisable");
        t = CrashGuard.record(t, 120_000);
        T.check(CrashGuard.shouldDisable(t, 120_000), "threeCrashesInFiveMinutesDisables");

        List<Long> old = new ArrayList<>();
        old = CrashGuard.record(old, 0);
        old = CrashGuard.record(old, 10_000);
        old = CrashGuard.record(old, 400_000);
        T.eq(old.size(), 1, "oldCrashesExpire size");
        T.check(!CrashGuard.shouldDisable(old, 400_000), "oldCrashesExpire not disabled");

        T.eq(CrashGuard.decode(null), List.of(), "decode null");
        T.eq(CrashGuard.decode("x,,y"), List.of(), "decode garbage");
        T.eq(CrashGuard.decode(CrashGuard.encode(List.of(1L, 2L))), List.of(1L, 2L), "encode/decode round trip");
    }
}
