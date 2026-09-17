/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

import java.util.List;
import java.util.Locale;

import app.morphe.extension.shared.Utils;

@SuppressLint("SetTextI18n")
@SuppressWarnings("deprecation")
final class ScheduledDeletionEditor {

    static final String SETTINGS_ROW_TITLE = "Scheduled deletion";

    private static final int[] UNIT_SECONDS = { 60, 3600, 86400 };
    private static final String[] UNIT_LABELS = { "Minutes", "Hours", "Days" };
    private static final int DEFAULT_SECONDS = 30 * 86400;

    private static final int TOUCH_TARGET_DP = 48;
    private static final int CONTROL_SPACING_DP = 8;
    private static final int SECTION_SPACING_DP = 16;
    private static final int BODY_TEXT_SP = 14;

    private ScheduledDeletionEditor() {}

    static String summary(Context context) {
        final String schedules = schedules(context);
        return schedules.isEmpty() ? "Off" : schedules;
    }

    private static String schedules(Context context) {
        final StringBuilder summary = new StringBuilder();
        for (String label : ScheduledDeletion.EMPTIED_LABELS) {
            final int seconds = ScheduledDeletionSettings.intervalSeconds(context, label);
            if (seconds <= ScheduledDeletionSettings.OFF) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(folderName(label)).append(" every ").append(intervalLabel(seconds));
        }
        return summary.toString();
    }

    static void showDeleted(List<String> labels) {
        if (labels.isEmpty()) {
            return;
        }
        if (!ScheduledDeletionSettings.showsToast(Utils.getContext())) {
            return;
        }

        Utils.showToastLong("Deleted all messages in " + folderNames(labels));
    }

    private static String folderNames(List<String> labels) {
        final StringBuilder names = new StringBuilder(folderName(labels.get(0)));
        for (int index = 1; index < labels.size(); index++) {
            names.append(index == labels.size() - 1 ? " and " : ", ")
                    .append(folderName(labels.get(index)));
        }
        return names.toString();
    }

