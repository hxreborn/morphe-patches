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
package app.hxreborn.extension.terabox;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String ENTRY_TAG = "hx_terabox_patches_entry";

    private static final int END_DRAWABLE = 2;

    private PatchesMenu() {
    }

    static int templateRowId() {
        return 0;
    }

    public static void attach(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.findViewWithTag(ENTRY_TAG) != null) {
            return;
        }
        View templateView = activity.findViewById(templateRowId());
        if (!(templateView instanceof TextView) || !(templateView.getParent() instanceof LinearLayout)) {
            Log.w(PatchSettings.LOG_TAG, "Settings row template missing");
            return;
        }
        TextView template = (TextView) templateView;
        LinearLayout parent = (LinearLayout) template.getParent();
        TextView row = new TextView(activity);
        row.setTag(ENTRY_TAG);
        row.setText(SETTINGS_ROW_TITLE);
        row.setTextColor(template.getTextColors());
        row.setTextSize(TypedValue.COMPLEX_UNIT_PX, template.getTextSize());
        row.setTypeface(template.getTypeface());
        row.setGravity(template.getGravity());
        row.setMinHeight(template.getMinHeight());
        Drawable[] templateDrawables = template.getCompoundDrawablesRelative();
        row.setCompoundDrawablePadding(template.getCompoundDrawablePadding());
        row.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
                copyDrawable(templateDrawables[END_DRAWABLE], activity), null);
        row.setPaddingRelative(template.getPaddingStart(), template.getPaddingTop(), template.getPaddingEnd(),
                template.getPaddingBottom());
        row.setBackground(copyDrawable(template.getBackground(), activity));
        row.setOnClickListener((view) -> new PatchesSettingsDialog(activity).show());
        parent.addView(row, parent.indexOfChild(template), new LinearLayout.LayoutParams(template.getLayoutParams()));
    }

    private static Drawable copyDrawable(Drawable drawable, Activity activity) {
        if (drawable == null || drawable.getConstantState() == null) {
            return null;
        }
        return drawable.getConstantState().newDrawable(activity.getResources()).mutate();
    }

}
