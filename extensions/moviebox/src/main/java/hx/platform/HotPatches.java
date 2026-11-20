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
package hx.platform;

import java.io.File;

import android.content.Context;

public final class HotPatches {

    private static final String PATCH_DIRECTORY = "tinker";

    private HotPatches() {
    }

    public static boolean removeApplied(Context context) {
        File patches = new File(context.getApplicationInfo().dataDir, PATCH_DIRECTORY);
        return patches.exists() && delete(patches);
    }

    private static boolean delete(File file) {
        File[] children = file.listFiles();
        boolean deleted = true;
        if (children != null) {
            for (File child : children) {
                deleted &= delete(child);
            }
        }
        return file.delete() && deleted;
    }

}
