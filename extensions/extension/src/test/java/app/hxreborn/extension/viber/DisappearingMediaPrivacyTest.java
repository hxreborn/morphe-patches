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

import android.content.ContentValues;
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
@Config(manifest = Config.NONE, sdk = 28, shadows = { DisappearingMediaPrivacyTest.AppliedPrivacy.class,
        DisappearingMediaPrivacyTest.DatabaseUpdate.class })
public final class DisappearingMediaPrivacyTest {

    private SQLiteDatabase database;

    @Before
    public void prepare() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
        this.database = SQLiteDatabase.create(null);
        this.database.execSQL(
                "CREATE TABLE messages (_id INTEGER PRIMARY KEY, timebomb INTEGER, unread INTEGER, sync_read INTEGER, extra_flags INTEGER DEFAULT 0, read_message_time INTEGER DEFAULT 0)");
        this.database.execSQL(
                "INSERT INTO messages (_id,timebomb,unread,sync_read) VALUES (1, 0, 1, 0), (2, 30, 1, 0), (3, 60, 0, 0)");
    }

    @After
    public void close() {
        this.database.close();
        PatchSettings.resetToDefaults();
    }

    @Test
    public void preserveReportsByDefault() {
        Assert.assertFalse(PatchSettings.disappearingMediaPrivacy());
        Assert.assertFalse(DisappearingMediaPrivacy.blockScreenshot(2));
        Assert.assertEquals(2, markUnreadRows());
        assertState(1, 0, 1);
        assertState(2, 0, 1);
    }

    @Test
    public void suppressOnlyScreenshotEvents() {
        enable();
        Assert.assertTrue(DisappearingMediaPrivacy.blockScreenshot(2));
        Assert.assertFalse(DisappearingMediaPrivacy.blockScreenshot(0));
        Assert.assertFalse(DisappearingMediaPrivacy.blockScreenshot(1));
        Assert.assertFalse(DisappearingMediaPrivacy.blockScreenshot(3));
    }

    @Test
    public void suppressTimerOffServiceReceiptsWithoutDroppingOrdinaryReceipts() {
        this.database.execSQL("INSERT INTO messages VALUES (4, 0, 1, 0, 134217728, 0)");
        enable();
        Assert.assertEquals(3, markUnreadRows());
        assertState(1, 0, 1);
        assertState(2, 0, 0);
        assertState(4, 0, 0);
    }

    @Test
    public void retainOnlyOrdinaryRowsInInferredReadUpdates() {
        this.database.execSQL("INSERT INTO messages VALUES (4, 0, 1, 0, 134217728, 0)");
        enable();
        ContentValues values = new ContentValues();
        values.put("read_message_time", 99L);
        Assert.assertEquals(1, this.database.update("messages", values,
                DisappearingMediaPrivacy.inferredReadSelection("_id>?"), new String[] { "0" }));
        try (Cursor cursor = this.database.rawQuery("SELECT _id FROM messages WHERE read_message_time>0", null)) {
            Assert.assertTrue(cursor.moveToFirst());
            Assert.assertEquals(1, cursor.getInt(0));
            Assert.assertFalse(cursor.moveToNext());
        }
    }

    @Test
    public void preserveInferredReadUpdatesWhenDisabled() {
        Assert.assertEquals("_id>?", DisappearingMediaPrivacy.inferredReadSelection("_id>?"));
    }

    @Test
    public void retainOrdinaryReceiptsAndLocalDisappearingReadState() {
        enable();
        Assert.assertEquals(2, markUnreadRows());
        assertState(1, 0, 1);
        assertState(2, 0, 0);
        assertState(3, 0, 0);
    }

    @Test
    public void keepSuppressedReceiptsOutOfQueueAfterDisabling() {
        enable();
        markUnreadRows();
        PatchSettings.setEnabled(PatchSetting.DISAPPEARING_MEDIA_PRIVACY.key, false);
        Assert.assertEquals(0, markUnreadRows());
        assertState(2, 0, 0);
        Assert.assertFalse(DisappearingMediaPrivacy.blockScreenshot(2));
    }

    private void enable() {
        PatchSettings.setEnabled(PatchSetting.DISAPPEARING_MEDIA_PRIVACY.key, true);
    }

    private int markUnreadRows() {
        ContentValues values = new ContentValues();
        values.put("unread", 0);
        values.put("sync_read", 1);
        return DisappearingMediaPrivacy.updateReadState(this.database, "messages", values, "unread>?",
                new String[] { "0" });
    }

    private void assertState(int id, int unread, int syncRead) {
        try (Cursor cursor = this.database.rawQuery("SELECT unread, sync_read FROM messages WHERE _id=?",
                new String[] { Integer.toString(id) })) {
            Assert.assertTrue(cursor.moveToFirst());
            Assert.assertEquals(unread, cursor.getInt(0));
            Assert.assertEquals(syncRead, cursor.getInt(1));
        }
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedPrivacy {

        @Implementation
        public static boolean disappearingMediaPrivacy() {
            return true;
        }

    }

    @Implements(value = DisappearingMediaPrivacy.class, isInAndroidSdk = false)
    public static final class DatabaseUpdate {

        @Implementation
        protected static int update(Object database, String table, ContentValues values, String selection,
                String[] arguments) {
            return ((SQLiteDatabase) database).update(table, values, selection, arguments);
        }

    }

}
