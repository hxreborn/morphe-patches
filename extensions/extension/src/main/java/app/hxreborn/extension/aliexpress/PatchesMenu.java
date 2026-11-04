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
package app.hxreborn.extension.aliexpress;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

@SuppressWarnings("unused")
public final class PatchesMenu {

    public static final String SETTINGS_ROW_TITLE = "hxreborn patches";

    private static final String ROW_TAG = "hx_aliexpress_patches_row";

    private static final int MIN_TOUCH_TARGET_DP = 48;

    private PatchesMenu() {
    }

    public static void attach(View root) {
        if (root == null || root.findViewWithTag(ROW_TAG) != null) {
            return;
        }
        Context context = root.getContext();
        int rowId = context.getResources().getIdentifier("rl_version_settings", "id", context.getPackageName());
        int titleId = context.getResources().getIdentifier("tv_version_title", "id", context.getPackageName());
        View reference = root.findViewById(rowId);
        View nativeTitle = root.findViewById(titleId);
        if (reference == null || !(nativeTitle instanceof TextView)
                || !(reference.getParent() instanceof LinearLayout)) {
            Log.w(PatchSettings.LOG_TAG, "Settings row or title not found");
            return;
        }
        Activity activity = activity(context);
        if (activity == null) {
            Log.w(PatchSettings.LOG_TAG, "Settings activity not found");
            return;
        }
        TextView title = (TextView) nativeTitle;
        LinearLayout parent = (LinearLayout) reference.getParent();
        TextView entry = new TextView(context);
        entry.setTag(ROW_TAG);
        entry.setText(SETTINGS_ROW_TITLE);
        entry.setTypeface(title.getTypeface());
        entry.setTextSize(TypedValue.COMPLEX_UNIT_PX, title.getTextSize());
        entry.setTextColor(title.getTextColors());
        entry.setGravity(Gravity.CENTER_VERTICAL);
        ViewGroup.MarginLayoutParams titleParams = (ViewGroup.MarginLayoutParams) title.getLayoutParams();
        entry.setPaddingRelative(reference.getPaddingStart() + titleParams.getMarginStart() + title.getPaddingStart(),
                reference.getPaddingTop(),
                reference.getPaddingEnd() + titleParams.getMarginEnd() + title.getPaddingEnd(),
                reference.getPaddingBottom());
        entry.setMinHeight(Dimensions.dp(context, MIN_TOUCH_TARGET_DP));
        TypedValue background = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true)) {
            entry.setBackgroundResource(background.resourceId);
        }
        entry.setOnClickListener((view) -> new PatchesSettingsDialog(activity, title).show());
        parent.addView(entry, parent.indexOfChild(reference),
                new LinearLayout.LayoutParams((LinearLayout.LayoutParams) reference.getLayoutParams()));
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

}
