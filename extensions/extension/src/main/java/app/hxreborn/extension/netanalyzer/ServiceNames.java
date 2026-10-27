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
package app.hxreborn.extension.netanalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

final class ServiceNames {

    private static final String RESOURCE = "port_database";

    private static final int COPY_BUFFER_SIZE = 16384;

    private final SQLiteDatabase database;

    private ServiceNames(SQLiteDatabase database) {
        this.database = database;
    }

    static ServiceNames open(Context context) {
        try {
            final int id = context.getResources().getIdentifier(RESOURCE, "raw", context.getPackageName());
            if (id == 0) {
                return new ServiceNames(null);
            }
            final long stamp = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
            final File target = new File(context.getCacheDir(), RESOURCE + "_" + stamp + ".db");
            if (!target.exists()) {
                copy(context, id, target);
            }
            return new ServiceNames(SQLiteDatabase.openDatabase(target.getPath(), null, SQLiteDatabase.OPEN_READONLY));
        } catch (PackageManager.NameNotFoundException | IOException | RuntimeException ex) {
            return new ServiceNames(null);
        }
    }

    String application(int port) {
        if (this.database == null) {
            return null;
        }
        try (Cursor cursor = this.database.rawQuery("SELECT protocol FROM ports WHERE port=?",
                new String[] { String.valueOf(port) })) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    String describe(String application) {
        if (this.database == null) {
            return "";
        }
        try (Cursor cursor = this.database.rawQuery("SELECT description FROM names WHERE protocol=?",
                new String[] { application })) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } catch (RuntimeException ex) {
            return "";
        }
    }

    void close() {
        if (this.database != null) {
            this.database.close();
        }
    }

    private static void copy(Context context, int id, File target) throws IOException {
        final File partial = new File(target.getPath() + ".part");
        try (InputStream in = context.getResources().openRawResource(id);
                OutputStream out = new FileOutputStream(partial)) {
            final byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        if (!partial.renameTo(target)) {
            throw new IOException("Cannot rename " + partial + " to " + target);
        }
    }

}
