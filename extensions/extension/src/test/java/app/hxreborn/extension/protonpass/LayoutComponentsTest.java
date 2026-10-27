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
package app.hxreborn.extension.protonpass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import app.hxreborn.extension.proton.PatchSettings;
import app.hxreborn.extension.proton.PatchedBuild;

@RunWith(RobolectricTestRunner.class)
public final class LayoutComponentsTest {

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
    }

    @Test
    public void ignoresAStoredHideSettingWhenUnpatched() {
        // given
        PatchSettings.setEnabled(LayoutComponents.HELP_CENTER, true);
        PatchSettings.setEnabled(LayoutComponents.SPREAD_THE_WORD, true);
        PatchSettings.setEnabled(LayoutComponents.ABOUT, true);

        // when
        assertFalse(LayoutComponents.isHelpCenterHidden());
        assertFalse(LayoutComponents.isSpreadTheWordHidden());
        assertFalse(LayoutComponents.isAboutHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void showsEveryComponentUntilHidden() {
        assertFalse(LayoutComponents.isHelpCenterHidden());
        assertFalse(LayoutComponents.isSpreadTheWordHidden());
        assertFalse(LayoutComponents.isAboutHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesHelpCenterAlone() {
        // given
        PatchSettings.setEnabled(LayoutComponents.HELP_CENTER, true);

        // when
        assertTrue(LayoutComponents.isHelpCenterHidden());
        assertFalse(LayoutComponents.isSpreadTheWordHidden());
        assertFalse(LayoutComponents.isAboutHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesSpreadTheWordAlone() {
        // given
        PatchSettings.setEnabled(LayoutComponents.SPREAD_THE_WORD, true);

        // when
        assertTrue(LayoutComponents.isSpreadTheWordHidden());
        assertFalse(LayoutComponents.isHelpCenterHidden());
        assertFalse(LayoutComponents.isAboutHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesTheAboutSectionAlone() {
        // given
        PatchSettings.setEnabled(LayoutComponents.ABOUT, true);

        // when
        assertTrue(LayoutComponents.isAboutHidden());
        assertFalse(LayoutComponents.isHelpCenterHidden());
        assertFalse(LayoutComponents.isSpreadTheWordHidden());
    }

    @Test
    @Config(shadows = Patched.class)
    public void hidesAnyKeyPassedToIsHidden() {
        // given
        PatchSettings.setEnabled("hide_other", true);

        // when
        assertTrue(LayoutComponents.isHidden("hide_other"));
        assertFalse(LayoutComponents.isHidden("hide_unset"));
    }

    @Implements(LayoutComponents.class)
    public static final class Patched {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

}
