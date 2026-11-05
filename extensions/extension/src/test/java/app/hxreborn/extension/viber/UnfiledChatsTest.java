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
import java.util.List;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
@Config(manifest = Config.NONE, sdk = 28,
        shadows = { UnfiledChatsTest.AppliedUnfiledChats.class, UnfiledChatsTest.HostFolderAccess.class },
        instrumentedPackages = "app.hxreborn.extension.viber")
public class UnfiledChatsTest {

    private static final String UNFILED = "1";

    private static final Object UNFILED_QUERY = new Object();

    private static final Object ALL_QUERY = new Object();

    private static final String CUSTOM = "[conversations].[_id] in (select conversation_id from folders_to_chats where folder_id = 'custom')";

    private static final int ADD_CHATS_ID = 1;

    private static final int TITLE_ID = 2;

    private static final int SUBTITLE_ID = 3;

    private static final String TAB = "Unfiled";

    private SQLiteDatabase database;

    @Before
    public void initializeFolders() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
        UnfiledChats.selectFolder("all");
        UnfiledChats.rememberQuery("hx_unfiled_chats", UNFILED_QUERY);
        this.database = SQLiteDatabase.create(null);
        this.database.execSQL("CREATE TABLE conversations (_id INTEGER PRIMARY KEY, deleted INTEGER, pinned INTEGER)");
        this.database.execSQL("CREATE TABLE folders (id TEXT PRIMARY KEY, type INTEGER)");
        this.database.execSQL("CREATE TABLE folders_to_chats (folder_id TEXT, conversation_id INTEGER)");
        this.database.execSQL(
                "INSERT INTO folders VALUES ('custom', 100), ('other', 100), ('predefined', 1), ('ai', 4), ('unread', 3)");
        this.database.execSQL("INSERT INTO conversations VALUES (1, 0, 0), (2, 0, 1), (3, 0, 0), (4, 1, 0), (5, 0, 0)");
        this.database.execSQL(
                "INSERT INTO folders_to_chats VALUES ('custom', 1), ('other', 1), ('ai', 2), ('predefined', 3), ('unread', 3), ('missing', 5), ('custom', NULL), ('custom', -1)");
    }

    @After
    public void closeFolders() {
        this.database.close();
        PatchSettings.resetToDefaults();
    }

    @Test
    public void excludeOnlyChatsInCustomFolders() {
        Assert.assertEquals(Arrays.asList(2L, 3L, 5L), readChats(UNFILED));
    }

    @Test
    public void preserveCustomFolderSelection() {
        Assert.assertEquals(CUSTOM, UnfiledChats.filterSelection(ALL_QUERY, CUSTOM));
        Assert.assertEquals(Arrays.asList(1L), readChats(CUSTOM));
    }

    @Test
    public void reflectMembershipChangesWithoutChangingFolders() {
        this.database.execSQL("DELETE FROM folders_to_chats WHERE conversation_id = 1");
        this.database.execSQL("INSERT INTO folders_to_chats VALUES ('custom', 2)");
        Assert.assertEquals(Arrays.asList(1L, 3L, 5L), readChats(UNFILED));
        try (Cursor folders = this.database.rawQuery("SELECT COUNT(*) FROM folders", null)) {
            Assert.assertTrue(folders.moveToFirst());
            Assert.assertEquals(5, folders.getInt(0));
        }
    }

    @Test
    public void preservePinnedOrderAndDeletedChatFilter() {
        String selection = "deleted = 0 ORDER BY pinned DESC, _id DESC";
        String query = "SELECT _id FROM conversations WHERE " + UnfiledChats.filterSelection(UNFILED_QUERY, selection);
        List<Long> ids = new ArrayList<>();
        try (Cursor chats = this.database.rawQuery(query, null)) {
            while (chats.moveToNext()) {
                ids.add(chats.getLong(0));
            }
        }
        Assert.assertEquals(Arrays.asList(2L, 5L, 3L), ids);
    }

    @Test
    public void includeEveryChatWhenNoCustomFoldersExist() {
        this.database.execSQL("DELETE FROM folders WHERE type = 100");
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 5L), readChats(UNFILED));
    }

    @Test
    public void returnEmptyListWhenEveryChatIsFiled() {
        this.database.execSQL("INSERT INTO folders_to_chats SELECT 'custom', _id FROM conversations");
        Assert.assertTrue(readChats(UNFILED).isEmpty());
    }

    @Test
    public void preserveAllChatsWhenSwitchIsDisabled() {
        PatchSettings.setEnabled(PatchSetting.UNFILED_CHATS.key, false);
        Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 5L), readChats(UNFILED));
        List<Object> tabs = new ArrayList<>();
        Assert.assertTrue(UnfiledChats.addTabAndIsEmpty(tabs));
    }

    @Test
    public void preserveNullSelectionAndMissingView() {
        Assert.assertNull(UnfiledChats.filterSelection(UNFILED_QUERY, null));
        UnfiledChats.updateEmptyView(null);
    }

    @Test
    public void insertUnfiledBeforeExistingFolders() {
        List<Object> tabs = new ArrayList<>(Arrays.asList("Work", "Family"));
        Assert.assertFalse(UnfiledChats.addTabAndIsEmpty(tabs));
        Assert.assertEquals(Arrays.asList(TAB, "Work", "Family"), tabs);
    }

    @Test
    public void showUnfiledWithoutUserFolders() {
        List<Object> tabs = new ArrayList<>();
        Assert.assertFalse(UnfiledChats.addTabAndIsEmpty(tabs));
        Assert.assertEquals(Arrays.asList(TAB), tabs);
    }

    @Test
    public void preserveExistingTabsWhenDisabled() {
        PatchSettings.setEnabled(PatchSetting.UNFILED_CHATS.key, false);
        List<Object> tabs = new ArrayList<>(Arrays.asList("Work"));
        Assert.assertFalse(UnfiledChats.addTabAndIsEmpty(tabs));
        Assert.assertEquals(Arrays.asList("Work"), tabs);
    }

    @Test
    public void removeAddChatsActionFromUnfiledEmptyState() {
        LinearLayout root = emptyView();
        UnfiledChats.selectFolder("hx_unfiled_chats");
        UnfiledChats.updateEmptyView(root);
        Assert.assertEquals(View.GONE, root.findViewById(ADD_CHATS_ID).getVisibility());
        Assert.assertEquals("No unfiled chats", ((TextView) root.findViewById(TITLE_ID)).getText().toString());
        Assert.assertEquals("Chats in your folders appear in All.",
                ((TextView) root.findViewById(SUBTITLE_ID)).getText().toString());
    }

    @Test
    public void restoreAddChatsActionForOtherFolders() {
        LinearLayout root = emptyView();
        UnfiledChats.selectFolder("hx_unfiled_chats");
        UnfiledChats.updateEmptyView(root);
        UnfiledChats.selectFolder("custom");
        UnfiledChats.updateEmptyView(root);
        Assert.assertEquals(View.VISIBLE, root.findViewById(ADD_CHATS_ID).getVisibility());
    }

    @Test
    public void clearSelectionForMissingFolder() {
        LinearLayout root = emptyView();
        UnfiledChats.selectFolder("hx_unfiled_chats");
        UnfiledChats.updateEmptyView(root);
        UnfiledChats.selectFolder(null);
        UnfiledChats.updateEmptyView(root);
        Assert.assertEquals(View.VISIBLE, root.findViewById(ADD_CHATS_ID).getVisibility());
    }

    @Test
    public void preserveUnrelatedView() {
        UnfiledChats.selectFolder("hx_unfiled_chats");
        UnfiledChats.updateEmptyView(new LinearLayout(RuntimeEnvironment.getApplication()));
    }

    @Test
    public void preserveAllConversationEligibility() {
        String where = "deleted = 0 AND _id <> 3 ORDER BY _id";
        List<Long> ids = new ArrayList<>();
        String query = "SELECT _id FROM conversations WHERE " + UnfiledChats.filterSelection(UNFILED_QUERY, where);
        try (Cursor chats = this.database.rawQuery(query, null)) {
            while (chats.moveToNext()) {
                ids.add(chats.getLong(0));
            }
        }
        Assert.assertEquals(Arrays.asList(2L, 5L), ids);
    }

    @Test
    public void preserveGroupingAndOrderingAroundNestedSubquery() {
        String where = "_id IN (SELECT _id FROM conversations GROUP BY _id) AND deleted = 0"
                + " GROUP BY _id ORDER BY pinned DESC, _id DESC";
        String filtered = UnfiledChats.filterSelection(UNFILED_QUERY, where);
        List<Long> ids = new ArrayList<>();
        try (Cursor chats = this.database.rawQuery("SELECT _id FROM conversations WHERE " + filtered, null)) {
            while (chats.moveToNext()) {
                ids.add(chats.getLong(0));
            }
        }
        Assert.assertEquals(Arrays.asList(2L, 5L, 3L), ids);
    }

    @Test
    public void keepOtherQueryObjectsUnfiltered() {
        String where = "deleted = 0 GROUP BY _id ORDER BY pinned DESC";
        Assert.assertSame(where, UnfiledChats.filterSelection(ALL_QUERY, where));
        Assert.assertSame(where, UnfiledChats.filterSelection(null, where));
    }

    @Test
    public void selectAllQueryOnlyForSyntheticFolder() {
        Assert.assertNull(UnfiledChats.queryFolder("hx_unfiled_chats"));
        Assert.assertEquals("custom", UnfiledChats.queryFolder("custom"));
        Assert.assertNull(UnfiledChats.queryFolder(null));
    }

    @Test
    public void keepUnregisteredFolderQueryUnfiltered() {
        Object custom = new Object();
        UnfiledChats.rememberQuery("custom", custom);
        UnfiledChats.rememberQuery("hx_unfiled_chats", null);
        String where = "deleted = 0 OR _id = 4";
        Assert.assertSame(where, UnfiledChats.filterSelection(custom, where));
    }

    private LinearLayout emptyView() {
        Context application = RuntimeEnvironment.getApplication();
        Resources resources = new Resources(application.getAssets(), application.getResources().getDisplayMetrics(),
                application.getResources().getConfiguration()) {
            @Override
            public int getIdentifier(String name, String type, String packageName) {
                switch (name) {
                    case "folders_empty_add_chats_button":
                        return ADD_CHATS_ID;
                    case "folders_empty_title":
                        return TITLE_ID;
                    case "folders_empty_subtitle":
                        return SUBTITLE_ID;
                    default:
                        return 0;
                }
            }
        };
        Context context = new ContextWrapper(application) {
            @Override
            public Resources getResources() {
                return resources;
            }
        };
        LinearLayout root = new LinearLayout(context);
        View addChats = new View(context);
        addChats.setId(ADD_CHATS_ID);
        root.addView(addChats);
        TextView title = new TextView(context);
        title.setId(TITLE_ID);
        root.addView(title);
        TextView subtitle = new TextView(context);
        subtitle.setId(SUBTITLE_ID);
        root.addView(subtitle);
        return root;
    }

    private List<Long> readChats(String selection) {
        Object owner = UNFILED.equals(selection) ? UNFILED_QUERY : ALL_QUERY;
        String where = "(" + selection + ") AND deleted = 0 ORDER BY _id";
        String query = "SELECT _id FROM conversations WHERE " + UnfiledChats.filterSelection(owner, where);
        List<Long> ids = new ArrayList<>();
        try (Cursor chats = this.database.rawQuery(query, null)) {
            while (chats.moveToNext()) {
                ids.add(chats.getLong(0));
            }
        }
        return ids;
    }

    @Implements(UnfiledChats.class)
    public static final class HostFolderAccess {

        @Implementation
        public static Object createTab() {
            return TAB;
        }

        @Implementation
        public static String folderId(Object folder) {
            return (String) folder;
        }

    }

    @Implements(AppliedPatches.class)
    public static final class AppliedUnfiledChats {

        @Implementation
        public static boolean unfiledChats() {
            return true;
        }

    }

}
