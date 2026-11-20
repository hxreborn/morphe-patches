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

import android.view.View;

final class ContentsRowMenu implements View.OnLongClickListener {

    private final View row;

    private final ContentsRow contentsRow;

    ContentsRowMenu(View row, ContentsRow contentsRow) {
        this.row = row;
        this.contentsRow = contentsRow;
    }

    @Override
    public boolean onLongClick(View view) {
        Object entry = this.contentsRow.contentsEntry();
        if (!(entry instanceof ContentsEntry) || !(entry instanceof ReadingPosition)
                || ((ContentsEntry) entry).contentsLink() != null || ((ReadingPosition) entry).readingPage() < 0) {
            return false;
        }
        new MaterialMenu(this.row.getContext(), new SettingsStyle(this.row.getContext())).show(this.row,
                new MaterialMenu.Item(ChapterSpeech.TITLE, "headphones", () -> readChapter(entry)));
        return true;
    }

    private void readChapter(Object entry) {
        ContentsPanel panel = this.contentsRow.contentsPanel();
        ChapterReader reader = panel.contentsReader();
        if (reader == null) {
            return;
        }
        Object start = reader.copyReadingPosition(entry);
        if (start == null) {
            return;
        }
        ChapterSpeech.request(this.row.getContext(), start, entry);
        this.contentsRow.openContentsEntry(this.row);
        reader.readAloudFrom(start);
    }

}
