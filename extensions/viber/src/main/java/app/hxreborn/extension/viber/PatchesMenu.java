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
package app.hxreborn.extension.viber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

@SuppressWarnings("unused")
public final class PatchesMenu {

    private static final String ENTRY_RESOURCE = "hx_viber_patches";

    private final Context context;

    public PatchesMenu(Context context) {
        this.context = context;
    }

    public Object createItem() {
        return buildItem(this.context, resourceId(this.context, "id", ENTRY_RESOURCE),
                resourceId(this.context, "string", ENTRY_RESOURCE),
                resourceId(this.context, "drawable", "hx_viber_more_patches"));
    }

    private static Object buildItem(Context context, int id, int title, int icon) {
        return null;
    }

    public static boolean openSettings(Activity activity, int id) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        int entryId = resourceId(activity, "id", ENTRY_RESOURCE);
        if (entryId == 0 || id != entryId) {
            return false;
        }
        activity.startActivity(new Intent(activity, PatchesSettingsActivity.class));
        return true;
    }

    private static int resourceId(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

}
