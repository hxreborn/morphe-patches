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
package app.hxreborn.extension.bittorrent;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

@SuppressWarnings("unused")
public final class ScreenInsets implements View.OnApplyWindowInsetsListener {

    private static final int ANDROID_17 = 37;

    private final boolean insetBottom;

    ScreenInsets(boolean insetBottom) {
        this.insetBottom = insetBottom;
    }

    public static void attach(Activity activity) {
        if (Build.VERSION.SDK_INT < ANDROID_17) {
            return;
        }
        View main = activity.findViewById(mainContentId());
        if (main != null) {
            main.getViewTreeObserver().addOnGlobalLayoutListener(new MainContent(main));
            attach(main, false);
        } else {
            attach(activity.findViewById(pageContentId()), true);
        }
        attach(activity.findViewById(drawerContentId()), true);
    }

    public static void attachSheet(View view) {
        if (Build.VERSION.SDK_INT >= ANDROID_17) {
            attach(view, true);
        }
    }

    private static void attach(View view, boolean insetBottom) {
        if (view == null) {
            return;
        }
        view.setFitsSystemWindows(false);
        view.setOnApplyWindowInsetsListener(new ScreenInsets(insetBottom));
        view.requestApplyInsets();
    }

    @TargetApi(Build.VERSION_CODES.R)
    @Override
    @SuppressLint("UseRequiresApi")
    @SuppressWarnings("NullableProblems")
    public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
        Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        int bottom = this.insetBottom ? safe.bottom : 0;
        view.setPadding(safe.left, safe.top, safe.right, bottom);
        return insets.inset(safe.left, safe.top, safe.right, bottom);
    }

    static void clearPageSpacing(View page, View main) {
        if (page.getPaddingTop() != 0) {
            page.setPadding(page.getPaddingLeft(), 0, page.getPaddingRight(), page.getPaddingBottom());
        }
        if (page.getBackground() instanceof ColorDrawable) {
            int color = ((ColorDrawable) page.getBackground()).getColor();
            if (!(main.getBackground() instanceof ColorDrawable)
                    || ((ColorDrawable) main.getBackground()).getColor() != color) {
                main.setBackgroundColor(color);
            }
        }
    }

    private static void clearToolbarMargin(View page, int id) {
        View view = page.findViewById(id);
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams parameters)) {
            return;
        }
        if (parameters.topMargin != 0) {
            parameters.topMargin = 0;
            view.setLayoutParams(parameters);
        }
    }

    private static int drawerButtonId() {
        return 0;
    }

    private static int searchButtonId() {
        return 0;
    }

    private static int addButtonId() {
        return 0;
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

    private static final class MainContent implements ViewTreeObserver.OnGlobalLayoutListener {

        private final View main;

        MainContent(View main) {
            this.main = main;
        }

        @Override
        public void onGlobalLayout() {
            update(this.main);
        }

        private void update(View view) {
            if (view.getId() == pageContentId()) {
                clearPageSpacing(view, this.main);
                clearToolbarMargin(view, drawerButtonId());
                clearToolbarMargin(view, searchButtonId());
                clearToolbarMargin(view, addButtonId());
                return;
            }
            if (view instanceof ViewGroup group) {
                for (int index = 0; index < group.getChildCount(); index++) {
                    update(group.getChildAt(index));
                }
            }
        }

    }

}
