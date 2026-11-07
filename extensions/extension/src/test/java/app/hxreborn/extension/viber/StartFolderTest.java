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
package app.hxreborn.extension.viber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = StartFolderTest.HostFolderAccess.class,
        instrumentedPackages = "app.hxreborn.extension.viber")
public class StartFolderTest {

    private static final int UNREAD_INDEX = 2;

    private static final int CUSTOM_INDEX = 3;

    private static final int UNREAD_FOLDER_TYPE = 3;

    private static final int AI_FOLDER_TYPE = 4;

    private static final List<Object> SYSTEM_TABS = Arrays.asList("all", "unread", "ai");

    private static final List<Object> TABS = Arrays.asList("All", "Unfiled", "Unread", "Custom");

    @Before
    public void initializeSettings() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @Test
    public void selectAllByDefault() {
        Assert.assertEquals(0, StartFolder.initialIndex(TABS, -1, new Object()));
    }

    @Test
    public void selectSavedFolderOnFirstEmission() {
        PatchSettings.setStartFolder("Custom");
        Assert.assertEquals(CUSTOM_INDEX, StartFolder.initialIndex(TABS, -1, new Object()));
    }

    @Test
    public void preserveManualSelectionOnLaterEmissions() {
        PatchSettings.setStartFolder("Unread");
        Object collector = new Object();
        StartFolder.initialIndex(TABS, -1, collector);
        Assert.assertEquals(-1, StartFolder.initialIndex(TABS, -1, collector));
    }

    @Test
    public void restoreSavedFolderWhenViberResets() {
        PatchSettings.setStartFolder("Unread");
        StartFolder.initialIndex(TABS, -1, new Object());
        Assert.assertEquals(UNREAD_INDEX, StartFolder.resetIndex(0));
    }

    @Test
    public void fallBackToAllWhenSavedFolderIsDeleted() {
        PatchSettings.setStartFolder("Custom");
        Assert.assertEquals(0, StartFolder.initialIndex(TABS.subList(0, CUSTOM_INDEX), -1, new Object()));
        Assert.assertEquals(0, StartFolder.resetIndex(CUSTOM_INDEX));
    }

    @Test
    public void preserveManualSelectionAfterSavedFolderIsDeleted() {
        PatchSettings.setStartFolder("Custom");
        Object collector = new Object();
        List<Object> remaining = TABS.subList(0, CUSTOM_INDEX);
        Assert.assertEquals(0, StartFolder.initialIndex(remaining, -1, collector));
        Assert.assertEquals(UNREAD_INDEX, StartFolder.initialIndex(remaining, UNREAD_INDEX, collector));
    }

    @Test
    public void followFolderIdentityAfterReordering() {
        PatchSettings.setStartFolder("Unread");
        List<Object> reordered = Arrays.asList("All", "Unread", "Custom", "Unfiled");
        Assert.assertEquals(1, StartFolder.initialIndex(reordered, -1, new Object()));
    }

    @Test
    public void fallBackToAllWhenUnfiledIsDisabled() {
        PatchSettings.setStartFolder("Unfiled");
        Assert.assertEquals(0, StartFolder.initialIndex(Arrays.asList("All", "Unread", "Custom"), -1, new Object()));
    }

    @Test
    public void preserveResetWithoutLoadedFolders() {
        StartFolder.initialIndex(Collections.emptyList(), -1, new Object());
        Assert.assertEquals(CUSTOM_INDEX, StartFolder.resetIndex(CUSTOM_INDEX));
    }

    @Test
    public void showSavedFolderName() {
        PatchSettings.setStartFolder("Custom");
        StartFolder.initialIndex(TABS, -1, new Object());
        Assert.assertEquals("Custom",
                StartFolder.folderTitle(StartFolder.folders().get(StartFolder.savedFolderIndexOrAll()),
                        RuntimeEnvironment.getApplication().getResources()));
    }

    @Test
    public void showAllTitleFromHostResources() {
        StartFolder.initialIndex(SYSTEM_TABS, -1, new Object());
        Assert.assertEquals(RuntimeEnvironment.getApplication().getString(android.R.string.selectAll),
                StartFolder.folderTitle(StartFolder.folders().get(StartFolder.savedFolderIndexOrAll()),
                        RuntimeEnvironment.getApplication().getResources()));
    }

    @Test
    public void showUnreadTitleFromHostResources() {
        PatchSettings.setStartFolder("unread");
        StartFolder.initialIndex(SYSTEM_TABS, -1, new Object());
        Assert.assertEquals(RuntimeEnvironment.getApplication().getString(android.R.string.cancel),
                StartFolder.folderTitle(StartFolder.folders().get(StartFolder.savedFolderIndexOrAll()),
                        RuntimeEnvironment.getApplication().getResources()));
    }

    @Test
    public void showAiTitleFromHostResources() {
        PatchSettings.setStartFolder("ai");
        StartFolder.initialIndex(SYSTEM_TABS, -1, new Object());
        Assert.assertEquals(RuntimeEnvironment.getApplication().getString(android.R.string.ok),
                StartFolder.folderTitle(StartFolder.folders().get(StartFolder.savedFolderIndexOrAll()),
                        RuntimeEnvironment.getApplication().getResources()));
    }

    @Test
    public void copyFolderListBeforeHostMutatesIt() {
        PatchSettings.setStartFolder("Unread");
        List<Object> source = new ArrayList<>(TABS);
        StartFolder.initialIndex(source, -1, new Object());
        source.clear();
        Assert.assertEquals(UNREAD_INDEX, StartFolder.resetIndex(0));
    }

    @Test
    public void resetSavedFolderToAll() {
        PatchSettings.setStartFolder("Custom");
        StartFolder.initialIndex(TABS, -1, new Object());
        PatchSettings.resetToDefaults();
        Assert.assertEquals(0, StartFolder.resetIndex(CUSTOM_INDEX));
    }

    @Implements(StartFolder.class)
    public static class HostFolderAccess {

        @Implementation
        protected static String folderId(Object tab) {
            return (String) tab;
        }

        @Implementation
        protected static int folderType(Object tab) {
            return switch ((String) tab) {
                case "all" -> 0;
                case "unread" -> UNREAD_FOLDER_TYPE;
                case "ai" -> AI_FOLDER_TYPE;
                default -> -1;
            };
        }

        @Implementation
        protected static int allTitleId() {
            return android.R.string.selectAll;
        }

        @Implementation
        protected static int unreadTitleId() {
            return android.R.string.cancel;
        }

        @Implementation
        protected static int aiAssistantsTitleId() {
            return android.R.string.ok;
        }

        @Implementation
        protected static String folderName(Object tab) {
            return (String) tab;
        }

    }

}
