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
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowArscAssetManager10;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public final class SecurityContextTest {

    private static final String TAG = "SecurityContext";

    private static final String ARCHIVE_NAME = "hx_security.apk";

    private static final byte[] ARCHIVE_BYTES = "signing archive".getBytes(StandardCharsets.UTF_8);

    private static File archiveIn(Context base) {
        return new File(base.getFilesDir(), ARCHIVE_NAME);
    }

    @Test
    public void returnsTheBaseContextWhenTheSigningDataIsMissing() {
        Context base = RuntimeEnvironment.getApplication();

        assertSame(base, SecurityContext.wrap(base));
    }

    @Test
    public void logsOneErrorWhenTheSigningDataIsMissing() {
        SecurityContext.wrap(RuntimeEnvironment.getApplication());

        List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag(TAG);
        assertEquals(1, logs.size());
        assertEquals(Log.ERROR, logs.get(0).type);
    }

    @Test
    public void leavesNoTemporaryFileWhenTheSigningDataIsMissing() {
        Context base = RuntimeEnvironment.getApplication();

        SecurityContext.wrap(base);

        assertArrayEquals(new String[0], base.getFilesDir().list());
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void extractsTheSigningArchiveIntoTheFilesDirectory() throws IOException {
        Context base = RuntimeEnvironment.getApplication();

        Context wrapped = SecurityContext.wrap(base);

        assertNotSame(base, wrapped);
        assertArrayEquals(ARCHIVE_BYTES, Files.readAllBytes(archiveIn(base).toPath()));
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void leavesOnlyTheArchiveInTheFilesDirectory() {
        Context base = RuntimeEnvironment.getApplication();

        SecurityContext.wrap(base);

        assertArrayEquals(new String[] { ARCHIVE_NAME }, base.getFilesDir().list());
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void replacesAnEarlierArchive() throws IOException {
        Context base = RuntimeEnvironment.getApplication();
        Files.write(archiveIn(base).toPath(), new byte[] { 9, 9 });

        SecurityContext.wrap(base);

        assertArrayEquals(ARCHIVE_BYTES, Files.readAllBytes(archiveIn(base).toPath()));
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void pointsThePackageCodePathAtTheExtractedArchive() {
        Context base = RuntimeEnvironment.getApplication();

        Context wrapped = SecurityContext.wrap(base);

        assertEquals(archiveIn(base).getAbsolutePath(), wrapped.getPackageCodePath());
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void pointsBothApplicationSourceDirectoriesAtTheExtractedArchive() {
        Context base = RuntimeEnvironment.getApplication();

        Context wrapped = SecurityContext.wrap(base);

        String archivePath = archiveIn(base).getAbsolutePath();
        assertEquals(archivePath, wrapped.getApplicationInfo().sourceDir);
        assertEquals(archivePath, wrapped.getApplicationInfo().publicSourceDir);
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void leavesTheBaseApplicationInfoUnchanged() {
        Context base = RuntimeEnvironment.getApplication();
        String sourceDirBefore = base.getApplicationInfo().sourceDir;

        SecurityContext.wrap(base).getApplicationInfo();

        assertEquals(sourceDirBefore, base.getApplicationInfo().sourceDir);
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void returnsItselfAsTheApplicationContext() {
        Context wrapped = SecurityContext.wrap(RuntimeEnvironment.getApplication());

        assertSame(wrapped, wrapped.getApplicationContext());
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void returnsTheSameContextWhenWrappedTwice() {
        Context wrapped = SecurityContext.wrap(RuntimeEnvironment.getApplication());

        assertSame(wrapped, SecurityContext.wrap(wrapped));
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void returnsTheBaseContextWhenTheArchiveCannotBeReplaced() {
        Context base = RuntimeEnvironment.getApplication();
        assertTrue(archiveIn(base).mkdir());

        assertSame(base, SecurityContext.wrap(base));
    }

    @Test
    @Config(sdk = 31, shadows = SigningArchiveAssets.class)
    public void leavesNoTemporaryFileWhenTheArchiveCannotBeReplaced() {
        Context base = RuntimeEnvironment.getApplication();
        assertTrue(archiveIn(base).mkdir());

        SecurityContext.wrap(base);

        assertArrayEquals(new String[] { ARCHIVE_NAME }, base.getFilesDir().list());
    }

    @Implements(AssetManager.class)
    public static class SigningArchiveAssets extends ShadowArscAssetManager10 {

        @Implementation
        protected InputStream open(String name) throws IOException {
            if (!ARCHIVE_NAME.equals(name)) {
                throw new FileNotFoundException(name);
            }
            return new ByteArrayInputStream(ARCHIVE_BYTES);
        }

    }

}
