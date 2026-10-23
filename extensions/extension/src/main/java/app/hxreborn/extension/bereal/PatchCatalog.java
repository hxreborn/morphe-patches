/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.util.ArrayList;
import java.util.List;

final class PatchCatalog {

    final List<PatchGroup> groups;

    private PatchCatalog(List<PatchGroup> groups) {
        this.groups = groups;
    }

    static PatchCatalog collect() {
        final List<PatchGroup> groups = new ArrayList<>();
        addIfApplied(groups, posting());
        addIfApplied(groups, feed());
        return new PatchCatalog(groups);
    }

    int applied() {
        int applied = 0;
        for (PatchGroup group : this.groups) {
            applied += group.rows.size();
        }
        return applied;
    }

    int on() {
        int on = 0;
        for (PatchGroup group : this.groups) {
            on += group.onCount();
        }
        return on;
    }

    int off() {
        return applied() - on();
    }

    private static void addIfApplied(List<PatchGroup> groups, PatchGroup group) {
        if (!group.isEmpty()) {
            groups.add(group);
        }
    }

    private static PatchGroup posting() {
        final PatchGroup group = new PatchGroup("Your posts");
        if (GalleryPhotos.isPatchIncluded()) {
            group.addToggle("ic_photo", "Post photos from your gallery",
                    "Tap the gallery button at the top of the camera to pick a back photo, a front photo or both, "
                            + "then tap the shutter. Sides you don't pick use the camera.",
                    PatchSettings.GALLERY_PHOTOS);
        }
        if (CustomLocation.isPatchIncluded()) {
            group.addLocation("ic_location_fill", "Use a custom location",
                    "Your BeReal shows the place you pick on the map below or find by name.",
                    PatchSettings.CUSTOM_LOCATION);
        }
        if (RetakeCount.isPatchIncluded()) {
            group.addToggle("ic_retake", "Choose your retake count",
                    "Each time you send a BeReal, you pick the retake count your friends see. Tap Use real count to "
                            + "send your true number of retakes.",
                    PatchSettings.CUSTOM_RETAKE_COUNT);
        }
        if (LateFlag.isPatchIncluded()) {
            group.addToggle("ic_clock", "Send late BeReals as on time",
                    "Your friends see no late tag on BeReals you take after the moment's window.",
                    PatchSettings.REMOVE_LATE_FLAG);
        }
        return group;
    }

    private static PatchGroup feed() {
        final PatchGroup group = new PatchGroup("Feed");
        if (SponsoredPosts.isPatchIncluded()) {
            group.addAlwaysOn("ic_grid_view", "Remove ads",
                    "Removes ads and sponsored BeReals from My Friends and Friends of Friends and the full-screen ad "
                            + "at launch, and stops the ad networks from loading.");
        }
        return group;
    }

}
