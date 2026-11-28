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
package app.hxreborn.extension.protonvpn;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.proton.PatchesTheme;

final class SplitTunnelingAppRow extends LinearLayout {

    private static final int ICON_SIZE_DP = 40;

    private static final int ICON_GAP_DP = 16;

    private static final int ROW_PADDING_DP = 8;

    private static final int NAME_TEXT_SP = 16;

    private final PackageManager packageManager;

    private final ImageView icon;

    private final TextView name;

    SplitTunnelingAppRow(Activity activity) {
        super(activity);
        this.packageManager = activity.getPackageManager();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        final int padding = PatchesTheme.dpToPx(activity, ROW_PADDING_DP);
        setPadding(0, padding, 0, padding);

        final int iconSize = PatchesTheme.dpToPx(activity, ICON_SIZE_DP);
        this.icon = new ImageView(activity);
        addView(this.icon, new LayoutParams(iconSize, iconSize));

        this.name = new TextView(activity);
        this.name.setTextSize(TypedValue.COMPLEX_UNIT_SP, NAME_TEXT_SP);
        this.name.setTextColor(PatchesTheme.textNorm(activity));
        final LayoutParams nameParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = PatchesTheme.dpToPx(activity, ICON_GAP_DP);
        addView(this.name, nameParams);
    }

    void bind(SplitTunnelingApp app) {
        this.name.setText(app.label);
        this.icon.setImageDrawable(iconOf(app.packageName));
    }

    private Drawable iconOf(String packageName) {
        try {
            return this.packageManager.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.w(VpnSettings.LOG_TAG, "App icon lookup failed for " + packageName, ex);
            return this.packageManager.getDefaultActivityIcon();
        }
    }

}
