/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.typing;

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
public final class TypingIndicatorTest {

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.DISABLE_TYPING_INDICATOR.resetToDefault();
    }

    @Test
    public void sendsTypingByDefault() {
        assertTrue(TypingIndicator.shouldSend(true));
    }

    @Test
    public void withholdsTypingOnceTurnedOn() {
        // given
        PatchSettings.DISABLE_TYPING_INDICATOR.save(true);

        // when
        final boolean send = TypingIndicator.shouldSend(true);

        // then
        assertFalse(send);
    }

    @Test
    public void sendsTypingAgainOnceTurnedOff() {
        // given
        PatchSettings.DISABLE_TYPING_INDICATOR.save(true);
        PatchSettings.DISABLE_TYPING_INDICATOR.save(false);

        // when
        final boolean send = TypingIndicator.shouldSend(true);

        // then
        assertTrue(send);
    }

    @Test
    public void neverSendsTypingStoppedAsTyping() {
        assertFalse(TypingIndicator.shouldSend(false));
    }

    @Test
    public void reportsPatchAsNotIncludedUntilApplied() {
        assertFalse(TypingIndicator.isPatchIncluded());
    }

}
