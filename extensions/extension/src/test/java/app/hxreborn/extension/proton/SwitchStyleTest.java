/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.widget.Switch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SwitchStyleTest {

    private static final int ACCENT = 0xFF6D4AFF;

    @Test
    public void offersNoMaterialThumbWithoutTheHostStyles() {
        assertNull(SwitchStyle.thumb(RuntimeEnvironment.getApplication(), ACCENT));
    }

    @Test
    public void offersNoMaterialTrackWithoutTheHostStyles() {
        assertNull(SwitchStyle.track(RuntimeEnvironment.getApplication(), ACCENT));
    }

    @Test
    public void fallsBackToTheWebToggleWithoutTheHostStyles() {
        // given
        final Switch control = new Switch(RuntimeEnvironment.getApplication());

        // when
        SwitchStyle.apply(control, ACCENT);

        // then
        assertNotNull(control.getThumbDrawable());
        assertNotNull(control.getTrackDrawable());
        assertEquals(52, control.getSwitchMinWidth());
    }

}
