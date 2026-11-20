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
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ConfirmDialog {

    private static final int MAX_WIDTH_DP = 560;

    private static final float WIDTH_FRACTION = 0.92f;

    private static final int RADIUS_DP = 24;

    private static final int PILL_RADIUS_DP = 24;

    private ConfirmDialog() {
    }

    static void showRestart(Activity activity, SettingsStyle style) {
        show(activity, style, "Restart required", "BeReal must restart to apply the change.", "Later", "Restart",
                () -> restartApp(activity));
    }

    static void show(Activity activity, SettingsStyle style, String titleText, String messageText, String cancelLabel,
            String confirmLabel, Runnable onConfirm) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(style.rounded(SettingsStyle.CARD_SURFACE, RADIUS_DP));
        layout.setPadding(style.dp(24), style.dp(24), style.dp(24), style.dp(16));

        final TextView title = style.text(titleText, SettingsStyle.TITLE_SP, SettingsStyle.PRIMARY_TEXT,
                style.fonts.semibold);
        SettingsStyle.markHeading(title);
        layout.addView(title);
        final TextView message = style.text(messageText, SettingsStyle.BODY_SP, SettingsStyle.SECONDARY_TEXT,
                style.fonts.regular);
        final LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = style.dp(8);
        messageParams.bottomMargin = style.dp(20);
        layout.addView(message, messageParams);

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.setContentView(layout);
        final Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            final DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setLayout(Math.min(Math.round(metrics.widthPixels * WIDTH_FRACTION), style.dp(MAX_WIDTH_DP)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.addView(button(style, cancelLabel, SettingsStyle.PRIMARY_TEXT, Color.TRANSPARENT, dialog::dismiss));
        final View confirm = button(style, confirmLabel, Color.BLACK, Color.WHITE, () -> {
            dialog.dismiss();
            onConfirm.run();
        });
        final LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        confirmParams.leftMargin = style.dp(8);
        actions.addView(confirm, confirmParams);
        layout.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.show();
    }

    private static View button(SettingsStyle style, String label, int textColor, int surface, Runnable action) {
        final TextView button = style.text(label, SettingsStyle.ROW_TITLE_SP, textColor, style.fonts.semibold);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(style.dp(96));
        button.setMinHeight(style.dp(48));
        button.setPadding(style.dp(24), 0, style.dp(24), 0);
        button.setBackground(style.ripple(surface, PILL_RADIUS_DP));
        button.setClickable(true);
        button.setOnClickListener((view) -> action.run());
        return button;
    }

    private static void restartApp(Activity activity) {
        final Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch == null) {
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(launch);
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }

}
