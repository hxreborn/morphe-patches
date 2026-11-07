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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, shadows = AiPreferencesTest.EnabledSettings.class,
        instrumentedPackages = "app.hxreborn.extension.viber")
public class AiPreferencesTest {

    private static final long CHATGPT_ASSISTANT_FLAG = 1L << 29;

    private static final long AI_ASSISTANT_FLAG = 1L << 22;

    private static final long NEWS_ASSISTANT_FLAG = 1L << 31;

    @After
    public void resetSetting() {
        EnabledSettings.enabled = true;
    }

    @Test
    public void redirectsPreferenceKeyWhenEnabled() {
        Assert.assertEquals(PatchSettings.DISABLED_KEY_PREFIX + "some_key", AiPreferences.preferenceKey("some_key"));
    }

    @Test
    public void hidesFeatureWhenEnabled() {
        Assert.assertFalse(AiPreferences.showImageRemix(true));
        Assert.assertFalse(AiPreferences.showFeature(true));
    }

    @Test
    public void marksBannerDismissedWhenEnabled() {
        Assert.assertTrue(AiPreferences.markDismissed(false));
    }

    @Test
    public void preservesHostValuesWhenDisabled() {
        EnabledSettings.enabled = false;
        Assert.assertEquals("some_key", AiPreferences.preferenceKey("some_key"));
        Assert.assertTrue(AiPreferences.showImageRemix(true));
        Assert.assertFalse(AiPreferences.showImageRemix(false));
        Assert.assertTrue(AiPreferences.showFeature(true));
        Assert.assertFalse(AiPreferences.showFeature(false));
        Assert.assertFalse(AiPreferences.markDismissed(false));
        Assert.assertTrue(AiPreferences.markDismissed(true));
    }

    @Test
    public void excludesOnlyChatGptConversations() {
        try (SQLiteDatabase database = SQLiteDatabase.create(null)) {
            database.execSQL("CREATE TABLE conversations (_id INTEGER PRIMARY KEY, deleted INTEGER, flags2 INTEGER)");
            database.execSQL(
                    "INSERT INTO conversations VALUES (1, 0, ?), (2, 0, 0), (3, 0, NULL), "
                            + "(4, 0, ?), (5, 1, 0), (6, 0, ?)",
                    new Object[] { CHATGPT_ASSISTANT_FLAG | AI_ASSISTANT_FLAG, NEWS_ASSISTANT_FLAG | AI_ASSISTANT_FLAG,
                            CHATGPT_ASSISTANT_FLAG });
            String selection = AiPreferences.filterSelection("conversations.deleted = 0 AND ") + "1";
            try (Cursor cursor = database.rawQuery("SELECT _id FROM conversations WHERE " + selection + " ORDER BY _id",
                    null)) {
                Assert.assertEquals(3, cursor.getCount());
                Assert.assertTrue(cursor.moveToFirst());
                Assert.assertEquals(2, cursor.getInt(0));
                Assert.assertTrue(cursor.moveToNext());
                Assert.assertEquals(3, cursor.getInt(0));
                Assert.assertTrue(cursor.moveToNext());
                Assert.assertEquals(4, cursor.getInt(0));
            }
            try (Cursor cursor = database.rawQuery("SELECT count(*) FROM conversations", null)) {
                Assert.assertTrue(cursor.moveToFirst());
                Assert.assertEquals(6, cursor.getInt(0));
            }
        }
    }

    @Test
    public void preservesSelectionWhenDisabled() {
        EnabledSettings.enabled = false;
        Assert.assertEquals("conversations.deleted = 0 AND ",
                AiPreferences.filterSelection("conversations.deleted = 0 AND "));
    }

    @Test
    public void preservesNullSelection() {
        Assert.assertNull(AiPreferences.filterSelection(null));
    }

    @Implements(value = PatchSettings.class, isInAndroidSdk = false)
    public static class EnabledSettings {

        static boolean enabled = true;

        @Implementation
        public static boolean disableAiFeatures() {
            return enabled;
        }

    }

}
