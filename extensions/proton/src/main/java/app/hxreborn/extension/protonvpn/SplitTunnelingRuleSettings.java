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
package app.hxreborn.extension.protonvpn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.text.TextUtils;
import android.util.Log;

import app.hxreborn.extension.proton.PatchSettings;

final class SplitTunnelingRuleSettings {

    static final int UNKNOWN_MATCH_COUNT = -1;

    private static final String VALUE_SEPARATOR = ",";

    private static final String CATEGORIES = "split_tunneling_rule_categories";

    private static final String KEYWORDS = "split_tunneling_rule_keywords";

    private static final String MATCH_COUNT = "split_tunneling_rule_match_count";

    private static final String MATCHED_APPS = "split_tunneling_rule_matched_apps";

    private static final String MATCHED_RULES = "split_tunneling_rule_matched_rules";

    private static final String CONFIGURATION_SEPARATOR = "\n";

    private SplitTunnelingRuleSettings() {

    }

    static Set<SplitTunnelingCategory> categories() {
        final Set<SplitTunnelingCategory> categories = EnumSet.noneOf(SplitTunnelingCategory.class);
        final List<String> ids = split(PatchSettings.getString(CATEGORIES, ""));
        for (SplitTunnelingCategory category : SplitTunnelingCategory.values()) {
            if (ids.contains(category.id)) {
                categories.add(category);
            }
        }
        return categories;
    }

    static void saveCategories(Set<SplitTunnelingCategory> categories) {
        final StringBuilder ids = new StringBuilder();
        for (SplitTunnelingCategory category : categories) {
            if (ids.length() > 0) {
                ids.append(VALUE_SEPARATOR);
            }
            ids.append(category.id);
        }
        PatchSettings.setString(CATEGORIES, ids.toString());
    }

    static String keywordText() {
        return PatchSettings.getString(KEYWORDS, "");
    }

    static List<String> keywords() {
        return split(keywordText());
    }

    static void saveKeywordText(String text) {
        PatchSettings.setString(KEYWORDS, TextUtils.join(VALUE_SEPARATOR + " ", split(text)));
    }

    static int matchCount() {
        final String saved = PatchSettings.getString(MATCH_COUNT, "");
        if (saved.isEmpty()) {
            return UNKNOWN_MATCH_COUNT;
        }

        try {
            return Integer.parseInt(saved);
        } catch (NumberFormatException ex) {
            Log.w(VpnSettings.LOG_TAG, "Saved rule match count is not a number", ex);
            return UNKNOWN_MATCH_COUNT;
        }
    }

    static void saveMatchCount(int count) {
        PatchSettings.setString(MATCH_COUNT, (count == UNKNOWN_MATCH_COUNT) ? "" : String.valueOf(count));
    }

    static List<String> matchedApps() {
        final String saved = PatchSettings.getString(MATCHED_APPS, "");
        if (saved.isEmpty()
                || !ruleConfiguration(categories(), keywords()).equals(PatchSettings.getString(MATCHED_RULES, ""))) {
            return Collections.emptyList();
        }
        return Arrays.asList(saved.split(VALUE_SEPARATOR));
    }

    static void saveMatches(Set<SplitTunnelingCategory> categories, List<String> keywords,
            List<SplitTunnelingApp> matches) {
        PatchSettings.setString(MATCHED_APPS, TextUtils.join(VALUE_SEPARATOR, SplitTunnelingApp.packageNames(matches)));
        PatchSettings.setString(MATCHED_RULES, ruleConfiguration(categories, keywords));
        saveMatchCount(matches.size());
    }

    private static String ruleConfiguration(Set<SplitTunnelingCategory> categories, List<String> keywords) {
        final List<String> categoryIds = new ArrayList<>();
        for (SplitTunnelingCategory category : SplitTunnelingCategory.values()) {
            if (categories.contains(category)) {
                categoryIds.add(category.id);
            }
        }
        return TextUtils.join(VALUE_SEPARATOR, categoryIds) + CONFIGURATION_SEPARATOR
                + TextUtils.join(VALUE_SEPARATOR, keywords);
    }

    static boolean isActive() {
        return !categories().isEmpty() || !keywords().isEmpty();
    }

    static List<String> split(String text) {
        if (text == null) {
            return Collections.emptyList();
        }

        final List<String> values = new ArrayList<>();
        for (String value : text.split("[,\\n]")) {
            final String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && !values.contains(trimmed)) {
                values.add(trimmed);
            }
        }
        return values;
    }

}
