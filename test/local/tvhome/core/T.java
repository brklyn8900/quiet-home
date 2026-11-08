package local.tvhome.core;

import java.util.Objects;

/** Tiny assert helper: records failures instead of stopping at the first one. */
final class T {
    static int checks = 0;
    static int failures = 0;

    static void eq(Object actual, Object expected, String what) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            failures++;
            System.out.println("FAIL " + what + ": expected <" + expected + "> but got <" + actual + ">");
        }
    }

    static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL " + what);
        }
    }
}
