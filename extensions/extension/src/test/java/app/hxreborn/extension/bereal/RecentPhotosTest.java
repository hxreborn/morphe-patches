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
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertArrayEquals;

import android.Manifest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class RecentPhotosTest {

    @Test
    @Config(sdk = 34)
    public void asksForFullAndSelectedPhotoAccessOnAndroid14() {
        assertArrayEquals(new String[] { "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" }, RecentPhotos.permissions());
    }

    @Test
    @Config(sdk = 33)
    public void asksForImageAccessOnAndroid13() {
        assertArrayEquals(new String[] { "android.permission.READ_MEDIA_IMAGES" }, RecentPhotos.permissions());
    }

    @Test
    @Config(sdk = 28)
    public void asksForStorageAccessBelowAndroid13() {
        assertArrayEquals(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, RecentPhotos.permissions());
    }

}
