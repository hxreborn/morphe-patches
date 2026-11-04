/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.bereal;

import java.util.ArrayList;
import java.util.List;

import app.hxreborn.extension.bereal.backup.MemoriesBackup;
import app.hxreborn.extension.bereal.blur.FriendsPosts;
import app.hxreborn.extension.bereal.download.DownloadFolder;
import app.hxreborn.extension.bereal.download.DownloadWatermark;
import app.hxreborn.extension.bereal.download.FriendsPostDownload;
import app.hxreborn.extension.bereal.fof.FriendsOfFriendsPrompts;
import app.hxreborn.extension.bereal.fof.FriendsOfFriendsTab;
import app.hxreborn.extension.bereal.latelabel.LateLabel;
import app.hxreborn.extension.bereal.promos.Promotions;
import app.hxreborn.extension.bereal.seen.ViewReceipts;
import app.hxreborn.extension.bereal.streak.StreakPrompts;
import app.hxreborn.extension.bereal.typing.TypingIndicator;

final class PatchCatalog {

    final List<PatchGroup> groups;

    private PatchCatalog(List<PatchGroup> groups) {
        this.groups = groups;
    }

    static PatchCatalog collect() {
        final List<PatchGroup> groups = new ArrayList<>();
        addIfApplied(groups, posting());
        addIfApplied(groups, friends());
        addIfApplied(groups, feed());
        addIfApplied(groups, privacy());
        addIfApplied(groups, updates());
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
            group.addToggle("ic_photo", "Post from gallery",
                    "Adds a gallery button to the camera for picking the back and front photo of your BeReal.",
                    PatchSettings.GALLERY_PHOTOS);
        }
        if (CustomLocation.isPatchIncluded()) {
            group.addLocation("ic_location_fill", "Custom location",
                    "Sets the location shown on your BeReal to a place you choose on a map or by name.",
                    PatchSettings.CUSTOM_LOCATION);
        }
        if (RetakeCount.isPatchIncluded()) {
            group.addToggle("ic_retake", "Custom retake count",
                    "Asks before each BeReal how many retakes your friends see on it.",
                    PatchSettings.CUSTOM_RETAKE_COUNT);
        }
        if (LateFlag.isPatchIncluded()) {
            group.addToggle("ic_clock", "Remove late flag",
                    "Sends BeReals you take after the moment's window as on time.", PatchSettings.REMOVE_LATE_FLAG);
        }
        return group;
    }

    private static PatchGroup friends() {
        final PatchGroup group = new PatchGroup("Friends' posts");
        if (FriendsPosts.isPatchIncluded()) {
            group.addToggle("ic_eyes", "Show friends' posts before posting",
                    "Shows friends' BeReals in My Friends and Friends of Friends before you post your own.",
                    PatchSettings.SHOW_FRIENDS_POSTS, true);
        }
        if (FriendsPostDownload.isPatchIncluded()) {
            group.addToggle("ic_square_and_arrow_down", "Save friends' BeReals",
                    "Adds Download to the 3-dot menu of friends' BeReals in the feed and on the RealMoji screen.",
                    PatchSettings.SAVE_FRIENDS_BEREALS);
        }
        if (DownloadWatermark.isPatchIncluded()) {
            group.addToggle("ic_photo_on_rectangle_angled", "Remove watermark",
                    "Saves downloaded BeReals at full size without the BeReal logo.", PatchSettings.REMOVE_WATERMARK);
        }
        if (DownloadFolder.isPatchIncluded()) {
            group.addFolder("ic_folder", "Download folder", "Saves downloaded BeReals to this folder.");
        }
        if (MemoriesBackup.isPatchIncluded()) {
            group.addAction("ic_square_and_arrow_down", "Back up memories", MemoriesBackup.summary(),
                    MemoriesBackup::start);
        }
        return group;
    }

    private static PatchGroup feed() {
        final PatchGroup group = new PatchGroup("Feed");
        if (SponsoredPosts.isPatchIncluded()) {
            group.addAlwaysOn("ic_grid_view", "Remove ads",
                    "Removes the full-screen ad at launch and sponsored BeReals in both feeds.");
        }
        if (FriendSuggestions.isPatchIncluded()) {
            group.addToggle("ic_person_2_foreground_fill", "Remove friend suggestions",
                    "Removes suggested friends, Contacts on BeReal and popular accounts from the Friends screen and "
                            + "the feed.",
                    PatchSettings.REMOVE_FRIEND_SUGGESTIONS);
        }
        if (StreakPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_bell_slash_fill", "Remove streak prompts",
                    "Removes the offers to recover your streak and the Save your streak sheet.",
                    PatchSettings.REMOVE_STREAK_PROMPTS);
        }
        if (Promotions.isPatchIncluded()) {
            group.addToggle("ic_rating_star_fill", "Remove promotions",
                    "Removes Recap cards, the \"How's BeReal?\" review prompt, the profile completion card, the "
                            + "notifications banner and the music tooltip.",
                    PatchSettings.REMOVE_PROMOTIONS);
        }
        if (FriendsOfFriendsPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_person_2_badge_plus_fill", "Remove Friends of Friends prompts",
                    "Removes the \"Share with Friends of Friends\" card and the end-of-feed messages.",
                    PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_PROMPTS);
        }
        if (LateLabel.isPatchIncluded()) {
            group.addToggle("ic_tag_fill", "Remove late label",
                    "Replaces the late label on friends' BeReals with how long ago they posted.",
                    PatchSettings.REMOVE_LATE_LABEL);
        }
        if (FriendsOfFriendsTab.isPatchIncluded()) {
            group.addToggle("ic_person_fill_and_arrow_left_and_arrow_right_outward", "Remove Friends of Friends tab",
                    "Removes the Friends of Friends tab from the home screen.",
                    PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB, true);
        }
        return group;
    }

    private static PatchGroup privacy() {
        final PatchGroup group = new PatchGroup("Privacy");
        if (ViewReceipts.isPatchIncluded()) {
            group.addToggle("ic_eye_slash_fill", "Disable view receipts",
                    "Stops sending which BeReals you viewed and which chats you opened.",
                    PatchSettings.DISABLE_VIEW_RECEIPTS);
        }
        if (TypingIndicator.isPatchIncluded()) {
            group.addToggle("ic_bubble_fill", "Disable typing indicator",
                    "Stops showing friends that you're typing in a chat.", PatchSettings.DISABLE_TYPING_INDICATOR);
        }
        if (DataCollection.isPatchIncluded()) {
            group.addAlwaysOn("ic_lock_shield", "Disable data collection",
                    "Turns off ad tracking, Firebase crash reports and sessions, Amplitude and Datadog analytics, "
                            + "Adjust attribution and the Shake bug reporter.");
        }
        if (ContactsPermission.isPatchIncluded()) {
            group.addAlwaysOn("ic_person_crop_rectangle", "Remove contacts permission",
                    "Stops BeReal from asking for or reading your phone's contacts.");
        }
        return group;
    }

    private static PatchGroup updates() {
        final PatchGroup group = new PatchGroup("Updates");
        if (UpdatePrompts.isPatchIncluded()) {
            group.addAlwaysOn("ic_arrow_up_circle_fill", "Remove update prompts",
                    "Removes BeReal's required and suggested update dialogs and the Play Store update prompt.");
        }
        return group;
    }

}
