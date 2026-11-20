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
package app.hxreborn.extension.protonauthenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import app.hxreborn.extension.proton.RecordingSettingsScreen;

@RunWith(RobolectricTestRunner.class)
public final class AuthenticatorSettingsTest {

    private final RecordingSettingsScreen screen = new RecordingSettingsScreen();

    @Test
    public void unpatchedBuildAddsNoRowAndNoName() {
        final AuthenticatorSettings settings = new AuthenticatorSettings();

        assertTrue(this.screen.rowsFrom(settings, "layout").isEmpty());
        assertTrue(settings.appliedPatchNames().isEmpty());
    }

    @Test
    @Config(shadows = { SectionsShadow.class })
    public void patchedBuildAddsTheSettingsSectionRowsInOrder() {
        final AuthenticatorSettings settings = new AuthenticatorSettings();

        this.screen.rowsFrom(settings, "layout");

        assertEquals(Arrays.asList("Hide Spread the word", "Hide Discover Proton", "Hide Proton Pass banner"),
                this.screen.titles());
        assertTrue(settings.hasLayoutComponents());
    }

    @Implements(SettingsSections.class)
    public static final class SectionsShadow {

        @Implementation
        public static boolean isPatched() {
            return true;
        }

    }

}
