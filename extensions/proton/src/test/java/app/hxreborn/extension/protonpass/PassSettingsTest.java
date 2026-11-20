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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import app.hxreborn.extension.proton.PatchedBuild;
import app.hxreborn.extension.proton.RecordingSettingsScreen;

@RunWith(RobolectricTestRunner.class)
public final class PassSettingsTest {

    private final RecordingSettingsScreen screen = new RecordingSettingsScreen();

    @Test
    public void unpatchedBuildAddsNoRowAndNoName() {
        final PassSettings settings = new PassSettings();

        assertTrue(this.screen.rowsFrom(settings, "layout").isEmpty());
        assertTrue(settings.appliedPatchNames().isEmpty());
    }

    @Test
    @Config(shadows = { PatchedBuild.Pass.class })
    public void patchedBuildListsTheNavigationAndPromotionPatches() {
        assertEquals(Collections.singletonList("Hide promotional messages"), new PassSettings().appliedPatchNames());
    }

    @Test
    @Config(shadows = { LayoutShadow.class })
    public void patchedBuildAddsTheLayoutRowsInOrder() {
        final PassSettings settings = new PassSettings();

        this.screen.rowsFrom(settings, "layout");

        assertEquals(Arrays.asList("Hide Help center", "Hide Spread the word", "Hide About"),
                this.screen.titles());
        assertTrue(settings.hasLayoutComponents());
    }

    @org.robolectric.annotation.Implements(LayoutComponents.class)
    public static final class LayoutShadow {

        @org.robolectric.annotation.Implementation
        public static boolean isPatched() {
            return true;
        }

        @org.robolectric.annotation.Implementation
        public static boolean hasSpreadTheWord() {
            return true;
        }

    }

}
