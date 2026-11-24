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
package app.hxreborn.extension.outlook.theme;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import app.hxreborn.extension.outlook.settings.PatchSettings;
import app.hxreborn.extension.shared.ColorEntries;
import app.hxreborn.extension.shared.ColorResourceTable;

@TargetApi(Build.VERSION_CODES.R)
final class ThemeResources {

    private static final String TABLE_PREFIX = "hx_outlook_theme_";

    private static final String TABLE_SUFFIX = ".apk";

    private static final int TABLE_FORMAT_VERSION = 1;

    private static final ResourcesLoader LOADER = new ResourcesLoader();

    private static Integer loadedColor;

    private ThemeResources() {
    }

    static synchronized void attach(Context context, int pickedColor, int referenceColorId, int[] colorPairs) {
        if (loadedColor == null || loadedColor != pickedColor) {
            try {
                loadTable(context, pickedColor, referenceColorId, colorPairs);
            } catch (IOException | RuntimeException ex) {
                Log.e(PatchSettings.LOG_TAG, "Cannot load the custom theme colors", ex);
                return;
            }
            loadedColor = pickedColor;
        }
        context.getResources().addLoaders(LOADER);
    }

    private static void loadTable(Context context, int pickedColor, int referenceColorId, int[] colorPairs)
            throws IOException {
        File directory = context.getCacheDir();
        File table = new File(directory, String.format(Locale.ROOT, "%sv%d_%08X%s", TABLE_PREFIX, TABLE_FORMAT_VERSION,
                pickedColor, TABLE_SUFFIX));
        if (!table.isFile()) {
            Resources resources = context.getResources();
            ColorResourceTable.writeApk(table, context.getPackageName(), resources,
                    entries(resources, pickedColor, referenceColorId, colorPairs));
        }
        try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(table, ParcelFileDescriptor.MODE_READ_ONLY)) {
            LOADER.setProviders(Collections.singletonList(ResourcesProvider.loadFromApk(descriptor)));
        }
        deleteOtherTables(directory, table);
    }

    private static ColorEntries entries(Resources resources, int pickedColor, int referenceColorId, int[] colorPairs) {
        ThemeColorRamp ramp = ThemeColorRamp.between(resources.getColor(referenceColorId, null), pickedColor);
        ColorEntries entries = new ColorEntries();
        for (int index = 0; index + 1 < colorPairs.length; index += 2) {
            int stockId = colorPairs[index];
            int customId = colorPairs[index + 1];
            entries.add(customId, resources.getResourceEntryName(customId),
                    ramp.transform(resources.getColor(stockId, null)));
        }
        return entries;
    }

    private static void deleteOtherTables(File directory, File kept) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(TABLE_PREFIX) && !file.equals(kept) && !file.delete()) {
                Log.w(PatchSettings.LOG_TAG, "Cannot delete the old custom theme table " + name);
            }
        }
    }

}
