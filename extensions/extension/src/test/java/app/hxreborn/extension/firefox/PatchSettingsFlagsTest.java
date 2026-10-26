/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsFlagsTest {

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void disablingTabNextTurnsOffTabEnd() {
        // given
        Context context = application();
        PatchSettings.saveTabNext(context, true);
        PatchSettings.saveTabEnd(context, true);

        // when
        PatchSettings.saveTabNext(context, false);

        // then
        assertFalse(PatchSettings.TAB_NEXT.saved(context));
        assertFalse(PatchSettings.TAB_END.saved(context));
        assertFalse(PatchSettings.tabNext());
        assertFalse(PatchSettings.tabEnd());
    }

    @Test
    public void enablingTabEndTurnsOnTabNext() {
        // given
        Context context = application();
        PatchSettings.saveTabNext(context, false);

        // when
        PatchSettings.saveTabEnd(context, true);

        // then
        assertTrue(PatchSettings.TAB_NEXT.saved(context));
        assertTrue(PatchSettings.TAB_END.saved(context));
        assertTrue(PatchSettings.tabNext());
        assertTrue(PatchSettings.tabEnd());
    }

    @Test
    public void enablingTabNextKeepsTabEnd() {
        // given
        Context context = application();
        PatchSettings.saveTabNext(context, true);
        PatchSettings.saveTabEnd(context, false);

        // when
        PatchSettings.saveTabNext(context, true);

        // then
        assertTrue(PatchSettings.TAB_NEXT.saved(context));
        assertFalse(PatchSettings.TAB_END.saved(context));
        assertFalse(PatchSettings.tabEnd());
    }

}
