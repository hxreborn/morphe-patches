/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

@SuppressLint("SetTextI18n")
@SuppressWarnings("deprecation")
final class ScheduledDeletionEditor {

    static final String SETTINGS_ROW_TITLE = "Scheduled deletion";

    private static final int[] UNIT_SECONDS = { 60, 3600, 86400 };
    private static final String[] UNIT_LABELS = { "Minutes", "Hours", "Days" };
    private static final int DEFAULT_SECONDS = 30 * 86400;

    private static final int TOUCH_TARGET_DP = 48;
    private static final int DIALOG_HORIZONTAL_PADDING_DP = 24;
    private static final int DIALOG_VERTICAL_PADDING_DP = 8;
    private static final int CONTROL_SPACING_DP = 8;
    private static final int BODY_TEXT_SP = 14;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ScheduledDeletionEditor() {}

    static String summary(Context context) {
        final int seconds = ScheduledDeletionSettings.intervalSeconds(context);
        if (seconds <= ScheduledDeletionSettings.OFF) return "Off";
        return "Every " + intervalLabel(seconds);
    }

    static void showEmptied(String label) {
        MAIN.post(() -> {
            final Activity activity = PatchApplication.resumedActivity();
            if (activity == null || activity.isFinishing()) return;
            if (!ScheduledDeletionSettings.showsToast(activity)) return;

            Toast.makeText(activity, "Deleted all messages in " + folderName(label),
                    Toast.LENGTH_LONG).show();
        });
    }

    static void show(Activity activity, Runnable onSettingChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) return;


        final int intervalSeconds = ScheduledDeletionSettings.intervalSeconds(activity);
        final int displayedSeconds = intervalSeconds > ScheduledDeletionSettings.OFF
                ? intervalSeconds
                : DEFAULT_SECONDS;
        final int unitIndex = largestExactUnit(displayedSeconds);

