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
package app.hxreborn.extension.ultimea.equalizer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.ultimea.HostStyle;

final class RemoteControls {

    private static final String CONTROL_TAG = "hx_ultimea_remote_control";

    private RemoteControls() {
    }

    static LinearLayout card(HostViews views) {
        LinearLayout card = views.card();
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView label = views.text("Remote controls", HostStyle.BODY_SP);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        label.setCompoundDrawablePadding(views.dp(HostStyle.GAP_DP));
        EqualizerIcon icon = new EqualizerIcon(EqualizerIcon.Glyph.REMOTE, views.style.text);
        icon.setBounds(0, 0, views.dp(HostStyle.TITLE_SP), views.dp(HostStyle.TITLE_SP));
        label.setCompoundDrawables(icon, null, null, null);
        card.addView(label);
        card.setMinimumHeight(0);
        card.setBackground(views.surface(views.style.surface, HostStyle.CARD_RADIUS_DP));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Remote controls");
        return card;
    }

    static void addRows(LinearLayout content, HostViews views, RemoteAdjustment adjustment) {
        TextView note = views.text("Bass, Midrange and Treble each move their whole band group by 0.1 dB per press.",
                HostStyle.CAPTION_SP);
        note.setTextColor(views.style.secondaryText);
        note.setPadding(views.dp(HostStyle.INSET_DP), 0, views.dp(HostStyle.INSET_DP), views.dp(HostStyle.GAP_DP));
        content.addView(note);
        for (EqualizerGroup group : EqualizerGroup.values()) {
            LinearLayout card = views.card();
            LinearLayout row = views.row();
            row.setMinimumHeight(views.dp(HostStyle.VOLUME_ROW_DP - 2 * HostStyle.GAP_DP));
            TextView label = views.text(group.title.replace(" EQ", ""), HostStyle.BODY_SP);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            for (int direction : new int[] { -1, 1 }) {
                Button button = views.pill((direction < 0) ? "−" : "+");
                button.setTag(CONTROL_TAG);
                button.setContentDescription(
                        group.title.replace(" EQ", "") + ((direction < 0) ? " decrease 0.1 dB" : " increase 0.1 dB"));
                views.repeat(button, () -> adjustment.step(group, direction));
                LinearLayout.LayoutParams target = new LinearLayout.LayoutParams(views.dp(HostStyle.PILL_WIDTH_DP),
                        views.dp(HostStyle.TOUCH_DP));
                if (direction > 0) {
                    target.leftMargin = views.dp(HostStyle.PILL_GAP_DP);
                    target.setMarginStart(views.dp(HostStyle.PILL_GAP_DP));
                }
                row.addView(button, target);
            }
            card.addView(row);
            views.addCard(content, card);
        }
    }

    static void setEnabled(View view, boolean enabled) {
        if (CONTROL_TAG.equals(view.getTag())) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : HostStyle.DISABLED_OPACITY);
        }
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            for (int index = 0; index < parent.getChildCount(); index++) {
                setEnabled(parent.getChildAt(index), enabled);
            }
        }
    }

}
