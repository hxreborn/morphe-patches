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
package app.hxreborn.extension.readera;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class SpeechFilterSettings {

    private static final String PREFERENCES = "hx_readera_speech";

    private static final String OMITTED_TEXT = "omitted_text";

    private static final String TITLE = "Omit words and symbols";

    private static final int MIN_ROW_HEIGHT_DP = 48;

    private static final int EDITOR_LINES = 6;

    private final Context context;

    private final SharedPreferences preferences;

    private SpeechFilterSettings(Context context) {
        this.context = context;
        this.preferences = preferences(context);
    }

    public static String filter(Context context, String text) {
        return new SpeechTextFilter(
                preferences(context).getString(OMITTED_TEXT, SpeechTextFilter.DEFAULT_CONFIGURATION))
            .filter(text);
    }

    public static void attach(View root) {
        if (!(root instanceof ScrollView)) {
            return;
        }
        View content = ((ScrollView) root).getChildAt(0);
        if (!(content instanceof LinearLayout)) {
            return;
        }
        LinearLayout rows = (LinearLayout) content;
        if (rows.findViewWithTag(TITLE) != null) {
            return;
        }
        new SpeechFilterSettings(root.getContext()).addRow(rows);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private void addRow(LinearLayout rows) {
        TextView row = new TextView(this.context);
        row.setTag(TITLE);
        row.setFocusable(true);
        row.setText(TITLE);
        TypedValue appearance = new TypedValue();
        this.context.getTheme().resolveAttribute(android.R.attr.textAppearanceListItem, appearance, true);
        row.setTextAppearance(this.context, appearance.resourceId);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View template = rows.getChildAt(0);
        row.setPaddingRelative(template.getPaddingStart(), template.getPaddingTop(), template.getPaddingEnd(),
                template.getPaddingBottom());
        int minimumHeight = Math.round(MIN_ROW_HEIGHT_DP * this.context.getResources().getDisplayMetrics().density);
        row.setMinHeight(Math.max(minimumHeight, template.getMinimumHeight()));
        TypedValue background = new TypedValue();
        this.context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true);
        row.setBackgroundResource(background.resourceId);
        row.setOnClickListener((view) -> edit());
        rows.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void edit() {
        EditText editor = new EditText(this.context);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setMinLines(EDITOR_LINES);
        editor.setGravity(android.view.Gravity.TOP);
        editor.setHint("One word, phrase or symbol per line");
        editor.setContentDescription(TITLE);
        editor.setText(this.preferences.getString(OMITTED_TEXT, SpeechTextFilter.DEFAULT_CONFIGURATION));
        new AlertDialog.Builder(this.context).setTitle(TITLE)
            .setMessage("Enter one word, phrase or symbol per line. Words match regardless of case. "
                    + "Leave empty to read everything.")
            .setView(editor)
            .setPositiveButton("Save", (dialog,
                    which) -> this.preferences.edit().putString(OMITTED_TEXT, editor.getText().toString()).apply())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

}
