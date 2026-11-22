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
package app.hxreborn.extension.shared.patchesmenu;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class PatchesSettingsPanel extends PatchesSettingsPanelBase {

    private final int background;

    private final View bar;

    private final ScrollView scroll;

    public PatchesSettingsPanel(Activity activity, int background, View bar, ScrollView scroll) {
        super(activity);
        this.background = background;
        this.bar = bar;
        this.scroll = scroll;
    }

    @Override
    @SuppressWarnings("deprecation")
    public View createContent() {
        LinearLayout screen = createColumn();
        screen.setBackgroundColor(this.background);
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        screen.addView(this.bar);
        addWeightedContent(screen, this.scroll);
        return screen;
    }

}
