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
@Config(manifest = Config.NONE, sdk = 28, shadows = { RemoveDeletedMessageNoticesTest.AppliedDeletedMessages.class })
public final class RemoveDeletedMessageNoticesTest {

    private static final int NOTICE_MIME = 1008;

    private static final int SILENT_MIME = 1007;

    @Before
    public void prepare() {
        PatchSettings.initialize(RuntimeEnvironment.getApplication());
        PatchSettings.resetToDefaults();
    }

    @After
    public void reset() {
        PatchSettings.resetToDefaults();
    }

    @Test
    public void removeNoticesByDefault() {
        Assert.assertTrue(PatchSettings.removeDeletedMessageNotices());
        Assert.assertEquals(SILENT_MIME, PatchSettings.deletedMessageMime(NOTICE_MIME));
        Assert.assertEquals("", PatchSettings.deletedMessageBody("Message deleted"));
        Assert.assertFalse(PatchSettings.hasStatusUnlessRemovingNotices(true));
        Assert.assertFalse(PatchSettings.showDeleteWithoutTraceOption(true));
    }

    @Test
    public void keepNoticesWhenSwitchedOff() {
        PatchSettings.setEnabled(PatchSetting.REMOVE_DELETED_MESSAGE_NOTICES.key, false);
        Assert.assertFalse(PatchSettings.removeDeletedMessageNotices());
        Assert.assertEquals(NOTICE_MIME, PatchSettings.deletedMessageMime(NOTICE_MIME));
        Assert.assertEquals("Message deleted", PatchSettings.deletedMessageBody("Message deleted"));
        Assert.assertTrue(PatchSettings.hasStatusUnlessRemovingNotices(true));
        Assert.assertTrue(PatchSettings.showDeleteWithoutTraceOption(true));
    }

    @Test
    public void removeNoticesAgainWhenSwitchedBackOn() {
        PatchSettings.setEnabled(PatchSetting.REMOVE_DELETED_MESSAGE_NOTICES.key, false);
        PatchSettings.setEnabled(PatchSetting.REMOVE_DELETED_MESSAGE_NOTICES.key, true);
        Assert.assertEquals(SILENT_MIME, PatchSettings.deletedMessageMime(NOTICE_MIME));
        Assert.assertEquals("", PatchSettings.deletedMessageBody("Message deleted"));
    }

    @Test
    public void restoreDefaultsRemovesNotices() {
        PatchSettings.setEnabled(PatchSetting.REMOVE_DELETED_MESSAGE_NOTICES.key, false);
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.removeDeletedMessageNotices());
    }

    @Implements(value = AppliedPatches.class, isInAndroidSdk = false)
    public static final class AppliedDeletedMessages {

        @Implementation
        public static boolean removeDeletedMessageNotices() {
            return true;
        }

    }

}
