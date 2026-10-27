/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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