        final Switch enabledSwitch = new Switch(activity);
        enabledSwitch.setText("Delete Trash and Spam on a schedule");
        enabledSwitch.setGravity(Gravity.CENTER_VERTICAL);
        enabledSwitch.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));
        enabledSwitch.setChecked(intervalSeconds > ScheduledDeletionSettings.OFF);

        final TextView periodLabel = new TextView(activity);
        periodLabel.setText("Delete every");
        periodLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, BODY_TEXT_SP);

        final EditText amountInput = new EditText(activity);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.setSingleLine(true);
        amountInput.setSelectAllOnFocus(true);
        amountInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        amountInput.setText(String.valueOf(displayedSeconds / UNIT_SECONDS[unitIndex]));
        amountInput.setContentDescription("Deletion interval");
        amountInput.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));

        final Spinner unitSpinner = new Spinner(activity);
        final ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, UNIT_LABELS);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        unitSpinner.setSelection(unitIndex);
        unitSpinner.setContentDescription("Interval unit");
        unitSpinner.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));

        final Switch notificationSwitch = new Switch(activity);
        notificationSwitch.setText("Show a toast after each deletion");
        notificationSwitch.setGravity(Gravity.CENTER_VERTICAL);
        notificationSwitch.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));
        notificationSwitch.setChecked(ScheduledDeletionSettings.showsToast(activity));

        final TextView warningText = new TextView(activity);
        warningText.setText("Deletes all Trash and Spam on a fixed schedule, regardless of "
                + "when a message was trashed. Deleted messages cannot be recovered.");
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_SP, BODY_TEXT_SP);

        final View[] settingViews = {
                periodLabel, amountInput, unitSpinner, notificationSwitch
        };
        setEnabled(settingViews, enabledSwitch.isChecked());
        enabledSwitch.setOnCheckedChangeListener(
                (button, checked) -> setEnabled(settingViews, checked));

        final LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true);
        content.requestFocus();
        final int horizontalPadding = dp(activity, DIALOG_HORIZONTAL_PADDING_DP);
        final int verticalPadding = dp(activity, DIALOG_VERTICAL_PADDING_DP);
        content.setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0);

        content.addView(enabledSwitch, matchWidth());

        final LinearLayout.LayoutParams periodLabelParams = matchWidth();
        periodLabelParams.topMargin = dp(activity, CONTROL_SPACING_DP);
        content.addView(periodLabel, periodLabelParams);

        final LinearLayout periodInput = new LinearLayout(activity);
        periodInput.setOrientation(LinearLayout.HORIZONTAL);
        periodInput.setGravity(Gravity.CENTER_VERTICAL);
        periodInput.addView(amountInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        unitParams.setMarginStart(dp(activity, CONTROL_SPACING_DP));
        periodInput.addView(unitSpinner, unitParams);
        content.addView(periodInput, matchWidth());

        final LinearLayout.LayoutParams notificationParams = matchWidth();
        notificationParams.topMargin = dp(activity, CONTROL_SPACING_DP);
        content.addView(notificationSwitch, notificationParams);

        final LinearLayout.LayoutParams warningParams = matchWidth();
        warningParams.topMargin = dp(activity, CONTROL_SPACING_DP);
        warningParams.bottomMargin = dp(activity, CONTROL_SPACING_DP);
        content.addView(warningText, warningParams);

        final ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        final AlertDialog dialog = new AlertDialog.Builder(activity,
                android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(SETTINGS_ROW_TITLE)
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignoredButton -> {
                final int seconds = enabledSwitch.isChecked()
                        ? parseIntervalSeconds(amountInput.getText().toString(),
                                UNIT_SECONDS[unitSpinner.getSelectedItemPosition()])
                        : ScheduledDeletionSettings.OFF;

                if (seconds < 0
                        || !ScheduledDeletionSettings.saveIntervalSeconds(activity, seconds)) {
                    Toast.makeText(activity, "The interval must be between 1 minute and 365 days",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ScheduledDeletionSettings.saveShowsToast(activity, notificationSwitch.isChecked());
                Toast.makeText(activity, savedMessage(seconds), Toast.LENGTH_SHORT).show();
                onSettingChanged.run();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private static void setEnabled(View[] views, boolean enabled) {
        for (View view : views) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private static int parseIntervalSeconds(String text, int unitSeconds) {
        final String trimmed = text.trim();
        if (trimmed.isEmpty()) return -1;
        try {
            final long amount = Long.parseLong(trimmed);
            if (amount < 1 || amount > ScheduledDeletionSettings.MAXIMUM_SECONDS / unitSeconds) {
                return -1;
            }
            final long seconds = amount * unitSeconds;
            if (seconds < ScheduledDeletionSettings.MINIMUM_SECONDS
                    || seconds > ScheduledDeletionSettings.MAXIMUM_SECONDS) {
                return -1;
            }
            return (int) seconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int largestExactUnit(int seconds) {
        for (int index = UNIT_SECONDS.length - 1; index >= 0; index--) {
            if (seconds >= UNIT_SECONDS[index] && seconds % UNIT_SECONDS[index] == 0) return index;
        }
        return UNIT_SECONDS.length - 1;
    }

    static String intervalLabel(int seconds) {
        final int index = largestExactUnit(seconds);
        final int amount = seconds / UNIT_SECONDS[index];
        final String unit = UNIT_LABELS[index].toLowerCase(Locale.US);
        return String.format(Locale.US, "%d %s", amount,
                amount == 1 ? unit.substring(0, unit.length() - 1) : unit);
    }

    private static String folderName(String label) {
        return label.charAt(0) + label.substring(1).toLowerCase(Locale.US);
    }

    private static String savedMessage(int seconds) {
        if (seconds <= ScheduledDeletionSettings.OFF) {
            return "Trash and Spam will not be deleted";
        }
        return "Trash and Spam will be deleted every " + intervalLabel(seconds);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
