/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
