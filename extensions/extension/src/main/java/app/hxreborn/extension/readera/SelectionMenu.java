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

import android.animation.StateListAnimator;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

final class SelectionMenu {

    private SelectionMenu() {
    }

    static void addAction(ScrollView selectionMenu, ScrollView quoteMenu, String label, Runnable action) {
        addAction(selectionMenu, label, action);
        addAction(quoteMenu, label, action);
    }

    private static void addAction(ScrollView menu, String label, Runnable action) {
        View content = menu.getChildAt(0);
        if (!(content instanceof LinearLayout)) {
            Log.w(PatchSettings.LOG_TAG, "Selection menu has no action list for " + label);
            return;
        }
        LinearLayout actions = (LinearLayout) content;
        int lastIndex = actions.getChildCount() - 1;
        View last = actions.getChildAt(lastIndex);
        if (!(last instanceof Button)) {
            Log.w(PatchSettings.LOG_TAG, "Selection menu has no template button for " + label);
            return;
        }
        if (actions.findViewWithTag(label) != null) {
            return;
        }
        Button template = (Button) last;
        Button button = new Button(template.getContext());
        button.setTag(label);
        button.setText(label);
        button.setBackground(copyBackground(template));
        button.setTextColor(template.getTextColors());
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, template.getTextSize());
        button.setTypeface(template.getTypeface());
        button.setLetterSpacing(template.getLetterSpacing());
        button.setSingleLine(true);
        button.setEllipsize(template.getEllipsize());
        button.setTransformationMethod(template.getTransformationMethod());
        button.setGravity(template.getGravity());
        button.setPaddingRelative(template.getPaddingStart(), template.getPaddingTop(), template.getPaddingEnd(),
                template.getPaddingBottom());
        button.setMinimumHeight(template.getMinimumHeight());
        button.setMinWidth(template.getMinWidth());
        button.setMinimumWidth(template.getMinimumWidth());
        StateListAnimator animator = template.getStateListAnimator();
        button.setStateListAnimator((animator != null) ? animator.clone() : null);
        button.setElevation(template.getElevation());
        button.setOnClickListener((view) -> action.run());
        actions.addView(button, lastIndex,
                new LinearLayout.LayoutParams((LinearLayout.LayoutParams) template.getLayoutParams()));
    }

    private static Drawable copyBackground(View template) {
        Drawable background = template.getBackground();
        Drawable.ConstantState state = (background != null) ? background.getConstantState() : null;
        if (state == null) {
            return null;
        }
        return state.newDrawable(template.getResources(), template.getContext().getTheme()).mutate();
    }

}
