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
package app.hxreborn.extension.protonmail.accountswipe;

import java.util.ArrayList;
import java.util.List;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchesDialog;
import app.hxreborn.extension.proton.PatchesTheme;

public final class AvatarSwitchStyleDialog {

    public static final String TITLE = "Swipe animation";

    private static final String HINT = "Swipe down on your avatar for the next account, up for the previous one.";

    private static final String SEARCH_PLACEHOLDER = "Search messages";

    private static final long CYCLE_MS = 2000L;

    private static final long FORWARD_START_MS = 400L;

    private static final long BACKWARD_START_MS = 1400L;

    private static final int AVATAR_DP = 40;

    private static final int PREVIEW_PADDING_DP = 16;

    private static final int PREVIEW_RADIUS_DP = 16;

    private static final int SEARCH_RADIUS_DP = 20;

    private static final int SEARCH_PADDING_DP = 16;

    private static final int SECTION_GAP_DP = 16;

    private static final int ROW_MIN_HEIGHT_DP = 48;

    private static final int ROW_GAP_DP = 12;

    private static final int LABEL_SP = 16;

    private static final int BODY_SP = 14;

    private static final int BUTTON_SP = 14;

    private static final float SECOND_AVATAR_HUE_SHIFT = 150f;

    private AvatarSwitchStyleDialog() {

    }

    public static void show(Activity activity, Runnable onChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new Chooser(activity, onChanged).show();
    }

    private static int dp(Activity activity, int value) {
        return PatchesTheme.dpToPx(activity, value);
    }

