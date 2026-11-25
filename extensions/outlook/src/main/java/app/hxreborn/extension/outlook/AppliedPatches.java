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
package app.hxreborn.extension.outlook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class AppliedPatches {

    public static final String KEEP_MESSAGES_UNREAD = "keepMessagesUnread";

    public static final String REMOVE_INBOX_ADS = "removeInboxAds";

    public static final String OPEN_EMAIL_TEMPLATES = "openEmailTemplates";

    public static final String SELECT_MULTIPLE_SEARCH_RESULTS = "selectMultipleSearchResults";

    public static final String HIDE_COPILOT = "hideCopilot";

    public static final String ADD_UNREAD_SHORTCUT = "addUnreadShortcut";

    public static final String HIDE_NAVIGATION_BUTTONS = "hideNavigationButtons";

    public static final String ADD_CUSTOM_THEMES = "addCustomThemes";

    private static final List<String> KEYS = Collections
        .unmodifiableList(Arrays.asList("fixSignIn", REMOVE_INBOX_ADS, KEEP_MESSAGES_UNREAD, ADD_UNREAD_SHORTCUT,
                OPEN_EMAIL_TEMPLATES, HIDE_COPILOT, SELECT_MULTIPLE_SEARCH_RESULTS, HIDE_NAVIGATION_BUTTONS,
                ADD_CUSTOM_THEMES, "gmsCoreSupport"));

    private AppliedPatches() {
    }

    private static String metadata(String key, boolean description) {
        return null;
    }

    public static List<String> keys() {
        return KEYS;
    }

    public static boolean isApplied(String key) {
        return name(key) != null;
    }

    public static String name(String key) {
        return metadata(key, false);
    }

    public static String description(String key) {
        return metadata(key, true);
    }

}
