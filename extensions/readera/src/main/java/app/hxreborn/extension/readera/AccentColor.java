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
package app.hxreborn.extension.readera;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import app.hxreborn.extension.shared.ColorEntries;
import app.hxreborn.extension.shared.ColorResourceTable;

@SuppressWarnings("unused")
public final class AccentColor {

    static final String TITLE = "Accent color";

    private static final String TABLE_PREFIX = "hx_readera_accent_";

    private static final String TABLE_SUFFIX = ".apk";

    private static final int TABLE_FORMAT_VERSION = 1;

    private static final int NO_RESOURCE = 0;

    private static File tableApk;

    private static int accent;

    private static int stockAccent;

    private static Method addAssetPathMethod;

    private static boolean failureLogged;

    private AccentColor() {
    }

    public static void applyToApplication(Context base) {
        int resourceId = accentColorId();
        if (resourceId == NO_RESOURCE) {
            return;
        }
        Resources resources = base.getResources();
        try {
            stockAccent = readColor(resources, resourceId);
            String preset = AccentPalette.getPreset(base);
            File cacheDirectory = base.getCacheDir();
            if (AccentPalette.STOCK.equals(preset)) {
                deleteTableFiles(cacheDirectory, null);
                return;
            }
            accent = AccentPalette.color(base, preset);
            tableApk = new File(cacheDirectory, String.format(Locale.ROOT, "%sv%d_%08X_%08X%s", TABLE_PREFIX,
                    TABLE_FORMAT_VERSION, resourceId, accent, TABLE_SUFFIX));
            writeTableApk(base.getPackageName(), resources, resourceId);
            deleteTableFiles(cacheDirectory, tableApk);
        } catch (IOException | RuntimeException ex) {
            logFirstFailure("Cannot write the accent color table", ex);
            tableApk = null;
            return;
        }
        if (!attachTableApk(base)) {
            tableApk = null;
        }
    }

    public static void applyToActivity(Context activity) {
        if (tableApk != null) {
            attachTableApk(activity);
        }
    }

    public static int jumpBarColor(int stockColor) {
        return (tableApk != null) ? accent : stockColor;
    }

    static int stockAccent() {
        return stockAccent;
    }

    private static int accentColorId() {
        return NO_RESOURCE;
    }

    private static boolean attachTableApk(Context context) {
        Resources resources = context.getResources();
        int resourceId = accentColorId();
        try {
            if (!tableApk.isFile()) {
                writeTableApk(context.getPackageName(), resources, resourceId);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                TableLoader.addTo(resources, tableApk);
            } else {
                addAssetPath(resources.getAssets(), tableApk);
            }
            int resolved = readColor(resources, resourceId);
            if (resolved != accent) {
                logFirstFailure(
                        String.format(Locale.ROOT, "Accent color resolved to %08X, expected %08X", resolved, accent),
                        null);
                return false;
            }
            return true;
        } catch (IOException | ReflectiveOperationException | RuntimeException ex) {
            logFirstFailure("Cannot attach the accent color table", ex);
            return false;
        }
    }

    private static void writeTableApk(String packageName, Resources resources, int resourceId) throws IOException {
        File directory = tableApk.getParentFile();
        if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create " + directory.getName());
        }
        ColorEntries entries = new ColorEntries();
        entries.add(resourceId, resources.getResourceEntryName(resourceId), accent);
        ColorResourceTable.writeApk(tableApk, packageName, resources, entries);
    }

    private static void deleteTableFiles(File directory, File kept) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            boolean tableFile = name.startsWith(TABLE_PREFIX)
                    && (name.endsWith(TABLE_SUFFIX) || name.endsWith(ColorResourceTable.TEMPORARY_SUFFIX));
            if (tableFile && !file.equals(kept) && !file.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Cannot delete the old accent color table " + name);
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void addAssetPath(AssetManager assets, File file) throws ReflectiveOperationException {
        if (addAssetPathMethod == null) {
            addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
        }
        Object cookie = addAssetPathMethod.invoke(assets, file.getAbsolutePath());
        if (!(cookie instanceof Integer) || (Integer) cookie == 0) {
            throw new IllegalStateException("AssetManager.addAssetPath rejected the accent color table");
        }
    }

    @SuppressWarnings("deprecation")
    private static int readColor(Resources resources, int resourceId) {
        return resources.getColor(resourceId);
    }

    private static void logFirstFailure(String message, Throwable cause) {
        if (!failureLogged) {
            failureLogged = true;
            Log.e(PatchSettings.LOG_TAG, message, cause);
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    @SuppressLint("UseRequiresApi")
    private static final class TableLoader {

        private static ResourcesLoader loader;

        private TableLoader() {
        }

        static void addTo(Resources resources, File file) throws IOException {
            if (loader == null) {
                try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY)) {
                    ResourcesLoader created = new ResourcesLoader();
                    created.addProvider(ResourcesProvider.loadFromApk(descriptor));
                    loader = created;
                }
            }
            resources.addLoaders(loader);
        }

    }

}
