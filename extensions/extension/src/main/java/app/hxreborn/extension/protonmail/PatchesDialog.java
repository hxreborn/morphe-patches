/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

final class PatchesDialog {

    static final int BACKGROUND_RADIUS_DP = 32;
    static final int PADDING_DP = 24;
    static final int BOTTOM_PADDING_DP = 12;
    static final int GAP_DP = 12;
    static final int TITLE_SP = 24;
    static final int LABEL_SP = 14;

    private static final int BODY_SP = 14;
    private static final int TOUCH_TARGET_DP = 48;
    private static final int BUTTON_MIN_WIDTH_DP = 96;
    private static final int BUTTON_RADIUS_DP = 20;
    private static final int MAX_WIDTH_DP = 560;
    private static final float WIDTH_FRACTION = 0.92f;

    private PatchesDialog() {}

    static LinearLayout createContentLayout(Activity activity) {
        final LinearLayout contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setBackground(PatchesTheme.createRoundedRectangle(activity,
                PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_SECONDARY),
                BACKGROUND_RADIUS_DP));
        contentLayout.setPadding(dp(activity, PADDING_DP), dp(activity, PADDING_DP),
                dp(activity, PADDING_DP), dp(activity, BOTTOM_PADDING_DP));
        return contentLayout;
    }

    static TextView createTitle(Activity activity, String titleText) {
        return createTextView(activity, titleText, TITLE_SP,
                PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_NORM));
    }

    static TextView createTextView(Activity activity, String value, int sizeSp, int color) {
        final TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        return view;
    }

    static View configureActionButton(Activity activity, TextView label, Runnable onClick) {
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setGravity(Gravity.CENTER);
        label.setMinWidth(dp(activity, BUTTON_MIN_WIDTH_DP));
        label.setMinHeight(dp(activity, TOUCH_TARGET_DP));
        label.setPadding(dp(activity, PADDING_DP / 2), 0, dp(activity, PADDING_DP / 2), 0);
        label.setBackground(
                PatchesTheme.createRoundedRectangle(activity, Color.TRANSPARENT, BUTTON_RADIUS_DP));
        PatchesTheme.makeClickable(label);
        label.setOnClickListener(ignored -> onClick.run());
        return label;
    }

    static LinearLayout createButtonRow(Activity activity) {
        final LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        return actions;
    }

    static Dialog createDialog(Activity activity, View contentLayout) {
        final Dialog dialog = new Dialog(activity,
                android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        dialog.setContentView(contentLayout);

        final Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(dialogWidthPx(activity), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    static void showConfirmation(Activity activity, String titleText, String message,
            String confirmLabel, Runnable onConfirm) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        final int textWeakColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_WEAK);
        final LinearLayout contentLayout = createContentLayout(activity);
        contentLayout.addView(createTitle(activity, titleText));

        final TextView body = createTextView(activity, message, BODY_SP, textWeakColor);
        final LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(activity, GAP_DP);
        bodyParams.bottomMargin = dp(activity, GAP_DP);
        contentLayout.addView(body, bodyParams);

        final LinearLayout actions = createButtonRow(activity);
        final Dialog dialog = createDialog(activity, contentLayout);
        actions.addView(configureActionButton(activity, createTextView(activity, "Later", LABEL_SP, textWeakColor),
                () -> dialog.dismiss()));
        actions.addView(configureActionButton(activity,
                createTextView(activity, confirmLabel, LABEL_SP,
                        AccentColor.getAccentColor(PatchesTheme.isDark(
                                PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_SECONDARY)))),
                () -> {
                    dialog.dismiss();
                    onConfirm.run();
                }));
        contentLayout.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dialog.show();
    }

    private static int dialogWidthPx(Activity activity) {
        final DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return Math.min(Math.round(metrics.widthPixels * WIDTH_FRACTION),
                dp(activity, MAX_WIDTH_DP));
    }

    private static int dp(Activity activity, int value) {
        return PatchesTheme.dpToPx(activity, value);
    }
}