    static void show(Activity activity, Runnable onSettingChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        final LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setFocusableInTouchMode(true);
        content.requestFocus();

        final String[] labels = ScheduledDeletion.EMPTIED_LABELS;
        final LabelSection[] sections = new LabelSection[labels.length];
        for (int index = 0; index < labels.length; index++) {
            sections[index] = new LabelSection(activity, labels[index]);
            sections[index].addTo(content, index > 0);
        }

        final Switch notificationSwitch = new Switch(activity);
        notificationSwitch.setText("Show a toast after each deletion");
        notificationSwitch.setGravity(Gravity.CENTER_VERTICAL);
        notificationSwitch.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));
        notificationSwitch.setChecked(ScheduledDeletionSettings.showsToast(activity));

        final TextView warningText = new TextView(activity);
        warningText.setText("Deletes everything in each folder on its own schedule, regardless "
                + "of when a message arrived there. Deleted messages cannot be recovered.");
        warningText.setTextSize(TypedValue.COMPLEX_UNIT_SP, BODY_TEXT_SP);

        final LinearLayout.LayoutParams notificationParams = matchWidth();
        notificationParams.topMargin = dp(activity, SECTION_SPACING_DP);
        content.addView(notificationSwitch, notificationParams);

        final LinearLayout.LayoutParams warningParams = matchWidth();
        warningParams.topMargin = dp(activity, CONTROL_SPACING_DP);
        warningParams.bottomMargin = dp(activity, CONTROL_SPACING_DP);
        content.addView(warningText, warningParams);

        final ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);

        final LinearLayout surface = PatchesDialog.createContentLayout(activity);
        surface.addView(PatchesDialog.createTitle(activity, SETTINGS_ROW_TITLE));
        surface.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final Dialog dialog = PatchesDialog.createDialog(activity, surface);
        final int accentColor = AccentColor.getAccentColor(
                PatchesTheme.isDark(PatchesTheme.resolveColorAttribute(
                        activity, PatchesTheme.BACKGROUND_SECONDARY)));
        final int textWeakColor =
                PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_WEAK);

        final LinearLayout actions = PatchesDialog.createButtonRow(activity);
        actions.addView(PatchesDialog.configureActionButton(activity,
                PatchesDialog.createTextView(activity, "Cancel", BODY_TEXT_SP, textWeakColor),
                dialog::dismiss));
        actions.addView(PatchesDialog.configureActionButton(activity,
                PatchesDialog.createTextView(activity, "Save", BODY_TEXT_SP, accentColor),
                () -> {
                    final int[] seconds = new int[sections.length];
                    for (int index = 0; index < sections.length; index++) {
                        seconds[index] = sections[index].selectedSeconds();
                        if (seconds[index] < 0) {
                            Toast.makeText(activity,
                                    "The interval must be between 1 minute and 365 days",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    for (int index = 0; index < sections.length; index++) {
                        ScheduledDeletionSettings.saveIntervalSeconds(
                                activity, sections[index].label, seconds[index]);
                    }
                    ScheduledDeletionSettings.saveShowsToast(
                            activity, notificationSwitch.isChecked());
                    Toast.makeText(activity, savedMessage(activity), Toast.LENGTH_SHORT).show();
                    onSettingChanged.run();
                    dialog.dismiss();
                }));
        surface.addView(actions, matchWidth());

        PatchesTheme.tintSwitch(notificationSwitch, accentColor);
        notificationSwitch.setTextColor(
                PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_NORM));
        warningText.setTextColor(textWeakColor);
        for (LabelSection section : sections) {
            section.applyTheme(accentColor, textWeakColor);
        }

        final Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
    }

    private static final class LabelSection {
        private final String label;
        private final Switch enabledSwitch;
        private final TextView periodLabel;
        private final EditText amountInput;
        private final Spinner unitSpinner;
        private final LinearLayout periodInput;

        LabelSection(Activity activity, String label) {
            this.label = label;
            final String folder = folderName(label);
            final int intervalSeconds =
                    ScheduledDeletionSettings.intervalSeconds(activity, label);
            final int displayedSeconds = intervalSeconds > ScheduledDeletionSettings.OFF
                    ? intervalSeconds
                    : DEFAULT_SECONDS;
            final int unitIndex = largestExactUnit(displayedSeconds);

            enabledSwitch = new Switch(activity);
            enabledSwitch.setText("Delete " + folder + " on a schedule");
            enabledSwitch.setGravity(Gravity.CENTER_VERTICAL);
            enabledSwitch.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));
            enabledSwitch.setChecked(intervalSeconds > ScheduledDeletionSettings.OFF);

            periodLabel = new TextView(activity);
            periodLabel.setText("Delete every");
            periodLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, BODY_TEXT_SP);

            amountInput = new EditText(activity);
            amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            amountInput.setSingleLine(true);
            amountInput.setSelectAllOnFocus(true);
            amountInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
            amountInput.setText(String.valueOf(displayedSeconds / UNIT_SECONDS[unitIndex]));
            amountInput.setContentDescription(folder + " deletion interval");
            amountInput.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));

            unitSpinner = new Spinner(activity);
            final ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, UNIT_LABELS);
            unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            unitSpinner.setAdapter(unitAdapter);
            unitSpinner.setSelection(unitIndex);
            unitSpinner.setContentDescription(folder + " interval unit");
            unitSpinner.setMinimumHeight(dp(activity, TOUCH_TARGET_DP));

            periodInput = new LinearLayout(activity);
            periodInput.setOrientation(LinearLayout.HORIZONTAL);
            periodInput.setGravity(Gravity.CENTER_VERTICAL);
            periodInput.addView(amountInput, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            unitParams.setMarginStart(dp(activity, CONTROL_SPACING_DP));
            periodInput.addView(unitSpinner, unitParams);

            final View[] settingViews = { periodLabel, amountInput, unitSpinner };
            setEnabled(settingViews, enabledSwitch.isChecked());
            enabledSwitch.setOnCheckedChangeListener(
                    (button, checked) -> setEnabled(settingViews, checked));
        }

        void applyTheme(int accentColor, int textWeakColor) {
            PatchesTheme.tintSwitch(enabledSwitch, accentColor);
            PatchesTheme.tintTextInput(amountInput, accentColor);
            enabledSwitch.setTextColor(PatchesTheme.resolveColorAttribute(
                    enabledSwitch.getContext(), PatchesTheme.TEXT_NORM));
            periodLabel.setTextColor(textWeakColor);
            amountInput.setTextColor(PatchesTheme.resolveColorAttribute(
                    amountInput.getContext(), PatchesTheme.TEXT_NORM));
        }

        void addTo(LinearLayout content, boolean separated) {
            final Context context = content.getContext();
            final LinearLayout.LayoutParams switchParams = matchWidth();
            if (separated) {
                switchParams.topMargin = dp(context, SECTION_SPACING_DP);
            }
            content.addView(enabledSwitch, switchParams);

            final LinearLayout.LayoutParams periodLabelParams = matchWidth();
            periodLabelParams.topMargin = dp(context, CONTROL_SPACING_DP);
            content.addView(periodLabel, periodLabelParams);

            content.addView(periodInput, matchWidth());
        }

        int selectedSeconds() {
            if (!enabledSwitch.isChecked()) {
                return ScheduledDeletionSettings.OFF;
            }
            return parseIntervalSeconds(amountInput.getText().toString(),
                    UNIT_SECONDS[unitSpinner.getSelectedItemPosition()]);
        }
    }

    private static void setEnabled(View[] views, boolean enabled) {
        for (View view : views) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private static int parseIntervalSeconds(String text, int unitSeconds) {
        final String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
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
            if (seconds >= UNIT_SECONDS[index] && seconds % UNIT_SECONDS[index] == 0) {
                return index;
            }
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

    private static String savedMessage(Context context) {
        final String schedules = schedules(context);
        return schedules.isEmpty() ? "Trash and Spam will not be deleted" : schedules;
    }

    private static int dp(Context context, int value) {
        return PatchesTheme.dpToPx(context, value);
    }

    private static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
