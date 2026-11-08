package local.tvhome.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Which background picture to show: a new one every 30 minutes, going through all of them in a shuffled order. */
public final class Backgrounds {
    public static final long SLOT_MS = 30L * 60_000L;

    private Backgrounds() {}

    /** The picture for the 30-minute slot containing {@code now}, or null when there are none. */
    public static String pick(List<String> names, long now) {
        if (names.isEmpty()) return null;
        List<String> order = new ArrayList<>(names);
        Collections.sort(order);
        Collections.shuffle(order, new Random(order.hashCode())); // same order every round for the same set of pictures
        return order.get((int) Math.floorMod(now / SLOT_MS, (long) order.size()));
    }

    /** Like {@link #pick(List, long)}, but a picture someone asked for at {@code askedAt} stays up for 30 minutes. */
    public static String pick(List<String> names, long now, String requested, long askedAt) {
        if (requestActive(names, now, requested, askedAt)) return requested;
        return pick(names, now);
    }

    /** When the background should next be re-checked. */
    public static long msUntilChange(long now, String requested, long askedAt) {
        long untilSlot = msUntilNext(now);
        if (requested == null || now < askedAt || now - askedAt >= SLOT_MS) return untilSlot;
        return SLOT_MS - (now - askedAt);
    }

    private static boolean requestActive(List<String> names, long now, String requested, long askedAt) {
        return requested != null && names.contains(requested) && now >= askedAt && now - askedAt < SLOT_MS;
    }

    public static long msUntilNext(long now) {
        return SLOT_MS - Math.floorMod(now, SLOT_MS);
    }
}
