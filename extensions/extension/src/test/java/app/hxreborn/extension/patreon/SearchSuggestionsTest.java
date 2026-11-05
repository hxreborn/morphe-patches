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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SearchSuggestionsTest {

    private enum SectionType {

        SUGGESTED_CAMPAIGNS, TRENDING_CAMPAIGNS, POPULAR_CAMPAIGNS, NEW_CAMPAIGNS, TOPICS, RECOMMENDED_CAMPAIGNS,
        CURATED_GUIDES, RECENTLY_VISITED_CAMPAIGNS, YOUR_CREATORS, UNKNOWN

    }

    private enum DisplayType {

        ONE_ROW_TOPIC_CAROUSEL, TWO_ROW_TOPIC_CAROUSEL, HIGHLIGHTED_ONE_ROW_TOPIC_CAROUSEL, TILE_CAMPAIGN_CAROUSEL

    }

    @Before
    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void removeSuggestionSectionsByType() {
        List<Object> items = Collections.singletonList(new Object());
        SectionType[] suggestions = { SectionType.SUGGESTED_CAMPAIGNS, SectionType.TRENDING_CAMPAIGNS,
                SectionType.POPULAR_CAMPAIGNS, SectionType.NEW_CAMPAIGNS, SectionType.TOPICS,
                SectionType.RECOMMENDED_CAMPAIGNS, SectionType.CURATED_GUIDES };
        for (SectionType type : suggestions) {
            assertTrue(type.name(), SearchSuggestions.filterItems(items, type, null).isEmpty());
        }
    }

    @Test
    public void removeTopicCarouselsWithoutSectionType() {
        List<Object> items = Collections.singletonList(new Object());
        DisplayType[] topics = { DisplayType.ONE_ROW_TOPIC_CAROUSEL, DisplayType.TWO_ROW_TOPIC_CAROUSEL,
                DisplayType.HIGHLIGHTED_ONE_ROW_TOPIC_CAROUSEL };
        for (DisplayType type : topics) {
            assertTrue(type.name(), SearchSuggestions.filterItems(items, null, type).isEmpty());
            assertTrue(type.name(), SearchSuggestions.filterItems(items, SectionType.UNKNOWN, type).isEmpty());
        }
    }

    @Test
    public void preserveTopicCarouselsWhenDisabled() {
        PatchSettings.setEnabled(SearchSuggestions.HIDE_SEARCH_SUGGESTIONS, false);
        List<Object> items = Collections.singletonList(new Object());
        assertSame(items, SearchSuggestions.filterItems(items, null, DisplayType.TWO_ROW_TOPIC_CAROUSEL));
    }

    @Test
    public void preserveUnknownSectionsWithOtherDisplays() {
        List<Object> items = Collections.singletonList(new Object());
        assertSame(items,
                SearchSuggestions.filterItems(items, SectionType.UNKNOWN, DisplayType.TILE_CAMPAIGN_CAROUSEL));
    }

    @Test
    public void keepOriginalItemsWhenDisabled() {
        PatchSettings.setEnabled(SearchSuggestions.HIDE_SEARCH_SUGGESTIONS, false);
        List<Object> items = Collections.singletonList(new Object());
        assertSame(items, SearchSuggestions.filterItems(items, SectionType.POPULAR_CAMPAIGNS, null));
    }

    @Test
    public void preservePersonalSections() {
        List<Object> items = Collections.singletonList(new Object());
        assertSame(items, SearchSuggestions.filterItems(items, SectionType.RECENTLY_VISITED_CAMPAIGNS, null));
        assertSame(items, SearchSuggestions.filterItems(items, SectionType.YOUR_CREATORS, null));
    }

    @Test
    public void preserveUnknownSections() {
        List<Object> items = Collections.singletonList(new Object());
        assertSame(items, SearchSuggestions.filterItems(items, SectionType.UNKNOWN, null));
        assertSame(items, SearchSuggestions.filterItems(items, null, null));
    }

    @Test
    public void preserveMissingItemsForUnknownSections() {
        assertNull(SearchSuggestions.filterItems(null, SectionType.UNKNOWN, null));
    }

}
