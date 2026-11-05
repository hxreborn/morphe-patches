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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@SuppressWarnings("unused")
public final class DataSaverOption extends View {

    private static final String DATA_SAVER_ROW = "video_rb_resolution_fluent_layout";

    public DataSaverOption(Context context, AttributeSet attributes) {
        super(context, attributes);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!PatchSettings.isPremiumUnlocked() || !(getParent() instanceof View)) {
            return;
        }
        int identifier = getResources().getIdentifier(DATA_SAVER_ROW, "id", getContext().getPackageName());
        View row = (identifier == 0) ? null : ((View) getParent()).findViewById(identifier);
        if (row != null) {
            ViewGroup.LayoutParams parameters = row.getLayoutParams();
            parameters.height = 0;
            row.setLayoutParams(parameters);
        }
    }

}
