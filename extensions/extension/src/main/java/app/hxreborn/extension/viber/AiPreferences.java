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
package app.hxreborn.extension.viber;

@SuppressWarnings("unused")
public final class AiPreferences {

    private AiPreferences() {
    }

    public static boolean showImageRemix(boolean visible) {
        return visible && !PatchSettings.disableAiFeatures();
    }

    public static Object overrideValue(String key, Object value) {
        if (!(value instanceof Boolean)) {
            return value;
        }
        return overrideBoolean(key, (Boolean) value);
    }

    public static boolean overrideBoolean(String key, boolean value) {
        if (!PatchSettings.disableAiFeatures()) {
            return value;
        }
        switch (key) {
            case "display_chat_summary_feature_key":
            case "display_image_remix_feature_key":
            case "display_ai_in_chat_feature_key":
            case "ai_reply_is_display_feature_setting_enabled":
            case "smart_recap_is_enabled":
                return false;
            case "general_ai_features_banner_dismissed_key":
                return true;
            default:
                return value;
        }
    }

}
