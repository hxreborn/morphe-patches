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
package app.hxreborn.extension.utorrent;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Switch;

@SuppressWarnings("unused")
public final class HostSwitch extends View.AccessibilityDelegate {

    private static final int DARK_TRACK_COLOR = 0xFF37802B;

    private final View control;

    private final SwitchChangeListener listener;

    private boolean checked;

    @SuppressWarnings("DataFlowIssue")
    HostSwitch(Context context, boolean checked, int accent, SwitchChangeListener listener) {
        this.control = create(context, PatchSettings.isDark(context) ? DARK_TRACK_COLOR : accent);
        this.checked = checked;
        this.listener = listener;
        setChecked(this.control, checked);
        bind(this.control, this);
        this.control.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    View view() {
        return this.control;
    }

    void attach(View row) {
        row.setAccessibilityDelegate(this);
        row.setOnClickListener((view) -> change(!this.checked));
    }

    public void change(boolean checked) {
        this.checked = checked;
        setChecked(this.control, checked);
        this.listener.onChanged(checked);
    }

    @Override
    @SuppressWarnings({ "NullableProblems", "deprecation" })
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(host, info);
        info.setClassName(Switch.class.getName());
        info.setCheckable(true);
        info.setChecked(this.checked);
    }

    private static View create(Context context, int accent) {
        return null;
    }

    private static void setChecked(View control, boolean checked) {
    }

    private static void bind(View control, HostSwitch listener) {
    }

}
