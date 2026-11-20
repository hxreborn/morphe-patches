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
package app.hxreborn.extension.terabox;

import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import app.morphe.extension.shared.Utils;

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
@Config(sdk = { 23, 36 }, shadows = { EveryPatchApplied.class, BackupCountTest.PendingFilesQuery.class })
public final class BackupCountTest {

    private static final String ACTIVE_SELECTION = "state = 104 OR state = 100";

    private Locale originalLocale;

    @Before
    public void setContext() {
        this.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Utils.setContext(RuntimeEnvironment.getApplication());
        PendingFilesQuery.cursor = null;
        PendingFilesQuery.failure = null;
        PendingFilesQuery.calls = 0;
    }

    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        Locale.setDefault(this.originalLocale);
    }

    @Test
    public void includesPausedRowsWithActiveRows() {
        try (SQLiteDatabase database = SQLiteDatabase.create(null)) {
            database.execSQL("CREATE TABLE inprogress (state INTEGER)");
            database.execSQL("INSERT INTO inprogress VALUES (100), (104), (105), (110)");
            Assert.assertEquals(3, countRows(database, BackupCount.selection()));
            PatchSettings.setEnabled(AppliedPatches.SHOW_BACKUP_COUNT, false);
            Assert.assertEquals(2, countRows(database, BackupCount.selection()));
        }
    }

    @Test
    public void showsRemainingFilesInBackupTitles() {
        Assert.assertEquals("Resume backup (22 left)", BackupCount.withRemaining("Resume backup", 22));
        Assert.assertEquals("Waiting for Wi-Fi (1 left)", BackupCount.withRemaining("Waiting for Wi-Fi", 1));
        Assert.assertEquals("Backing up (1,024 left)", BackupCount.withRemaining("Backing up", 1024));
    }

    @Test
    public void closesCursorAfterCountingPendingFiles() {
        try (SQLiteDatabase database = SQLiteDatabase.create(null)) {
            database.execSQL("CREATE TABLE inprogress (state INTEGER)");
            database.execSQL("INSERT INTO inprogress VALUES (100), (104), (105), (110)");
            PendingFilesQuery.cursor = database
                .rawQuery("SELECT state FROM inprogress WHERE " + BackupCount.selection(), null);
            Assert.assertEquals(3, BackupCount.withRemainingCount(99));
            Assert.assertTrue(PendingFilesQuery.cursor.isClosed());
        }
    }

    @Test
    public void keepsCurrentCountWithoutPendingCursor() {
        Assert.assertEquals(7, BackupCount.withRemainingCount(7));
        Assert.assertEquals(1, PendingFilesQuery.calls);
    }

    @Test
    public void keepsCurrentCountWhenPendingQueryFails() {
        PendingFilesQuery.failure = new SQLiteException("Failed test query");
        Assert.assertEquals(7, BackupCount.withRemainingCount(7));
        Assert.assertEquals(1, PendingFilesQuery.calls);
    }

    @Test
    public void skipsPendingQueryWhenDisabled() {
        PatchSettings.setEnabled(AppliedPatches.SHOW_BACKUP_COUNT, false);
        Assert.assertEquals(7, BackupCount.withRemainingCount(7));
        Assert.assertEquals(0, PendingFilesQuery.calls);
    }

    @Test
    public void refreshesTitlesOnlyWhenTheirRemainingCountChanges() {
        Object first = new Object();
        Object second = new Object();
        Assert.assertTrue(BackupCount.shouldRefreshTitle(first, 22));
        Assert.assertFalse(BackupCount.shouldRefreshTitle(first, 22));
        Assert.assertTrue(BackupCount.shouldRefreshTitle(first, 21));
        Assert.assertTrue(BackupCount.shouldRefreshTitle(second, 21));
        PatchSettings.setEnabled(AppliedPatches.SHOW_BACKUP_COUNT, false);
        Assert.assertFalse(BackupCount.shouldRefreshTitle(first, 21));
        PatchSettings.resetToDefaults();
        Assert.assertTrue(BackupCount.shouldRefreshTitle(first, 21));
    }

    @Test
    public void keepsStockTitlesWithoutPendingFiles() {
        Assert.assertEquals("Backup completed", BackupCount.withRemaining("Backup completed", 0));
        Assert.assertEquals("Backup is off", BackupCount.withRemaining("Backup is off", -1));
        Assert.assertNull(BackupCount.withRemaining(null, 22));
    }

    @Test
    public void restoresStockCountsWhenDisabled() {
        PatchSettings.setEnabled(AppliedPatches.SHOW_BACKUP_COUNT, false);
        Assert.assertEquals(ACTIVE_SELECTION, BackupCount.selection());
        Assert.assertEquals("Resume backup", BackupCount.withRemaining("Resume backup", 22));
        PatchSettings.resetToDefaults();
        Assert.assertEquals("Resume backup (22 left)", BackupCount.withRemaining("Resume backup", 22));
    }

    private static int countRows(SQLiteDatabase database, String selection) {
        try (Cursor cursor = database.rawQuery("SELECT count(*) FROM inprogress WHERE " + selection, null)) {
            Assert.assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    @Implements(BackupCount.class)
    public static final class PendingFilesQuery {

        private static Cursor cursor;

        private static RuntimeException failure;

        private static int calls;

        @Implementation
        protected static Cursor queryPendingFiles() {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return cursor;
        }

    }

}
