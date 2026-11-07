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
package app.hxreborn.extension.internetspeedmeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PremiumSettings {

    static final String NOTIFICATION_PREFERENCE = "NotificationPreference";

    static final String TAP_ACTION = "opendialog";

    static final String ACCENT = "NotificationAccent";

    static final String ACCENT_BACKGROUND = "NotificationAccentBG";

    static final String STARTING_HOUR = "startingHour";

    static final int SHOW_WHEN_CONNECTED = 0;

    static final int SHOW_ALWAYS = 1;

    static final int TAP_SHOWS_DIALOG = 0;

    static final int ACCENT_THEME = 0;

    private static final String DATE_CHANGED_ACTION = "com.sourabh.ACTION_DATE_CHANGED";

    private static final String DATE_RECEIVER = "com.internet.speed.meter.lite.PermanentBroadcast";

    private PremiumSettings() {

    }

    static int notificationPreferenceTitle() {
        return 0;
    }

    static int tapActionTitle() {
        return 0;
    }

    static int startingHourTitle() {
        return 0;
    }

    static int accentTitle() {
        return 0;
    }

    static int themeTitle() {
        return 0;
    }

    public static boolean onRowClick(View row) {
        Activity activity = (Activity) row.getContext();
        int title = rowTitle(row);
        if (title == notificationPreferenceTitle()) {
            showNotificationPreference(activity, row);
        } else if (title == tapActionTitle()) {
            showTapAction(activity, row);
        } else if (title == startingHourTitle()) {
            showStartingHour(activity);
        } else if (title == accentTitle()) {
            showAccent(activity, row);
        } else if (title == themeTitle()) {
            ThemeEditor.show(activity);
        } else {
            return false;
        }
        return true;
    }

    public static void bindRow(View row) {
        Context context = row.getContext();
        row.findViewById(HostResources.promoRibbonId()).setVisibility(View.GONE);
        String summary = summary(context, rowTitle(row));
        if (summary != null) {
            summaryView(row).setText(summary);
        }
    }

    private static int rowTitle(View row) {
        Context context = row.getContext();
        TextView title = row.findViewById(HostResources.rowTitleViewId());
        CharSequence text = title.getText();
        int[] titles = { notificationPreferenceTitle(), tapActionTitle(), startingHourTitle(), accentTitle(),
                themeTitle() };
        for (int titleId : titles) {
            if (context.getString(titleId).contentEquals(text)) {
                return titleId;
            }
        }
        return 0;
    }

    static int notificationPreference(Context context) {
        return AppResources.preferences(context).getInt(NOTIFICATION_PREFERENCE, SHOW_ALWAYS);
    }

    private static String summary(Context context, int title) {
        SharedPreferences preferences = AppResources.preferences(context);
        if (title == notificationPreferenceTitle()) {
            return context.getResources()
                .getStringArray(HostResources.notificationPreferenceOptions())[notificationPreference(context)];
        }
        if (title == tapActionTitle()) {
            return context.getResources().getStringArray(HostResources.tapActionOptions())[preferences
                .getInt(TAP_ACTION, TAP_SHOWS_DIALOG)];
        }
        if (title == accentTitle()) {
            return context.getResources().getStringArray(HostResources.accentOptions())[preferences.getInt(ACCENT,
                    ACCENT_THEME)];
        }
        return null;
    }

    private static TextView summaryView(View row) {
        return row.findViewById(HostResources.rowSummaryViewId());
    }

    private static void showNotificationPreference(Activity activity, View row) {
        String[] items = activity.getResources().getStringArray(HostResources.notificationPreferenceOptions());
        new AlertDialog.Builder(activity)
            .setTitle(activity.getString(HostResources.notificationPreferenceDialogTitle()))
            .setSingleChoiceItems(items, notificationPreference(activity), (dialog, which) -> {
                AppResources.preferences(activity).edit().putInt(NOTIFICATION_PREFERENCE, which).apply();
                summaryView(row).setText(items[which]);
                SmartNotification.applyPreference(activity);
                dialog.dismiss();
            })
            .show();
    }

    private static void showTapAction(Activity activity, View row) {
        String[] items = activity.getResources().getStringArray(HostResources.tapActionOptions());
        SharedPreferences preferences = AppResources.preferences(activity);
        new AlertDialog.Builder(activity).setTitle(activity.getString(tapActionTitle()))
            .setSingleChoiceItems(items, preferences.getInt(TAP_ACTION, TAP_SHOWS_DIALOG), (dialog, which) -> {
                preferences.edit().putInt(TAP_ACTION, which).apply();
                summaryView(row).setText(items[which]);
                dialog.dismiss();
            })
            .show();
    }

    private static void showStartingHour(Activity activity) {
        SharedPreferences preferences = AppResources.preferences(activity);
        new AlertDialog.Builder(activity).setTitle(activity.getString(startingHourTitle()))
            .setSingleChoiceItems(activity.getResources().getStringArray(HostResources.startingHourOptions()),
                    preferences.getInt(STARTING_HOUR, 0), (dialog, which) -> {
                        preferences.edit().putInt(STARTING_HOUR, which).apply();
                        Intent dateChanged = new Intent(DATE_CHANGED_ACTION);
                        dateChanged.setClassName(activity, DATE_RECEIVER);
                        activity.sendBroadcast(dateChanged);
                        dialog.dismiss();
                    })
            .show();
    }

    private static void showAccent(Activity activity, View row) {
        SharedPreferences preferences = AppResources.preferences(activity);
        String[] items = activity.getResources().getStringArray(HostResources.accentOptions());
        View content = View.inflate(activity, HostResources.notificationAccentLayout(), null);
        Spinner spinner = content.findViewById(HostResources.accentSpinnerId());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferences.getInt(ACCENT, ACCENT_THEME));
        CheckBox background = content.findViewById(HostResources.accentBackgroundCheckboxId());
        background.setChecked(preferences.getBoolean(ACCENT_BACKGROUND, false));
        new AlertDialog.Builder(activity).setTitle(activity.getString(accentTitle()))
            .setView(content)
            .setPositiveButton(activity.getString(HostResources.okLabel()), (dialog, which) -> {
                preferences.edit()
                    .putInt(ACCENT, spinner.getSelectedItemPosition())
                    .putBoolean(ACCENT_BACKGROUND, background.isChecked())
                    .apply();
                summaryView(row).setText(items[spinner.getSelectedItemPosition()]);
            })
            .setNegativeButton(activity.getString(HostResources.cancelLabel()), null)
            .show();
    }

}
