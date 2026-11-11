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

import android.app.Activity;
import android.widget.ScrollView;

@SuppressWarnings("unused")
public final class SelectionSource {

    static final String TITLE = "Copy with source";

    private static final String CLIP_LABEL = "quote-from-doc";

    private SelectionSource() {
    }

    public static void attach(SelectionToolbar toolbar, ScrollView selectionMenu, ScrollView quoteMenu) {
        SelectionMenu.addAction(selectionMenu, quoteMenu, TITLE, () -> copy(toolbar));
    }

    static String withSource(String source, String text) {
        return (source == null || source.isEmpty()) ? text : source + "\n\n" + text;
    }

    private static void copy(SelectionToolbar toolbar) {
        String text = toolbar.selectionText(true);
        if (text.isEmpty()) {
            return;
        }
        Activity activity = toolbar.selectionActivity();
        String source = documentSource(activity);
        toolbar.closeSelection();
        copyText(activity, CLIP_LABEL, withSource(source, text));
    }

    static String documentSource(Activity activity) {
        return "";
    }

    static void copyText(Activity activity, String label, String text) {
    }

}
