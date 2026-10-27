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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class FriendSuggestionsTest {

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.REMOVE_FRIEND_SUGGESTIONS.save(true);
    }

    @Test
    public void removesSuggestionsByDefault() {
        assertTrue(FriendSuggestions.removed());
    }

    @Test
    public void emptiesTheSuggestionListWhenRemoved() {
        final List<String> suggestions = Arrays.asList("a", "b");

        assertTrue(FriendSuggestions.suggestions(suggestions).isEmpty());
    }

    @Test
    public void keepsTheSuggestionListWhenShown() {
        PatchSettings.REMOVE_FRIEND_SUGGESTIONS.save(false);
        final List<String> suggestions = Arrays.asList("a", "b");

        assertSame(suggestions, FriendSuggestions.suggestions(suggestions));
    }

    @Test
    public void turnsOffTheFeedWidgetWhenRemoved() {
        assertFalse(FriendSuggestions.shouldDisplay(true));
    }

    @Test
    public void keepsTheFeedWidgetStateWhenShown() {
        PatchSettings.REMOVE_FRIEND_SUGGESTIONS.save(false);

        assertTrue(FriendSuggestions.shouldDisplay(true));
        assertFalse(FriendSuggestions.shouldDisplay(false));
    }

    @Test
    public void followsTheSetting() {
        PatchSettings.REMOVE_FRIEND_SUGGESTIONS.save(false);
        assertFalse(FriendSuggestions.removed());
        PatchSettings.REMOVE_FRIEND_SUGGESTIONS.save(true);
        assertTrue(FriendSuggestions.removed());
    }

}
