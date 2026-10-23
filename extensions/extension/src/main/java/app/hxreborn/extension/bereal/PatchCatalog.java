/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.util.ArrayList;
import java.util.List;

import app.hxreborn.extension.bereal.fof.FriendsOfFriendsPrompts;
import app.hxreborn.extension.bereal.latelabel.LateLabel;
import app.hxreborn.extension.bereal.promos.Promotions;
import app.hxreborn.extension.bereal.streak.StreakPrompts;

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
                    "Tap the gallery button at the top of the camera to pick a back and a front photo, "
                            + "or shoot as usual.",
                    PatchSettings.GALLERY_PHOTOS);
        }
        if (CustomLocation.isPatchIncluded()) {
            group.addLocation("ic_location_fill", "Use a custom location",
                    "Your BeReal shows the place you choose below, on the map or by searching a place name.",
                    PatchSettings.CUSTOM_LOCATION);
        }
        if (RetakeCount.isPatchIncluded()) {
            group.addToggle("ic_retake", "Choose your retake count",
                    "When you send a BeReal, a prompt asks how many retakes your friends see. Pick Use real count to "
                            + "send the number you took.",
                    PatchSettings.CUSTOM_RETAKE_COUNT);
        }
        if (LateFlag.isPatchIncluded()) {
            group.addToggle("ic_clock", "Send late BeReals as on time",
                    "BeReals taken after the moment's window reach your friends without the late tag.",
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
        if (FriendSuggestions.isPatchIncluded()) {
            group.addToggle("ic_person_2_foreground_fill", "Remove friend suggestions",
                    "Removes suggested friends, Contacts on BeReal and popular accounts from the Friends screen and "
                            + "the feed.",
                    PatchSettings.REMOVE_FRIEND_SUGGESTIONS);
        }
        if (StreakPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_bell_slash_fill", "Remove streak offers",
                    "Removes the offers to recover your streak and the Save your streak sheet. Your streak count "
                            + "doesn't change.",
                    PatchSettings.REMOVE_STREAK_PROMPTS);
        }
        if (Promotions.isPatchIncluded()) {
            group.addToggle("ic_rating_star_fill", "Remove BeReal promos",
                    "Removes Recap cards, the How's BeReal? review prompt, the profile completion card, the "
                            + "notifications banner and the add music tooltip.",
                    PatchSettings.REMOVE_PROMOTIONS);
        }
        if (FriendsOfFriendsPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_person_2_badge_plus_fill", "Remove Friends of Friends prompts",
                    "Removes the Share with Friends of Friends card and the messages at the end of the feed.",
                    PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_PROMPTS);
        }
        if (LateLabel.isPatchIncluded()) {
            group.addToggle("ic_tag_fill", "Remove late labels",
                    "Friends' BeReals show when they were posted, without late or how late they were.",
                    PatchSettings.REMOVE_LATE_LABEL);
        }
        return group;
    }

}
