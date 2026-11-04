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
package app.hxreborn.extension.liftoff;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ConfirmationDialog {

    static final String RESET_DESCRIPTION = "Restores every switch to its default.";

    private static final int WIDTH_DP = 320;

    private static final int PADDING_DP = 24;

    private static final int GAP_DP = 16;

    private static final int RADIUS_DP = 28;

    private static final int BUTTON_ROW_BOTTOM_PADDING_DP = 12;

    private final Activity activity;

    private final SettingsStyle style;

    private final Choice choice;

    private final Runnable onConfirm;

    private final Dialog dialog;

    private ConfirmationDialog(Activity activity, SettingsStyle style, Choice choice, Runnable onConfirm) {
        this.activity = activity;
        this.style = style;
        this.choice = choice;
        this.onConfirm = onConfirm;
        this.dialog = new Dialog(activity);
        this.dialog.setOwnerActivity(activity);
    }

    static Dialog showReset(Activity activity, SettingsStyle style, Runnable onReset) {
        return new ConfirmationDialog(activity, style, Choice.RESET, onReset).show();
    }

    static Dialog showRestart(Activity activity, SettingsStyle style, Runnable onRestart) {
        return new ConfirmationDialog(activity, style, Choice.RESTART, onRestart).show();
    }

    private Dialog show() {
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.dialog.setContentView(panel());
        this.dialog.show();
        Window window = this.dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int availableWidth = this.activity.getWindow().getDecorView().getWidth();
            if (availableWidth <= 0) {
                availableWidth = this.activity.getResources().getDisplayMetrics().widthPixels;
            }
            window.setLayout(Math.min(this.style.dp(WIDTH_DP), availableWidth), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return this.dialog;
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this.activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = this.style.dp(PADDING_DP);
        panel.setPadding(padding, padding, padding, this.style.dp(BUTTON_ROW_BOTTOM_PADDING_DP));
        panel.setBackground(this.style.rounded(this.style.cardSurface, RADIUS_DP));
        panel.addView(
                this.style.text(this.choice.title, SettingsStyle.TITLE_SP, this.style.primaryText, this.style.medium));
        TextView body = this.style.text(this.choice.message, SettingsStyle.MESSAGE_SP, this.style.secondaryText, null);
        body.setPadding(0, this.style.dp(GAP_DP), 0, padding);
        panel.addView(body);
        LinearLayout buttons = new LinearLayout(this.activity);
        buttons.setGravity(Gravity.END);
        buttons.addView(this.style.textButton(this.choice.cancel, (view) -> this.dialog.dismiss()));
        buttons.addView(this.style.textButton(this.choice.confirm, (view) -> {
            this.dialog.dismiss();
            this.onConfirm.run();
        }));
        panel.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return panel;
    }

    private enum Choice {

        RESET("Reset to defaults?", RESET_DESCRIPTION, "Reset", "Cancel"),
        RESTART("Restart required", "Liftoff must restart to apply the change.", "Restart", "Later");

        final String title;

        final String message;

        final String confirm;

        final String cancel;

        Choice(String title, String message, String confirm, String cancel) {
            this.title = title;
            this.message = message;
            this.confirm = confirm;
            this.cancel = cancel;
        }

    }

}
