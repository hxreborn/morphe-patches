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
package app.hxreborn.extension.citra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

@SuppressWarnings("unused")
public final class AzaharNote {

    private static final String TAG = "hxCitra";

    private static final String PREFERENCES = "hx_citra_patches";

    private static final String SHOWN_KEY = "azahar_note_shown";

    private static final String TITLE = "A note from hxreborn";

    private static final String MESSAGE = "This app is Azahar, the free open-source 3DS emulator, "
            + "repackaged with ads and a Play license check. "
            + "Azahar's own build is newer, has no ads and needs no patching.";

    private static final String GET_AZAHAR_LABEL = "Get Azahar";

    private static final String OK_LABEL = "OK";

    private static final String RELEASES_URL = "https://github.com/azahar-emu/azahar/releases/latest";

    private AzaharNote() {

    }

    public static void showOnce(Activity activity) {
        final SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (preferences.getBoolean(SHOWN_KEY, false) || activity.isFinishing()) {
            return;
        }
        new AlertDialog.Builder(activity).setTitle(TITLE)
            .setMessage(MESSAGE)
            .setPositiveButton(GET_AZAHAR_LABEL, (dialog, which) -> openReleases(activity))
            .setNegativeButton(OK_LABEL, null)
            .setOnDismissListener((dialog) -> preferences.edit().putBoolean(SHOWN_KEY, true).apply())
            .show();
    }

    private static void openReleases(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "No browser to open the Azahar release page", ex);
        }
    }

}
