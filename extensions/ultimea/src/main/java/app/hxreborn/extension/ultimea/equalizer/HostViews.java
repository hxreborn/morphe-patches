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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.ultimea.HostStyle;

final class HostViews {

    private static final int HOLD_DELAY_MS = 400;

    private static final int REPEAT_MS = 80;

    private static final int TAB_LINE_WIDTH_DP = 80;

    private static final int TAB_LINE_HEIGHT_DP = 2;

    final HostStyle style;

    private final Context context;

    HostViews(Context context) {
        this.context = context;
        this.style = new HostStyle(context);
    }

    int dp(float value) {
        return Math.round(value * this.context.getResources().getDisplayMetrics().density);
    }

    GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    @SuppressWarnings("SameParameterValue")
    RippleDrawable surface(int color, int radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(this.style.ripple), rounded(color, radiusDp),
                rounded(Color.WHITE, radiusDp));
    }

    static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : HostStyle.DISABLED_OPACITY);
    }

    LinearLayout column() {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    LinearLayout row() {
        LinearLayout layout = new LinearLayout(this.context);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setBaselineAligned(false);
        return layout;
    }

    LinearLayout card() {
        LinearLayout card = column();
        card.setPadding(dp(HostStyle.INSET_DP), dp(HostStyle.GAP_DP), dp(HostStyle.INSET_DP), dp(HostStyle.GAP_DP));
        card.setBackground(rounded(this.style.surface, HostStyle.CARD_RADIUS_DP));
        return card;
    }

    TextView text(String value, int size) {
        TextView text = new TextView(this.context);
        text.setText(value);
        text.setTextColor(this.style.text);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    TextView heading(String value) {
        TextView heading = text(value, HostStyle.TITLE_SP);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            heading.setAccessibilityHeading(true);
        }
        return heading;
    }

    Button button(String label) {
        Button button = new Button(this.context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, HostStyle.BODY_SP);
        button.setTextColor(this.style.accent);
        button.setMinWidth(0);
        button.setMinimumWidth(dp(HostStyle.TOUCH_DP));
        button.setMinHeight(dp(HostStyle.TOUCH_DP));
        button.setPadding(0, 0, 0, 0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(this.style.ripple), null,
                rounded(Color.WHITE, HostStyle.CARD_RADIUS_DP)));
        return button;
    }

    Button icon(String label, EqualizerIcon.Glyph glyph) {
        Button button = button("");
        button.setContentDescription(label);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
        button.setIncludeFontPadding(false);
        EqualizerIcon icon = new EqualizerIcon(glyph, this.style.text);
        icon.setBounds(0, 0, dp(HostStyle.TITLE_SP), dp(HostStyle.TITLE_SP));
        int inset = dp((HostStyle.TOUCH_DP - HostStyle.TITLE_SP) / 2f);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(this.style.ripple),
                new InsetDrawable(icon, inset), rounded(Color.WHITE, HostStyle.CARD_RADIUS_DP)));
        button.setGravity(Gravity.CENTER);
        return button;
    }

    Button mode(String label, EqualizerIcon.Glyph glyph) {
        Drawable icon = new EqualizerIcon(this.context, glyph, this.style.accent);
        Button button = new Button(this.context) {
            @Override
            protected void onDraw(Canvas canvas) {
                Paint paint = getPaint();
                paint.setColor(getCurrentTextColor());
                paint.setTextAlign(Paint.Align.CENTER);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                int iconSize = dp(HostStyle.TITLE_SP);
                int gap = dp(HostStyle.GAP_DP);
                float groupHeight = iconSize + gap + metrics.descent - metrics.ascent;
                float top = (getHeight() - groupHeight) / 2;
                int left = (getWidth() - iconSize) / 2;
                icon.setBounds(left, Math.round(top), left + iconSize, Math.round(top) + iconSize);
                icon.draw(canvas);
                CharSequence text = TextUtils.ellipsize(getText(), getPaint(),
                        Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight()), TextUtils.TruncateAt.END);
                canvas.drawText(text.toString(), getWidth() / 2f, top + iconSize + gap - metrics.ascent, paint);
            }
        };
        button.setText(label);
        button.setContentDescription(label);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, HostStyle.BODY_SP);
        button.setTextColor(this.style.accent);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinHeight(dp(HostStyle.TOUCH_DP));
        button.setPadding(dp(HostStyle.GAP_DP), dp(HostStyle.GAP_DP), dp(HostStyle.GAP_DP), dp(HostStyle.GAP_DP));
        return button;
    }

    Button pill(String label) {
        Button button = button(label);
        button.setTextColor(this.style.pillInk);
        int inset = dp((HostStyle.TOUCH_DP - HostStyle.PILL_HEIGHT_DP) / 2f);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(this.style.ripple),
                new InsetDrawable(rounded(this.style.pill, HostStyle.PILL_HEIGHT_DP / 2f), 0, inset, 0, inset),
                new InsetDrawable(rounded(Color.WHITE, HostStyle.PILL_HEIGHT_DP / 2f), 0, inset, 0, inset)));
        return button;
    }

    void tab(Button tab, boolean selected) {
        tab.setSelected(selected);
        tab.setTextColor(selected ? this.style.accent : this.style.text);
        android.graphics.drawable.LayerDrawable underline = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] {
                        rounded(selected ? this.style.selectedSurface : Color.TRANSPARENT, 0) });
        underline.setLayerSize(0, dp(TAB_LINE_WIDTH_DP), dp(TAB_LINE_HEIGHT_DP));
        underline.setLayerGravity(0, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        tab.setBackground(new RippleDrawable(ColorStateList.valueOf(this.style.ripple), underline,
                rounded(Color.WHITE, HostStyle.CARD_RADIUS_DP)));
    }

    @SuppressLint("ClickableViewAccessibility")
    void repeat(Button button, Runnable change) {
        Runnable repeat = new Runnable() {
            @Override
            public void run() {
                if (button.isPressed() && button.isEnabled() && button.isAttachedToWindow()) {
                    change.run();
                    button.postDelayed(this, REPEAT_MS);
                }
            }
        };
        button.setOnClickListener((view) -> change.run());
        button.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                button.setPressed(true);
                button.performClick();
                button.postDelayed(repeat, HOLD_DELAY_MS);
            } else if (action == android.view.MotionEvent.ACTION_UP
                    || action == android.view.MotionEvent.ACTION_CANCEL) {
                button.setPressed(false);
                button.removeCallbacks(repeat);
            } else if (action == android.view.MotionEvent.ACTION_MOVE && (event.getX() < 0 || event.getY() < 0
                    || event.getX() > button.getWidth() || event.getY() > button.getHeight())) {
                button.setPressed(false);
                button.removeCallbacks(repeat);
            }
            return true;
        });
    }

    void addCard(LinearLayout parent, View card) {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.bottomMargin = dp(HostStyle.GAP_DP);
        parent.addView(card, layout);
    }

}
