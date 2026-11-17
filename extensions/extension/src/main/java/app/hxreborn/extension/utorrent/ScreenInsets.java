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
package app.hxreborn.extension.utorrent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

@SuppressWarnings("unused")
public final class ScreenInsets implements View.OnApplyWindowInsetsListener {

    private static final int ANDROID_17 = 37;

    ScreenInsets() {
    }

    public static void attach(Activity activity) {
        if (Build.VERSION.SDK_INT < ANDROID_17) {
            return;
        }
        attach(activity.findViewById(mainContentId()));
        attach(activity.findViewById(pageContentId()));
        attach(activity.findViewById(drawerContentId()));
    }

    public static void attachAudio(Activity activity) {
        if (Build.VERSION.SDK_INT < ANDROID_17) {
            return;
        }
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content != null && content.getChildCount() == 1) {
            attach(content.getChildAt(0));
        }
    }

    public static void attachQueue(Dialog dialog) {
        if (Build.VERSION.SDK_INT >= ANDROID_17) {
            attach(dialog.findViewById(android.R.id.content));
        }
    }

    private static void attach(View view) {
        if (view == null) {
            return;
        }
        view.setFitsSystemWindows(false);
        view.setOnApplyWindowInsetsListener(new ScreenInsets());
        view.requestApplyInsets();
    }

    @TargetApi(Build.VERSION_CODES.R)
    @Override
    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
        Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
        return insets.inset(safe.left, safe.top, safe.right, safe.bottom);
    }

    private static int drawerContentId() {
        return 0;
    }

    private static int mainContentId() {
        return 0;
    }

    private static int pageContentId() {
        return 0;
    }

}
