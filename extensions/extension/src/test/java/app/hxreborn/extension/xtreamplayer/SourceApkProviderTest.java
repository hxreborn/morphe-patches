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
package app.hxreborn.extension.xtreamplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
public final class SourceApkProviderTest {

    private static final String APK_CONTENT = "installed apk contents";

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private Context context;

    private String originalSource;

    private File source;

    private SourceApkProvider provider;

    @Before
    public void attachProviderToInstalledApk() throws Exception {
        this.context = RuntimeEnvironment.getApplication();
        this.originalSource = this.context.getApplicationInfo().sourceDir;
        this.source = this.folder.newFile("source.apk");
        try (FileOutputStream output = new FileOutputStream(this.source)) {
            output.write(APK_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
        this.context.getApplicationInfo().sourceDir = this.source.getAbsolutePath();
        ProviderInfo information = new ProviderInfo();
        information.authority = SourceApkProvider.uri(this.context).getAuthority();
        information.exported = false;
        information.grantUriPermissions = true;
        this.provider = new SourceApkProvider();
        this.provider.attachInfo(this.context, information);
    }

    @After
    public void restoreInstalledApkPath() {
        this.context.getApplicationInfo().sourceDir = this.originalSource;
    }

    @Test
    public void readsInstalledApkWithoutCopyingIt() throws Exception {
        try (ParcelFileDescriptor descriptor = this.provider.openFile(SourceApkProvider.uri(this.context), "r");
                BufferedReader input = new BufferedReader(new InputStreamReader(
                        new ParcelFileDescriptor.AutoCloseInputStream(descriptor), StandardCharsets.UTF_8))) {
            Assert.assertEquals(APK_CONTENT, input.readLine());
        }
    }

    @Test
    public void returnsRequestedSizeColumn() {
        try (Cursor cursor = this.provider.query(SourceApkProvider.uri(this.context),
                new String[] { OpenableColumns.SIZE }, null, null, null)) {
            Assert.assertArrayEquals(new String[] { OpenableColumns.SIZE }, cursor.getColumnNames());
            Assert.assertTrue(cursor.moveToFirst());
            Assert.assertEquals(this.source.length(), cursor.getLong(0));
        }
    }

    @Test
    public void rejectsPathOutsideApk() {
        Uri uri = SourceApkProvider.uri(this.context).buildUpon().path("/../files/settings.xml").build();
        Assert.assertThrows(FileNotFoundException.class, () -> this.provider.openFile(uri, "r"));
    }

    @Test
    public void rejectsQueryOnApkPath() {
        Uri uri = SourceApkProvider.uri(this.context).buildUpon().query("download=true").build();
        Assert.assertThrows(FileNotFoundException.class, () -> this.provider.openFile(uri, "r"));
    }

    @Test
    public void rejectsFragmentOnApkPath() {
        Uri uri = SourceApkProvider.uri(this.context).buildUpon().fragment("download").build();
        Assert.assertThrows(FileNotFoundException.class, () -> this.provider.openFile(uri, "r"));
    }

    @Test
    public void rejectsWriteAccess() {
        Assert.assertThrows(FileNotFoundException.class,
                () -> this.provider.openFile(SourceApkProvider.uri(this.context), "rw"));
    }

}
