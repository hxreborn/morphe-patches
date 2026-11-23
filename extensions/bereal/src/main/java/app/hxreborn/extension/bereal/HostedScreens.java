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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Opens the patch screens on mount installs. A mount install keeps the stock manifest, so Android doesn't know
 * the activities the patch adds. Their intents then target a stock activity with the same manifest flags, and
 * {@link PatchesComponentFactory} swaps in the patch activity.
 */
public final class HostedScreens {

    /** Opaque, not exported, standard launch mode, main process. */
    public static final String OPAQUE_HOST = "com.pairip.licensecheck.LicenseActivity";

    /** Theme.Translucent.NoTitleBar, not exported, excluded from recents, main process. */
    public static final String TRANSLUCENT_HOST = "com.google.android.gms.auth.api.signin.internal.SignInHubActivity";

    static final String ACTION_SETTINGS = "app.hxreborn.extension.bereal.SHOW_SETTINGS";

    static final String ACTION_GALLERY_PICKER = "app.hxreborn.extension.bereal.SHOW_GALLERY_PICKER";

    static final String ACTION_RETAKE_COUNT = "app.hxreborn.extension.bereal.SHOW_RETAKE_COUNT";

    private HostedScreens() {
    }

    static Intent intent(Context context, Class<? extends Activity> screen) {
        final Intent intent = new Intent(context, screen);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && context.getPackageManager().resolveActivity(intent, 0) == null) {
            final String action = action(screen);
            if (action != null) {
                intent.setClassName(context, (screen == PatchesSettingsActivity.class) ? OPAQUE_HOST : TRANSLUCENT_HOST)
                        .setAction(action);
            }
        }
        return intent;
    }

    static Activity instantiate(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return null;
        }
        switch (intent.getAction()) {
            case ACTION_SETTINGS:
                return new PatchesSettingsActivity();
            case ACTION_GALLERY_PICKER:
                return new GalleryPickerActivity();
            case ACTION_RETAKE_COUNT:
                return new RetakeCountActivity();
            default:
                return null;
        }
    }

    private static String action(Class<? extends Activity> screen) {
        if (screen == PatchesSettingsActivity.class) {
            return ACTION_SETTINGS;
        }
        if (screen == GalleryPickerActivity.class) {
            return ACTION_GALLERY_PICKER;
        }
        if (screen == RetakeCountActivity.class) {
            return ACTION_RETAKE_COUNT;
        }
        return null;
    }

}
