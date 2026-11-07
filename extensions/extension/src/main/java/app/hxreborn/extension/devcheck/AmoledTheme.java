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
package app.hxreborn.extension.devcheck;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

@SuppressWarnings("unused")
public final class AmoledTheme {

    private static final String OVERLAY_STYLE = "hx_amoled_theme_overlay";

    private static final String PREFERENCE_ID = "hx_amoled_preference";

    private static final String PREFERENCE_TITLE = "hx_amoled_preference_title";

    private AmoledTheme() {
    }

    static int dialogListId() {
        return 0;
    }

    static int lightThemeAttribute() {
        return 0;
    }

    public static void addPreference(Dialog dialog) {
        Context context = dialog.getContext();
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        int listId = dialogListId();
        int preferenceId = resources.getIdentifier(PREFERENCE_ID, "id", packageName);
        int titleId = resources.getIdentifier(PREFERENCE_TITLE, "string", packageName);
        View listView = dialog.findViewById(listId);
        if (!(listView instanceof ListView) || preferenceId == 0 || titleId == 0) {
            Log.w(PatchSettings.LOG_TAG, "Could not find theme dialog resources");
            return;
        }
        if (dialog.findViewById(preferenceId) != null) {
            return;
        }
        Activity activity = ownerActivity(dialog);
        if (activity == null) {
            Log.w(PatchSettings.LOG_TAG, "Could not find theme dialog activity");
            return;
        }
        ListView list = (ListView) listView;
        ListAdapter adapter = list.getAdapter();
        if (adapter == null || adapter.getCount() == 0) {
            Log.w(PatchSettings.LOG_TAG, "Could not find theme choices");
            return;
        }
        View template = adapter.getView(0, null, list);
        if (!(template instanceof CheckedTextView)) {
            Log.w(PatchSettings.LOG_TAG, "Could not find checkable theme row");
            return;
        }
        TypedArray attributes = context
            .obtainStyledAttributes(new int[] { android.R.attr.listChoiceIndicatorMultiple });
        Drawable checkbox = attributes.getDrawable(0);
        attributes.recycle();
        if (checkbox == null) {
            Log.w(PatchSettings.LOG_TAG, "Could not find theme checkbox drawable");
            return;
        }
        CheckedTextView preference = (CheckedTextView) template;
        preference.setId(preferenceId);
        preference.setText(context.getString(titleId));
        preference.setSingleLine(false);
        preference.setEllipsize(null);
        preference.setCheckMarkDrawable(null);
        preference.setCompoundDrawablesRelativeWithIntrinsicBounds(checkbox.mutate(), null, null, null);
        preference.setChecked(PatchSettings.isAmoled(context));
        preference.setOnClickListener((view) -> {
            boolean enabled = !preference.isChecked();
            PatchSettings.setAmoled(context, enabled);
            preference.setChecked(enabled);
            dialog.dismiss();
            activity.recreate();
        });
        FrameLayout footer = new FrameLayout(context);
        footer.addView(preference,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        list.addFooterView(footer, null, false);
    }

    private static Activity ownerActivity(Dialog dialog) {
        Activity owner = dialog.getOwnerActivity();
        if (owner != null) {
            return owner;
        }
        Context context = dialog.getContext();
        while (context instanceof ContextWrapper && !(context instanceof Activity)) {
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) {
                break;
            }
            context = base;
        }
        return (context instanceof Activity) ? (Activity) context : null;
    }

    public static void applyOverlay(Activity activity) {
        if (!PatchSettings.isAmoled(activity)) {
            return;
        }
        Resources resources = activity.getResources();
        String packageName = activity.getPackageName();
        TypedValue lightTheme = new TypedValue();
        if (!activity.getTheme().resolveAttribute(lightThemeAttribute(), lightTheme, true) || lightTheme.data != 0) {
            return;
        }
        int overlay = resources.getIdentifier(OVERLAY_STYLE, "style", packageName);
        if (overlay == 0) {
            Log.w(PatchSettings.LOG_TAG, "Could not find AMOLED theme overlay");
            return;
        }
        activity.getTheme().applyStyle(overlay, true);
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
    }

}
