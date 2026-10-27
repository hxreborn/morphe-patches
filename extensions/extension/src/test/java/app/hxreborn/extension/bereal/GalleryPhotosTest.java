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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GalleryPhotosTest {

    private Context context;

    @Before
    public void enableGallery() {
        this.context = RuntimeEnvironment.getApplication();
        Utils.setContext(this.context);
        PatchSettings.GALLERY_PHOTOS.save(true);
    }

    @After
    public void restoreDefaults() {
        GalleryPhotos.clear(this.context);
        PatchSettings.GALLERY_PHOTOS.save(false);
    }

    @Test
    public void keepsTheCameraImageWhenTheSettingIsOff() throws IOException {
        // given
        givenStagedPhotos();
        PatchSettings.GALLERY_PHOTOS.save(false);

        // when
        final Object replaced = GalleryPhotos.replace(false);

        // then
        assertNull(replaced);
    }

    @Test
    public void keepsTheCameraImageForASideWithoutAGalleryPhoto() throws IOException {
        // given
        givenStagedPhotos();
        GalleryPhotos.file(this.context, true).delete();

        // when
        final Object replaced = GalleryPhotos.replace(true);

        // then
        assertNull(replaced);
    }

    @Test
    public void keepsTheStagedPhotosAfterOneCapture() throws IOException {
        // given
        givenStagedPhotos();

        // when
        GalleryPhotos.replace(true);

        // then
        assertTrue(GalleryPhotos.file(this.context, false).isFile());
        assertTrue(GalleryPhotos.file(this.context, true).isFile());
    }

    @Test
    public void clearsTheStagedPhotosAfterBothCaptures() throws IOException {
        // given
        givenStagedPhotos();
        GalleryPhotos.replace(true);

        // when
        GalleryPhotos.replace(false);

        // then
        assertFalse(GalleryPhotos.file(this.context, false).isFile());
        assertFalse(GalleryPhotos.file(this.context, true).isFile());
    }

    @Test
    public void clearsTheStagedPhotoAfterBothCapturesWhenOnlyOneSideIsPicked() throws IOException {
        // given
        givenStagedPhotos();
        GalleryPhotos.file(this.context, true).delete();
        GalleryPhotos.replace(false);

        // when
        GalleryPhotos.replace(true);

        // then
        assertFalse(GalleryPhotos.file(this.context, false).isFile());
    }

    private void givenStagedPhotos() throws IOException {
        GalleryPhotos.directory(this.context).mkdirs();
        for (boolean front : new boolean[] { false, true }) {
            try (FileOutputStream output = new FileOutputStream(GalleryPhotos.file(this.context, front))) {
                output.write(new byte[] { 1, 2, 3, 4 });
            }
        }
    }

}
