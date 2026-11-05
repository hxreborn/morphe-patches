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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
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
@Config(manifest = Config.NONE, shadows = BusinessChatsTest.AppliedBusinessControls.class)
public final class BusinessChatsTest {

    private static final String SELECTION_PREFIX = "[conversations].[deleted] = 0 and ";

    private static final Set<String> SELECTED = Collections.singleton("1");

    private SQLiteDatabase database;

    @Before
    public void createConversations() {
        this.database = SQLiteDatabase.create(null);
        this.database.execSQL("CREATE TABLE conversations (_id INTEGER PRIMARY KEY, deleted INTEGER DEFAULT 0, "
                + "conversation_type INTEGER DEFAULT 0, business_inbox_flags INTEGER DEFAULT 1, "
                + "unread_events_count INTEGER DEFAULT 0, flags INTEGER DEFAULT 0, flags2 INTEGER DEFAULT 0)");
        this.database.execSQL("INSERT INTO conversations (_id) VALUES (1), (2)");
    }

    @After
    public void clearPreferencesAndCloseDatabase() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("hx_viber_patches", Context.MODE_PRIVATE).edit().clear().commit();
        this.database.close();
    }

    @Test
    public void filterOnlySelectedReadBusinessChats() {
        Assert.assertEquals(Collections.singleton(2L), visible(true, SELECTED));
    }

    @Test
    public void restoreEveryNewUnreadMessage() {
        this.database.execSQL("UPDATE conversations SET unread_events_count = 1 WHERE _id = 1");
        Assert.assertEquals(new HashSet<>(Arrays.asList(1L, 2L)), visible(true, SELECTED));
        this.database.execSQL("UPDATE conversations SET unread_events_count = 0 WHERE _id = 1");
        Assert.assertEquals(Collections.singleton(2L), visible(true, SELECTED));
    }

    @Test
    public void preserveMarkedUnreadChats() {
        this.database.execSQL("UPDATE conversations SET flags = ? WHERE _id = 1", new Object[] { 1L << 33 });
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveVerificationChatsAfterReading() {
        this.database.execSQL("UPDATE conversations SET flags2 = ? WHERE _id = 1", new Object[] { 1L << 14 });
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveUnknownUnreadCounts() {
        this.database.execSQL("UPDATE conversations SET unread_events_count = NULL WHERE _id = 1");
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
        this.database.execSQL("UPDATE conversations SET unread_events_count = -1 WHERE _id = 1");
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveUnknownFlags() {
        this.database.execSQL("UPDATE conversations SET flags = NULL WHERE _id = 1");
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveChatsThatStopBeingBusinesses() {
        this.database.execSQL("UPDATE conversations SET business_inbox_flags = 0 WHERE _id = 1");
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveGroupsAndInboxRows() {
        this.database.execSQL("UPDATE conversations SET conversation_type = 1 WHERE _id = 1");
        Assert.assertTrue(visible(true, SELECTED).contains(1L));
    }

    @Test
    public void preserveHostDeletion() {
        this.database.execSQL("UPDATE conversations SET deleted = 1 WHERE _id = 2");
        Assert.assertTrue(visible(true, SELECTED).isEmpty());
        Assert.assertEquals(Collections.singleton(1L), visible(false, SELECTED));
    }

    @Test
    public void preserveSelectionWhenDisabledOrEmpty() {
        Assert.assertSame(SELECTION_PREFIX, BusinessChats.filterSelection(SELECTION_PREFIX, false, SELECTED));
        Assert.assertSame(SELECTION_PREFIX,
                BusinessChats.filterSelection(SELECTION_PREFIX, true, Collections.emptySet()));
        Assert.assertNull(BusinessChats.filterSelection(null, true, SELECTED));
    }

    @Test
    public void rejectMalformedAndNonpositiveIds() {
        Set<String> malformed = new HashSet<>(Arrays.asList("1) OR 1=1", "0", "-1", "9999999999999999999999"));
        Assert.assertSame(SELECTION_PREFIX, BusinessChats.filterSelection(SELECTION_PREFIX, true, malformed));
        malformed.add("1");
        Assert.assertEquals(Collections.singleton(2L), visible(true, malformed));
    }

    @Test
    public void suppressOnlyDiscoveryPreferenceKeys() {
        Assert.assertTrue(BusinessChats.isDiscoveryPrompt("pref_business_chat_inbox_show_intro_dialog_ftue"));
        Assert.assertTrue(BusinessChats.isDiscoveryPrompt("pref_business_chat_inbox_show_tooltip_ftue"));
        Assert.assertTrue(BusinessChats.isDiscoveryPrompt("can_show_business_inbox_promotion_tooltip"));
        Assert.assertFalse(BusinessChats.isDiscoveryPrompt("receive_business_messages_pref_"));
        Assert.assertFalse(BusinessChats.isDiscoveryPrompt("pref_business_chat_inbox_pinned"));
        Assert.assertFalse(BusinessChats.isDiscoveryPrompt(null));
    }

    @Test
    public void resetConversationSelectionWithSwitchDefaults() {
        Context context = RuntimeEnvironment.getApplication();
        PatchSettings.initialize(context);
        context.getSharedPreferences("hx_viber_patches", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("selected_business_chats", SELECTED)
            .commit();
        PatchSettings.setEnabled(PatchSetting.FILTER_BUSINESS_CHATS.key, false);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.isEnabled(PatchSetting.FILTER_BUSINESS_CHATS.key));
        Assert.assertTrue(context.getSharedPreferences("hx_viber_patches", Context.MODE_PRIVATE).getAll().isEmpty());
    }

    @Test
    public void readSavedSelectionAndMasterSwitch() {
        Context context = RuntimeEnvironment.getApplication();
        PatchSettings.initialize(context);
        PatchSettings.setSelectedBusinessChats(SELECTED);
        Assert.assertNotEquals(SELECTION_PREFIX, BusinessChats.filterSelection(SELECTION_PREFIX));
        Assert.assertTrue(BusinessChats.suppressPrompt("pref_business_chat_inbox_show_intro_dialog_ftue"));
        PatchSettings.setEnabled(PatchSetting.FILTER_BUSINESS_CHATS.key, false);
        Assert.assertSame(SELECTION_PREFIX, BusinessChats.filterSelection(SELECTION_PREFIX));
        Assert.assertFalse(BusinessChats.suppressPrompt("pref_business_chat_inbox_show_intro_dialog_ftue"));
    }

    @Test
    public void keepSavedSelectionIndependentOfReturnedSet() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.setSelectedBusinessChats(SELECTED);
        PatchSettings.selectedBusinessChats().clear();
        Assert.assertEquals(SELECTED, PatchSettings.selectedBusinessChats());
    }

    private Set<Long> visible(boolean enabled, Set<String> selected) {
        String selection = BusinessChats.filterSelection(SELECTION_PREFIX, enabled, selected);
        Set<Long> ids = new HashSet<>();
        try (Cursor cursor = this.database.rawQuery(
                "SELECT _id FROM conversations WHERE " + selection + "1 = 1 GROUP BY _id ORDER BY _id DESC", null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    @Implements(AppliedPatches.class)
    public static final class AppliedBusinessControls {

        @Implementation
        public static boolean filterBusinessChats() {
            return true;
        }

    }

}