    private static int shiftHue(int color) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + SECOND_AVATAR_HUE_SHIFT) % 360f;
        return Color.HSVToColor(hsv);
    }

    private static final class Chooser {

        private final Activity activity;

        private final Runnable onChanged;

        private final int surfaceColor;

        private final int previewColor;

        private final int textNormColor;

        private final int textWeakColor;

        private final int accentColor;

        private final List<RadioButton> radios = new ArrayList<>();

        private AvatarSwitchStyle pending = AccountAvatarSwipe.getSwitchStyle();

        Chooser(Activity activity, Runnable onChanged) {
            this.activity = activity;
            this.onChanged = onChanged;
            this.surfaceColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_SECONDARY);
            this.previewColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.BACKGROUND_NORM);
            this.textNormColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_NORM);
            this.textWeakColor = PatchesTheme.resolveColorAttribute(activity, PatchesTheme.TEXT_WEAK);
            this.accentColor = AccentColor.getAccentColor(PatchesTheme.isDark(this.surfaceColor));
        }

        void show() {
            final LinearLayout surface = PatchesDialog.createContentLayout(this.activity);
            surface.addView(PatchesDialog.createTitle(this.activity, TITLE));

            final LinearLayout content = new LinearLayout(this.activity);
            content.setOrientation(LinearLayout.VERTICAL);
            final AvatarSlotView avatar = new AvatarSlotView(this.activity, this.previewColor);
            content.addView(previewBar(avatar), topMargin(SECTION_GAP_DP));
            content.addView(PatchesDialog.createTextView(this.activity, HINT, BODY_SP, this.textWeakColor),
                    topMargin(ROW_GAP_DP));

            final LinearLayout options = new LinearLayout(this.activity);
            options.setOrientation(LinearLayout.VERTICAL);
            for (AvatarSwitchStyle style : AvatarSwitchStyle.values()) {
                options.addView(optionRow(style));
            }
            content.addView(options, topMargin(ROW_GAP_DP));

            final ScrollView scroll = new ScrollView(this.activity);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.addView(content);
            surface.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            final Dialog dialog = PatchesDialog.createDialog(this.activity, surface);
            final LinearLayout actions = PatchesDialog.createButtonRow(this.activity);
            actions.addView(PatchesDialog.configureActionButton(this.activity,
                    PatchesDialog.createTextView(this.activity, "Cancel", BUTTON_SP, this.textWeakColor),
                    dialog::dismiss));
            actions.addView(PatchesDialog.configureActionButton(this.activity,
                    PatchesDialog.createTextView(this.activity, "Apply", BUTTON_SP, this.accentColor), () -> {
                        dialog.dismiss();
                        if (this.pending != AccountAvatarSwipe.getSwitchStyle()) {
                            AccountAvatarSwipe.setSwitchStyle(this.pending);
                            this.onChanged.run();
                        }
                    }));
            surface.addView(actions, topMargin(ROW_GAP_DP));

            final ValueAnimator loop = previewLoop(avatar);
            dialog.setOnDismissListener((ignored) -> loop.cancel());
            dialog.show();
            loop.start();
        }

        private View previewBar(AvatarSlotView avatar) {
            final LinearLayout bar = new LinearLayout(this.activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setBackground(PatchesTheme.createRoundedRectangle(this.activity, this.previewColor, PREVIEW_RADIUS_DP));
            final int padding = dp(this.activity, PREVIEW_PADDING_DP);
            bar.setPadding(padding, padding, padding, padding);
            bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

            final TextView search = PatchesDialog.createTextView(this.activity, SEARCH_PLACEHOLDER, BODY_SP,
                    this.textWeakColor);
            search.setSingleLine(true);
            search.setGravity(Gravity.CENTER_VERTICAL);
            search.setPadding(dp(this.activity, SEARCH_PADDING_DP), 0, dp(this.activity, SEARCH_PADDING_DP), 0);
            search
                .setBackground(PatchesTheme.createRoundedRectangle(this.activity, this.surfaceColor, SEARCH_RADIUS_DP));
            bar.addView(search, new LinearLayout.LayoutParams(0, dp(this.activity, AVATAR_DP), 1f));

            final int avatarPx = dp(this.activity, AVATAR_DP);
            final LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(avatarPx, avatarPx);
            avatarParams.setMarginStart(dp(this.activity, ROW_GAP_DP));
            bar.addView(avatar, avatarParams);
            return bar;
        }

        private ValueAnimator previewLoop(AvatarSlotView avatar) {
            final int avatarPx = dp(this.activity, AVATAR_DP);
            final Bitmap first = AvatarSlotView.drawAvatar(this.activity, avatarPx, avatarPx, 0, this.accentColor,
                    Color.WHITE, "A");
            final Bitmap second = AvatarSlotView.drawAvatar(this.activity, avatarPx, avatarPx, 0,
                    shiftHue(this.accentColor), Color.WHITE, "B");

            final ValueAnimator loop = ValueAnimator.ofFloat(0f, CYCLE_MS);
            loop.setDuration(CYCLE_MS);
            loop.setInterpolator(new LinearInterpolator());
            loop.setRepeatCount(ValueAnimator.INFINITE);
            loop.addUpdateListener((update) -> {
                final float elapsed = (float) update.getAnimatedValue();
                final boolean forward = elapsed < BACKWARD_START_MS;
                final float start = (forward) ? FORWARD_START_MS : BACKWARD_START_MS;
                final float linear = Math.min(1f, Math.max(0f, (elapsed - start) / AvatarSlotView.TRANSITION_MS));
                avatar.setAvatars((forward) ? first : second, (forward) ? second : first);
                avatar.setFrame(this.pending, AvatarSlotView.EASING.getInterpolation(linear), (forward) ? 1 : -1);
            });
            return loop;
        }

        private View optionRow(AvatarSwitchStyle style) {
            final LinearLayout row = new LinearLayout(this.activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(this.activity, ROW_MIN_HEIGHT_DP));

            final RadioButton radio = new RadioButton(this.activity);
            radio.setTag(style);
            radio.setChecked(style == this.pending);
            radio.setClickable(false);
            radio.setFocusable(false);
            radio.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            radio.setButtonTintList(ColorStateList.valueOf(this.accentColor));
            this.radios.add(radio);
            row.addView(radio);

            final TextView label = PatchesDialog.createTextView(this.activity, style.getLabel(), LABEL_SP,
                    this.textNormColor);
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            final LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            labelParams.setMarginStart(dp(this.activity, ROW_GAP_DP));
            row.addView(label, labelParams);

            row.setContentDescription(style.getLabel());
            row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(radio.isChecked());
                }
            });
            PatchesTheme.makeClickable(row);
            row.setOnClickListener((ignored) -> select(style));
            return row;
        }

        private void select(AvatarSwitchStyle style) {
            this.pending = style;
            for (RadioButton radio : this.radios) {
                radio.setChecked(radio.getTag() == style);
            }
        }

        private LinearLayout.LayoutParams topMargin(int marginDp) {
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(this.activity, marginDp);
            return params;
        }

    }

}
