package local.tvhome.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BackgroundsTest {
    static final long SLOT = Backgrounds.SLOT_MS;

    static void run() {
        T.eq(Backgrounds.SLOT_MS, 1_800_000L, "changesEvery30Minutes");
        List<String> names = List.of("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        T.eq(Backgrounds.pick(List.of(), 0), null, "noPicturesGivesNull");
        T.eq(Backgrounds.pick(names, 5 * SLOT + 10), Backgrounds.pick(names, 5 * SLOT + SLOT - 1), "samePictureWithinSlot");
        T.check(!Backgrounds.pick(names, 5 * SLOT).equals(Backgrounds.pick(names, 6 * SLOT)), "nextSlotChangesPicture");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < names.size(); i++) seen.add(Backgrounds.pick(names, (100 + i) * SLOT));
        T.eq(seen.size(), names.size(), "everyPictureShownOncePerRound");
        T.eq(Backgrounds.pick(List.of("d.jpg", "a.jpg", "c.jpg", "b.jpg"), 7 * SLOT), Backgrounds.pick(names, 7 * SLOT),
                "fileListingOrderDoesNotMatter");
        T.eq(Backgrounds.msUntilNext(5 * SLOT + 1000), SLOT - 1000, "msUntilNext");

        long asked = 5 * SLOT + 29 * 60_000; // asked a minute before the slot ends
        T.eq(Backgrounds.pick(names, asked + 1000, "c.jpg", asked), "c.jpg", "requestedPictureShowsNow");
        T.eq(Backgrounds.pick(names, asked + SLOT - 1, "c.jpg", asked), "c.jpg", "requestedPictureStaysFor30Minutes");
        T.eq(Backgrounds.pick(names, asked + SLOT, "c.jpg", asked), Backgrounds.pick(names, asked + SLOT), "rotationResumesAfter30Minutes");
        T.eq(Backgrounds.pick(names, asked + 1000, "gone.jpg", asked), Backgrounds.pick(names, asked + 1000), "missingRequestIgnored");
        T.eq(Backgrounds.msUntilChange(asked + 1000, "c.jpg", asked), SLOT - 1000, "nextCheckWhenRequestEnds");
        T.eq(Backgrounds.msUntilChange(5 * SLOT + 1000, null, 0), SLOT - 1000, "nextCheckAtSlotWithoutRequest");
    }
}
