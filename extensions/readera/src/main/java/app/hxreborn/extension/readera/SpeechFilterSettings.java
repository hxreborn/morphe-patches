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
import android.widget.EditText;

@SuppressWarnings("unused")
public final class SpeechFilterSettings {

    static final String TITLE = "Omit words and symbols";

    private static final String OMITTED_TEXT = "omitted_text";

    private static final int EDITOR_LINES = 6;

    private final Context context;

    private final SharedPreferences preferences;

    private SpeechFilterSettings(Context context) {
        this.context = context;
        this.preferences = PatchSettings.speechPreferences(context);
    }

    public static String filter(Context context, String text) {
        return savedFilter(context).filter(text);
    }

    static void open(Context context, Runnable saved) {
        new SpeechFilterSettings(context).edit(saved);
    }

    static int count(Context context) {
        return savedFilter(context).entryCount();
    }

    private static SpeechTextFilter savedFilter(Context context) {
        return new SpeechTextFilter(PatchSettings.speechPreferences(context)
            .getString(OMITTED_TEXT, SpeechTextFilter.DEFAULT_CONFIGURATION));
    }

    private void edit(Runnable saved) {
        EditText editor = new EditText(this.context);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setMinLines(EDITOR_LINES);
        editor.setGravity(android.view.Gravity.TOP);
        editor.setHint("One word, phrase or symbol per line");
        editor.setContentDescription(TITLE);
        editor.setText(this.preferences.getString(OMITTED_TEXT, SpeechTextFilter.DEFAULT_CONFIGURATION));
        new AlertDialog.Builder(this.context).setTitle(TITLE)
            .setMessage("One word, phrase or symbol per line. A word matches only as a whole word, in any case. "
                    + "An empty list reads everything.")
            .setView(editor)
            .setPositiveButton("Save", (dialog, which) -> {
                this.preferences.edit().putString(OMITTED_TEXT, editor.getText().toString()).apply();
                saved.run();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

}
