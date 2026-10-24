/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.streak;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import app.hxreborn.extension.bereal.PatchSettings;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class StreakPromptsTest {

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.REMOVE_STREAK_PROMPTS.save(true);
    }

    @Test
    public void removesPromptsByDefault() {
        assertTrue(StreakPrompts.removed());
    }

    @Test
    public void keepsPromptsWhenTheSettingIsOff() {
        PatchSettings.REMOVE_STREAK_PROMPTS.save(false);

        assertFalse(StreakPrompts.removed());
    }

    @Test
    public void reportsThePatchAsNotIncludedUntilPatched() {
        assertFalse(StreakPrompts.isPatchIncluded());
    }

}
