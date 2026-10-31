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

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.StringSetting;

public final class PatchSettings {

    public static final BooleanSetting GALLERY_PHOTOS = new BooleanSetting("bereal_gallery_photos", false);

    public static final BooleanSetting CUSTOM_LOCATION = new BooleanSetting("bereal_custom_location", false);

    public static final StringSetting LOCATION_LATITUDE = new StringSetting("bereal_location_latitude", "");

    public static final StringSetting LOCATION_LONGITUDE = new StringSetting("bereal_location_longitude", "");

    public static final StringSetting LOCATION_LABEL = new StringSetting("bereal_location_label", "");

    public static final BooleanSetting CUSTOM_RETAKE_COUNT = new BooleanSetting("bereal_custom_retake_count", false);

    public static final BooleanSetting REMOVE_LATE_FLAG = new BooleanSetting("bereal_remove_late_flag", false);

    public static final BooleanSetting REMOVE_FRIEND_SUGGESTIONS = new BooleanSetting("bereal_hide_friend_suggestions",
            true);

    public static final BooleanSetting REMOVE_FRIENDS_OF_FRIENDS_PROMPTS = new BooleanSetting(
            "bereal_remove_friends_of_friends_prompts", true);

    public static final BooleanSetting REMOVE_FRIENDS_OF_FRIENDS_TAB = new BooleanSetting(
            "bereal_remove_friends_of_friends_tab", false);

    public static final BooleanSetting DISABLE_VIEW_RECEIPTS = new BooleanSetting("bereal_disable_view_receipts",
            false);

    public static final BooleanSetting DISABLE_TYPING_INDICATOR = new BooleanSetting("bereal_disable_typing_indicator",
            false);

    public static final BooleanSetting REMOVE_LATE_LABEL = new BooleanSetting("bereal_remove_late_label", true);

    public static final BooleanSetting REMOVE_STREAK_PROMPTS = new BooleanSetting("bereal_remove_streak_prompts", true);

    public static final BooleanSetting SHOW_FRIENDS_POSTS = new BooleanSetting("bereal_show_friends_posts", false);

    public static final BooleanSetting REMOVE_PROMOTIONS = new BooleanSetting("bereal_remove_promotions", true);

    public static final BooleanSetting SAVE_FRIENDS_BEREALS = new BooleanSetting("bereal_save_friends_bereals", true);

    public static final BooleanSetting REMOVE_WATERMARK = new BooleanSetting("bereal_remove_watermark", true);

    public static final StringSetting DOWNLOAD_FOLDER = new StringSetting("bereal_download_folder", "");

    public static final StringSetting BACKUP_SAVED_IDS = new StringSetting("bereal_backup_saved_ids", "");

    public static final StringSetting BACKUP_LAST_RUN = new StringSetting("bereal_backup_last_run", "");

    public static final StringSetting BACKUP_LAST_COUNT = new StringSetting("bereal_backup_last_count", "");

    private PatchSettings() {
    }

}
