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
package app.hxreborn.extension.netanalyzer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import app.hxreborn.extension.shared.Dimensions;

@TargetApi(Build.VERSION_CODES.Q)
final class ExportConfiguration {

    static final String DEFAULT_SEPARATOR = ",";

    private static final String PREFERENCES = "ExportConfiguration";

    private static final String SEPARATOR_KEY = "csvSeparator";

    private static final String TITLE = "Configure CSV Separator";

    private static final String MESSAGE = "The most common CSV separators are \",\" and \";\".";

    private static final int DIALOG_INSET_DP = 24;

    private static final int FIELD_TOP_DP = 8;

    private ExportConfiguration() {
    }

    static String separator(Context context) {
        final String separator = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(SEPARATOR_KEY, DEFAULT_SEPARATOR);
        return (separator != null) ? separator : DEFAULT_SEPARATOR;
    }

    static void show(Activity activity) {
        final HostPalette palette = HostPalette.of(activity);
        final FilledField field = new FilledField(activity, palette, "", "", InputType.TYPE_CLASS_TEXT);
        field.input().setText(separator(activity));
        final LinearLayout container = new LinearLayout(activity);
        final int inset = dp(activity, DIALOG_INSET_DP);
        container.setPaddingRelative(inset, dp(activity, FIELD_TOP_DP), inset, 0);
        container.addView(field, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(activity).setTitle(TITLE)
            .setMessage(MESSAGE)
            .setView(container)
            .setPositiveButton("OK", (dialog, which) -> save(activity, field.input()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static void save(Context context, EditText input) {
        final String text = input.getText().toString();
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(SEPARATOR_KEY, text.isEmpty() ? DEFAULT_SEPARATOR : text)
            .apply();
    }

    private static int dp(Context context, int value) {
        return Dimensions.dp(context, value);
    }

}
