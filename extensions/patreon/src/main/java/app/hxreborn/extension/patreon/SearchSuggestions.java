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
package app.hxreborn.extension.patreon;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public final class SearchSuggestions {

    static final String HIDE_SEARCH_SUGGESTIONS = "hide_search_suggestions";

    private static final Set<String> SUGGESTION_TYPES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("SUGGESTED_CAMPAIGNS", "TRENDING_CAMPAIGNS", "POPULAR_CAMPAIGNS",
                "NEW_CAMPAIGNS", "TOPICS", "RECOMMENDED_CAMPAIGNS", "CURATED_GUIDES")));

    private static final Set<String> TOPIC_DISPLAYS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("ONE_ROW_TOPIC_CAROUSEL", "TWO_ROW_TOPIC_CAROUSEL", "HIGHLIGHTED_ONE_ROW_TOPIC_CAROUSEL")));

    private SearchSuggestions() {
    }

    public static boolean isPatched() {
        return false;
    }

    static boolean isEnabled() {
        return PatchSettings.isEnabled(HIDE_SEARCH_SUGGESTIONS, true);
    }

    public static List<?> filterItems(List<?> items, Enum<?> sectionType, Enum<?> displayType) {
        boolean suggestedSection = sectionType != null && SUGGESTION_TYPES.contains(sectionType.name());
        boolean topicCarousel = displayType != null && TOPIC_DISPLAYS.contains(displayType.name());
        if ((suggestedSection || topicCarousel) && isEnabled()) {
            return Collections.emptyList();
        }
        return items;
    }

    public static String patchName() {
        return null;
    }

    public static String patchDescription() {
        return null;
    }

}
