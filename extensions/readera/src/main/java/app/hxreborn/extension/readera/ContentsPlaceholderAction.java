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

import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

final class ContentsPlaceholderAction {

    private final TextView placeholder;

    private final ContentsPanel panel;

    private final CharSequence stockMessage;

    ContentsPlaceholderAction(TextView placeholder, ContentsPanel panel) {
        this.placeholder = placeholder;
        this.panel = panel;
        this.stockMessage = placeholder.getText();
    }

    void attach() {
        String message = this.stockMessage + "\n\n" + ChapterSpeech.TEN_PAGES;
        SpannableString action = new SpannableString(message);
        TypedArray attributes = this.placeholder.getContext()
            .obtainStyledAttributes(new int[] { android.R.attr.colorAccent });
        int accent = attributes.getColor(0, this.placeholder.getCurrentTextColor());
        attributes.recycle();
        action.setSpan(new ForegroundColorSpan(accent), message.length() - ChapterSpeech.TEN_PAGES.length(),
                message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.placeholder.setText(action);
        this.placeholder.setOnClickListener((view) -> readPages());
    }

    private void readPages() {
        ChapterReader reader = this.panel.contentsReader();
        if (reader == null) {
            return;
        }
        Object start = reader.currentReadingPosition();
        if (!(start instanceof ReadingPosition)) {
            return;
        }
        ChapterSpeech.request(this.placeholder.getContext(), start, null);
        this.panel.closeContents();
        reader.readAloudFrom(start);
    }

}
