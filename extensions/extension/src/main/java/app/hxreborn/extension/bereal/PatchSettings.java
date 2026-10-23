/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

    public static final BooleanSetting REMOVE_LATE_LABEL = new BooleanSetting("bereal_remove_late_label", true);

    public static final BooleanSetting REMOVE_STREAK_PROMPTS = new BooleanSetting("bereal_remove_streak_prompts", true);

    public static final BooleanSetting REMOVE_PROMOTIONS = new BooleanSetting("bereal_remove_promotions", true);

    private PatchSettings() {
    }

}
