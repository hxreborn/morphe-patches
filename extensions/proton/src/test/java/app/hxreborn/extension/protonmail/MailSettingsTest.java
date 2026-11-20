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
package app.hxreborn.extension.protonmail;

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
import app.hxreborn.extension.protonmail.accountswipe.AccountSwipeRows;
import app.hxreborn.extension.protonmail.accountswipe.AvatarSwitchStyleDialog;

@RunWith(RobolectricTestRunner.class)
public final class MailSettingsTest {

    private final RecordingSettingsScreen screen = new RecordingSettingsScreen();

    @Test
    public void unpatchedBuildAddsNoRowAndNoName() {
        final MailSettings settings = new MailSettings();

        assertTrue(this.screen.rowsFrom(settings, "account").isEmpty());
        assertTrue(this.screen.rowsFrom(settings, "general").isEmpty());
        assertTrue(this.screen.rowsFrom(settings, "layout").isEmpty());
        assertTrue(settings.appliedPatchNames().isEmpty());
    }

    @Test
    public void accountSwipeRowsAddTheStyleAndVibrationRowsInOrder() {
        AccountSwipeRows.add(this.screen, this.screen.views());

        assertEquals(Arrays.asList(AvatarSwitchStyleDialog.TITLE, "Vibrate on switch"), this.screen.titles());
        assertEquals(2, this.screen.views().size());
    }

    @Test
    public void scheduledDeletionRowsAddTheEditorRow() {
        ScheduledDeletionRows.add(this.screen, this.screen.views());

        assertEquals(Collections.singletonList(ScheduledDeletionEditor.SETTINGS_ROW_TITLE), this.screen.titles());
        assertEquals(1, this.screen.views().size());
    }

    @Test
    @Config(shadows = { PatchedBuild.Mail.class })
    public void patchedBuildListsTheMailPatchesInDisplayOrder() {
        assertEquals(
                Arrays.asList("Remove 'Sent from' signature", "Remove free accounts limit",
                        "Scheduled Trash and Spam deletion", "Unlock custom time picker", "Swipe to switch accounts"),
                new MailSettings().appliedPatchNames());
    }

    @Test
    @Config(shadows = { SidebarShadow.class })
    public void patchedBuildAddsTheSidebarRows() {
        final MailSettings settings = new MailSettings();

        this.screen.rowsFrom(settings, "layout");

        assertEquals(Arrays.asList("Hide Subscription", "Hide Report a problem"), this.screen.titles());
        assertTrue(settings.hasLayoutComponents());
    }

    @org.robolectric.annotation.Implements(SidebarEntries.class)
    public static final class SidebarShadow {

        @org.robolectric.annotation.Implementation
        public static boolean isPatched() {
            return true;
        }

    }

}
