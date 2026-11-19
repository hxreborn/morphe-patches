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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import app.morphe.extension.shared.Utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 })
public final class OriginalDateSortTest {

    private static final String UPLOAD_TIME_DESCENDING = "isdir DESC, server_mtime DESC";

    private static final String UPLOAD_TIME_ASCENDING = "isdir DESC, server_mtime ASC";

    private static final String NAME_ASCENDING = "isdir DESC, file_name COLLATE LOCALIZED ASC";

    private static final String SIZE_DESCENDING = "isdir DESC, file_size DESC";

    private static final String SECOND_TIME_DESCENDING = "isdir DESC, server_mtime_second DESC";

    private static final long ORIGINAL_TIME = 500;

    @Before
    public void setContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        OriginalDateSort.loadProcessSetting();
    }

    @Test
    public void keepsUploadTimeOrderWhenPatchIsNotApplied() {
        OriginalDateSort.loadProcessSetting();

        Assert.assertEquals(UPLOAD_TIME_DESCENDING, OriginalDateSort.timeSortOrder(UPLOAD_TIME_DESCENDING));
        Assert.assertFalse(OriginalDateSort.usesOriginalTime(ORIGINAL_TIME));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void keepsUploadTimeOrderWhenSwitchIsOff() {
        PatchSettings.setEnabled(AppliedPatches.SORT_BY_ORIGINAL_DATE, false);
        OriginalDateSort.loadProcessSetting();

        Assert.assertEquals(UPLOAD_TIME_DESCENDING, OriginalDateSort.timeSortOrder(UPLOAD_TIME_DESCENDING));
        Assert.assertFalse(OriginalDateSort.usesOriginalTime(ORIGINAL_TIME));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void keepsProcessSettingUntilReloaded() {
        OriginalDateSort.loadProcessSetting();
        PatchSettings.setEnabled(AppliedPatches.SORT_BY_ORIGINAL_DATE, false);

        Assert.assertTrue(OriginalDateSort.sortsByOriginalDate());
        Assert.assertTrue(OriginalDateSort.usesOriginalTime(ORIGINAL_TIME));
        Assert.assertNotEquals(UPLOAD_TIME_DESCENDING, OriginalDateSort.timeSortOrder(UPLOAD_TIME_DESCENDING));

        OriginalDateSort.loadProcessSetting();

        Assert.assertFalse(OriginalDateSort.sortsByOriginalDate());
        Assert.assertFalse(OriginalDateSort.usesOriginalTime(ORIGINAL_TIME));
        Assert.assertEquals(UPLOAD_TIME_DESCENDING, OriginalDateSort.timeSortOrder(UPLOAD_TIME_DESCENDING));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void keepsOrdersOtherThanUploadTime() {
        OriginalDateSort.loadProcessSetting();

        Assert.assertEquals(NAME_ASCENDING, OriginalDateSort.timeSortOrder(NAME_ASCENDING));
        Assert.assertEquals(SIZE_DESCENDING, OriginalDateSort.timeSortOrder(SIZE_DESCENDING));
        Assert.assertEquals(SECOND_TIME_DESCENDING, OriginalDateSort.timeSortOrder(SECOND_TIME_DESCENDING));
        Assert.assertNull(OriginalDateSort.timeSortOrder(null));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void usesOriginalTimeOnlyWhenPositive() {
        OriginalDateSort.loadProcessSetting();

        Assert.assertTrue(OriginalDateSort.usesOriginalTime(1));
        Assert.assertFalse(OriginalDateSort.usesOriginalTime(0));
        Assert.assertFalse(OriginalDateSort.usesOriginalTime(-1));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void ordersMissingZeroAndNegativeOriginalTimesByUploadTime() {
        OriginalDateSort.loadProcessSetting();
        SQLiteDatabase database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE cachefilelist (file_name TEXT, isdir INTEGER, server_mtime INTEGER, "
                + "client_mtime INTEGER)");
        database.execSQL("INSERT INTO cachefilelist VALUES ('original300', 0, 900, 300), ('missing500', 0, 500, NULL), "
                + "('zero400', 0, 400, 0), ('negative450', 0, 450, -1), ('original600', 0, 100, 600), "
                + "('folder', 1, 50, 50)");

        List<String> descending = fileNames(database, OriginalDateSort.timeSortOrder(UPLOAD_TIME_DESCENDING));
        List<String> ascending = fileNames(database, OriginalDateSort.timeSortOrder(UPLOAD_TIME_ASCENDING));
        database.close();

        Assert.assertEquals(
                Arrays.asList("folder", "original600", "missing500", "negative450", "zero400", "original300"),
                descending);
        Assert.assertEquals(
                Arrays.asList("folder", "original300", "zero400", "negative450", "missing500", "original600"),
                ascending);
    }

    private static List<String> fileNames(SQLiteDatabase database, String order) {
        List<String> names = new ArrayList<>();
        try (Cursor cursor = database.query("cachefilelist", new String[] { "file_name" }, null, null, null, null,
                order)) {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        }
        return names;
    }

}
