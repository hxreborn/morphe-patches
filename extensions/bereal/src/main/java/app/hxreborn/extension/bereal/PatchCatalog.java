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
            group.addToggle("ic_photo", GalleryPhotos.patchName(), GalleryPhotos.patchDescription(),
                    PatchSettings.GALLERY_PHOTOS);
        }
        if (CustomLocation.isPatchIncluded()) {
            group.addLocation("ic_location_fill", CustomLocation.patchName(), CustomLocation.patchDescription(),
                    PatchSettings.CUSTOM_LOCATION);
        }
        if (RetakeCount.isPatchIncluded()) {
            group.addToggle("ic_retake", RetakeCount.patchName(), RetakeCount.patchDescription(),
                    PatchSettings.CUSTOM_RETAKE_COUNT);
        }
        if (LateFlag.isPatchIncluded()) {
            group.addToggle("ic_clock", LateFlag.patchName(), LateFlag.patchDescription(),
                    PatchSettings.REMOVE_LATE_FLAG);
        }
        return group;
    }

    private static PatchGroup friends() {
        final PatchGroup group = new PatchGroup("Friends' posts");
        if (FriendsPosts.isPatchIncluded()) {
            group.addToggle("ic_eyes", FriendsPosts.patchName(), FriendsPosts.patchDescription(),
                    PatchSettings.SHOW_FRIENDS_POSTS, true);
        }
        if (FriendsPostDownload.isPatchIncluded()) {
            group.addToggle("ic_square_and_arrow_down", FriendsPostDownload.patchName(),
                    FriendsPostDownload.patchDescription(), PatchSettings.SAVE_FRIENDS_BEREALS);
        }
        if (DownloadWatermark.isPatchIncluded()) {
            group.addToggle("ic_photo_on_rectangle_angled", DownloadWatermark.patchName(),
                    DownloadWatermark.patchDescription(), PatchSettings.REMOVE_WATERMARK);
        }
        if (DownloadFolder.isPatchIncluded()) {
            group.addFolder("ic_folder", "Download folder", "Saves downloaded BeReals to this folder.");
        }
        if (MemoriesBackup.isPatchIncluded()) {
            group.addAction("ic_square_and_arrow_down", MemoriesBackup.patchName(), MemoriesBackup.summary(),
                    MemoriesBackup::start);
        }
        return group;
    }

    private static PatchGroup feed() {
        final PatchGroup group = new PatchGroup("Feed");
        if (SponsoredPosts.isPatchIncluded()) {
            group.addAlwaysOn("ic_grid_view", SponsoredPosts.patchName(), SponsoredPosts.patchDescription());
        }
        if (FriendSuggestions.isPatchIncluded()) {
            group.addToggle("ic_person_2_foreground_fill", FriendSuggestions.patchName(),
                    FriendSuggestions.patchDescription(), PatchSettings.REMOVE_FRIEND_SUGGESTIONS);
        }
        if (StreakPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_bell_slash_fill", StreakPrompts.patchName(),
                    StreakPrompts.patchDescription(), PatchSettings.REMOVE_STREAK_PROMPTS);
        }
        if (Promotions.isPatchIncluded()) {
            group.addToggle("ic_rating_star_fill", Promotions.patchName(), Promotions.patchDescription(),
                    PatchSettings.REMOVE_PROMOTIONS);
        }
        if (FriendsOfFriendsPrompts.isPatchIncluded()) {
            group.addExperimentalToggle("ic_person_2_badge_plus_fill", FriendsOfFriendsPrompts.patchName(),
                    FriendsOfFriendsPrompts.patchDescription(), PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_PROMPTS);
        }
        if (LateLabel.isPatchIncluded()) {
            group.addToggle("ic_tag_fill", LateLabel.patchName(), LateLabel.patchDescription(),
                    PatchSettings.REMOVE_LATE_LABEL);
        }
        if (FriendsOfFriendsTab.isPatchIncluded()) {
            group.addToggle("ic_person_fill_and_arrow_left_and_arrow_right_outward", FriendsOfFriendsTab.patchName(),
                    FriendsOfFriendsTab.patchDescription(), PatchSettings.REMOVE_FRIENDS_OF_FRIENDS_TAB, true);
        }
        return group;
    }

    private static PatchGroup privacy() {
        final PatchGroup group = new PatchGroup("Privacy");
        if (ViewReceipts.isPatchIncluded()) {
            group.addToggle("ic_eye_slash_fill", ViewReceipts.patchName(), ViewReceipts.patchDescription(),
                    PatchSettings.DISABLE_VIEW_RECEIPTS);
        }
        if (TypingIndicator.isPatchIncluded()) {
            group.addToggle("ic_bubble_fill", TypingIndicator.patchName(), TypingIndicator.patchDescription(),
                    PatchSettings.DISABLE_TYPING_INDICATOR);
        }
        if (DataCollection.isPatchIncluded()) {
            group.addAlwaysOn("ic_lock_shield", DataCollection.patchName(), DataCollection.patchDescription());
        }
        if (ContactsPermission.isPatchIncluded()) {
            group.addAlwaysOn("ic_person_crop_rectangle", ContactsPermission.patchName(),
                    ContactsPermission.patchDescription());
        }
        return group;
    }

    private static PatchGroup updates() {
        final PatchGroup group = new PatchGroup("Updates");
        if (UpdatePrompts.isPatchIncluded()) {
            group.addAlwaysOn("ic_arrow_up_circle_fill", UpdatePrompts.patchName(), UpdatePrompts.patchDescription());
        }
        return group;
    }

}
