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

    private static PatchGroup friends() {
        final PatchGroup group = new PatchGroup("Friends' posts");
        if (FriendsPosts.isPatchIncluded()) {
            group.addToggle("ic_eyes", "See friends' BeReals before posting",
                    "You see friends' BeReals in My Friends and Friends of Friends before you post your own. Restart "
                            + "BeReal to apply this.",
                    PatchSettings.SHOW_FRIENDS_POSTS, true);
        }
        if (FriendsPostDownload.isPatchIncluded()) {
            group.addToggle("ic_square_and_arrow_down", "Save friends' BeReals",
                    "Download appears in the 3-dot menu of friends' BeReals in the feed and on the RealMoji screen.",
                    PatchSettings.SAVE_FRIENDS_BEREALS);
        }
        if (DownloadWatermark.isPatchIncluded()) {
            group.addToggle("ic_photo_on_rectangle_angled", "Remove BeReal logo from downloads",
                    "BeReals you download save at full size without the BeReal logo.", PatchSettings.REMOVE_WATERMARK);
        }
        if (DownloadFolder.isPatchIncluded()) {
            group.addFolder("ic_folder", "Download folder", "BeReals you download save to this folder.");
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
                    "Removes the late label from friends' BeReals. Each one shows how long ago your friend posted it.",
                    PatchSettings.REMOVE_LATE_LABEL);
        }
        if (FriendsOfFriendsTab.isPatchIncluded()) {
            group.addToggle("ic_person_fill_and_arrow_left_and_arrow_right_outward",
                    "Remove the Friends of Friends tab",
                    "The home screen shows My Friends only. Restart BeReal to apply this.",
                    PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB, true);
        }
        return group;
    }

    private static PatchGroup privacy() {
        final PatchGroup group = new PatchGroup("Privacy");
        if (ViewReceipts.isPatchIncluded()) {
            group.addToggle("ic_eye_slash_fill", "Turn off seen status",
                    "BeReal stops sending which BeReals you viewed and which chats you opened.",
                    PatchSettings.DISABLE_VIEW_RECEIPTS);
        }
        if (TypingIndicator.isPatchIncluded()) {
            group.addToggle("ic_bubble_fill", "Turn off typing indicator",
                    "Friends don't see when you're typing in a chat. You still see when they type.",
                    PatchSettings.DISABLE_TYPING_INDICATOR);
        }
        if (DataCollection.isPatchIncluded()) {
            group.addAlwaysOn("ic_lock_shield", "Block tracking",
                    "Turns off the advertising ID, Firebase crash reports and sessions, Amplitude and Datadog "
                            + "analytics, Adjust attribution and the Shake bug reporter.");
        }
        if (ContactsPermission.isPatchIncluded()) {
            group.addAlwaysOn("ic_person_crop_rectangle", "Remove contacts permission",
                    "BeReal can't ask for or read your phone's contacts.");
        }
        return group;
    }

    private static PatchGroup updates() {
        final PatchGroup group = new PatchGroup("Updates");
        if (UpdatePrompts.isPatchIncluded()) {
            group.addAlwaysOn("ic_arrow_up_circle_fill", "Remove update popups",
                    "Removes BeReal's required and suggested update popups and the Play Store update prompt.");
        }
        return group;
    }

}
