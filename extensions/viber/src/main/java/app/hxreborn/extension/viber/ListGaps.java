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
package app.hxreborn.extension.viber;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

@SuppressWarnings("unused")
public final class ListGaps implements ViewTreeObserver.OnGlobalLayoutListener {

    private final ViewGroup list;

    private final Rect visibleBounds = new Rect();

    private final boolean originalNestedScrolling;

    private ListGaps(ViewGroup list) {
        this.list = list;
        this.originalNestedScrolling = list.isNestedScrollingEnabled();
    }

    public static int chatFooterHeight(int height) {
        return PatchSettings.removeListGaps() ? 0 : height;
    }

    public static void observeList(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup list = (ViewGroup) view;
            list.getViewTreeObserver().addOnGlobalLayoutListener(new ListGaps(list));
        }
    }

    @Override
    public void onGlobalLayout() {
        boolean nestedScrolling = this.originalNestedScrolling;
        if (PatchSettings.removeListGaps() && this.list.getChildCount() > 0 && !this.list.canScrollVertically(-1)
                && !this.list.canScrollVertically(1) && this.list.getGlobalVisibleRect(this.visibleBounds)) {
            View lastRow = this.list.getChildAt(this.list.getChildCount() - 1);
            int contentBottom = lastRow.getBottom() + this.list.getPaddingBottom();
            int availableHeight = this.visibleBounds.height();
            if (contentBottom > 0 && contentBottom <= availableHeight) {
                nestedScrolling = false;
            }
        }
        if (this.list.isNestedScrollingEnabled() != nestedScrolling) {
            this.list.setNestedScrollingEnabled(nestedScrolling);
        }
    }

}
