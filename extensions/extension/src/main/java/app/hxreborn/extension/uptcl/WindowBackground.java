/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.uptcl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

@SuppressWarnings("unused")
public final class WindowBackground {

    private WindowBackground() {

    }

    public static void paintWhite(Activity activity) {
        Window window = activity.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        View decorView = window.getDecorView();
        decorView.getViewTreeObserver().addOnPreDrawListener(() -> {
            keepDarkIcons(decorView);
            return true;
        });
    }

    private static void keepDarkIcons(View decorView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int darkIcons = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null && (controller.getSystemBarsAppearance() & darkIcons) != darkIcons) {
                controller.setSystemBarsAppearance(darkIcons, darkIcons);
            }
            return;
        }
        int darkIcons = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            darkIcons |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        int visibility = decorView.getSystemUiVisibility();
        if ((visibility & darkIcons) != darkIcons) {
            decorView.setSystemUiVisibility(visibility | darkIcons);
        }
    }

}
