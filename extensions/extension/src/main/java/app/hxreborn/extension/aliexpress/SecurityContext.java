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
package app.hxreborn.extension.aliexpress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.util.Log;

@SuppressWarnings("unused")
public final class SecurityContext extends ContextWrapper {

    private static final String ARCHIVE_NAME = "hx_aliexpress_security.apk";

    private static final int COPY_BUFFER_BYTES = 8192;

    private final File archive;

    private SecurityContext(Context base, File archive) {
        super(base);
        this.archive = archive;
    }

    public static Context wrap(Context base) {
        if (base instanceof SecurityContext) {
            return base;
        }
        try {
            return new SecurityContext(base, extractArchive(base));
        } catch (IOException ex) {
            Log.e(PatchSettings.LOG_TAG, "Could not extract signing data", ex);
            return base;
        }
    }

    private static File extractArchive(Context base) throws IOException {
        File destination = new File(base.getFilesDir(), ARCHIVE_NAME);
        File temporary = File.createTempFile("hxAliExpress", ".apk", base.getFilesDir());
        try {
            try (InputStream source = base.getAssets().open(ARCHIVE_NAME);
                    FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[COPY_BUFFER_BYTES];
                int length;
                while ((length = source.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Could not replace signing data");
            }
            return destination;
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Could not remove temporary signing data");
            }
        }
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        ApplicationInfo application = new ApplicationInfo(super.getApplicationInfo());
        application.sourceDir = this.archive.getAbsolutePath();
        application.publicSourceDir = application.sourceDir;
        return application;
    }

    @Override
    public String getPackageCodePath() {
        return this.archive.getAbsolutePath();
    }

}
