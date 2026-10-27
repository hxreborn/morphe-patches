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

    private static final String[] PREMIUM_ROWS = { "notif_pref_title", "notif_action_title", "startingHour_title",
            "Notification_accent_title", "theme_title" };

    private PremiumSettings() {

    }

    public static boolean onRowClick(View row) {
        Activity activity = (Activity) row.getContext();
        switch (rowTitleName(row)) {
            case "notif_pref_title":
                showNotificationPreference(activity, row);
                return true;
            case "notif_action_title":
                showTapAction(activity, row);
                return true;
            case "startingHour_title":
                showStartingHour(activity);
                return true;
            case "Notification_accent_title":
                showAccent(activity, row);
                return true;
            case "theme_title":
                ThemeEditor.show(activity);
                return true;
            default:
                return false;
        }
    }

    public static void bindRow(View row) {
        Context context = row.getContext();
        row.findViewById(AppResources.id(context, "id", "promo_ribbon")).setVisibility(View.GONE);
        String summary = summary(context, rowTitleName(row));
        if (summary != null) {
            summaryView(row).setText(summary);
        }
    }

    private static String rowTitleName(View row) {
        TextView title = row.findViewById(AppResources.id(row.getContext(), "id", "textView1"));
        CharSequence text = title.getText();
        for (String name : PREMIUM_ROWS) {
            if (AppResources.string(row.getContext(), name).contentEquals(text)) {
                return name;
            }
        }
        return "";
    }

    static int notificationPreference(Context context) {
        return AppResources.preferences(context).getInt(NOTIFICATION_PREFERENCE, SHOW_ALWAYS);
    }

    private static String summary(Context context, String titleName) {
        SharedPreferences preferences = AppResources.preferences(context);
        switch (titleName) {
            case "notif_pref_title":
                return AppResources.array(context, "notification_pref_dialog_array")[notificationPreference(context)];
            case "notif_action_title":
                return AppResources.array(context, "notif_tap_action_array")[preferences.getInt(TAP_ACTION,
                        TAP_SHOWS_DIALOG)];
            case "Notification_accent_title":
                return AppResources.array(context, "notification_accent_array")[preferences.getInt(ACCENT,
                        ACCENT_THEME)];
            default:
                return null;
        }
    }

    private static TextView summaryView(View row) {
        return row.findViewById(AppResources.id(row.getContext(), "id", "textView2"));
    }

    private static void showNotificationPreference(Activity activity, View row) {
        String[] items = AppResources.array(activity, "notification_pref_dialog_array");
        new AlertDialog.Builder(activity).setTitle(AppResources.string(activity, "Notification_Preference_dialog"))
            .setSingleChoiceItems(items, notificationPreference(activity), (dialog, which) -> {
                AppResources.preferences(activity).edit().putInt(NOTIFICATION_PREFERENCE, which).apply();
                summaryView(row).setText(items[which]);
                SmartNotification.applyPreference(activity);
                dialog.dismiss();
            })
            .show();
    }

    private static void showTapAction(Activity activity, View row) {
        String[] items = AppResources.array(activity, "notif_tap_action_array");
        SharedPreferences preferences = AppResources.preferences(activity);
        new AlertDialog.Builder(activity).setTitle(AppResources.string(activity, "notif_action_title"))
            .setSingleChoiceItems(items, preferences.getInt(TAP_ACTION, TAP_SHOWS_DIALOG), (dialog, which) -> {
                preferences.edit().putInt(TAP_ACTION, which).apply();
                summaryView(row).setText(items[which]);
                dialog.dismiss();
            })
            .show();
    }

    private static void showStartingHour(Activity activity) {
        SharedPreferences preferences = AppResources.preferences(activity);
        new AlertDialog.Builder(activity).setTitle(AppResources.string(activity, "startingHour_title"))
            .setSingleChoiceItems(AppResources.array(activity, "starting_hour_array"),
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
        String[] items = AppResources.array(activity, "notification_accent_array");
        View content = View.inflate(activity, AppResources.id(activity, "layout", "notification_accent"), null);
        Spinner spinner = content.findViewById(AppResources.id(activity, "id", "NotificationAccentSpinner"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(preferences.getInt(ACCENT, ACCENT_THEME));
        CheckBox background = content.findViewById(AppResources.id(activity, "id", "accentIsBgCheckbox"));
        background.setChecked(preferences.getBoolean(ACCENT_BACKGROUND, false));
        new AlertDialog.Builder(activity).setTitle(AppResources.string(activity, "Notification_accent_title"))
            .setView(content)
            .setPositiveButton(AppResources.string(activity, "OK"), (dialog, which) -> {
                preferences.edit()
                    .putInt(ACCENT, spinner.getSelectedItemPosition())
                    .putBoolean(ACCENT_BACKGROUND, background.isChecked())
                    .apply();
                summaryView(row).setText(items[spinner.getSelectedItemPosition()]);
            })
            .setNegativeButton(AppResources.string(activity, "Cancel"), null)
            .show();
    }

}
