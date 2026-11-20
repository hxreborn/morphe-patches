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
package app.hxreborn.extension.bereal;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

@SuppressWarnings({ "unused", "deprecation" })
public final class RetakeCountActivity extends Activity {

    private static final String STATE_COUNT = "count";

    private static final int SCRIM = 0x99000000;

    private static final int BUTTON_PRIMARY_SURFACE = 0xFFFAFAFA;

    private static final int BUTTON_PRIMARY_TEXT = Color.BLACK;

    private static final int BUTTON_OUTLINE = 0xFF565658;

    private static final int RIPPLE_ON_LIGHT = 0x33000000;

    private static final int CARD_RADIUS_DP = 16;

    private static final int BUTTON_RADIUS_DP = 16;

    private static final int TARGET_DP = 48;

    private static final int MAX_COUNT = Integer.MAX_VALUE;

    private static final int MAX_DIGITS = 10;

    private SettingsStyle style;

    private EditText countInput;

    private TextView applyButton;

    private int count;

    private boolean resolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.style = new SettingsStyle(this);
        this.count = (savedInstanceState != null) ? savedInstanceState.getInt(STATE_COUNT) : RetakeCount.realCount();

        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(SCRIM);
        root.setOnClickListener((view) -> finishWith(null));

        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(this.style.rounded(SettingsStyle.CARD_SURFACE, CARD_RADIUS_DP));
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setClickable(true);

        card.addView(this.style.text("Retake count", 20f, SettingsStyle.PRIMARY_TEXT, this.style.fonts.semibold));
        final TextView subtitle = this.style.text("Real count: " + RetakeCount.realCount(), 13f,
                SettingsStyle.SECONDARY_TEXT, this.style.fonts.regular);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        card.addView(subtitle);
        card.addView(stepper());

        final LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(0, dp(20), 0, 0);
        this.applyButton = button("Apply", true, (view) -> finishWith(Integer.valueOf(this.count)));
        buttons.addView(this.applyButton, buttonParams(0));
        buttons.addView(button("Use real count", false, (view) -> finishWith(null)), buttonParams(dp(8)));
        card.addView(buttons);

        final FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(card, cardParams);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(root);
        refresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COUNT, this.count);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!this.resolved && isFinishing()) {
            RetakeCount.choose(null);
        }
    }

    private LinearLayout stepper() {
        final LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.addView(stepButton("−", "Decrease", -1));
        this.countInput = countField();
        final LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(0, dp(56), 1f);
        fieldParams.leftMargin = dp(8);
        fieldParams.rightMargin = dp(8);
        row.addView(this.countInput, fieldParams);
        row.addView(stepButton("+", "Increase", 1));
        return row;
    }

    private EditText countField() {
        final EditText field = new EditText(this);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_DIGITS) });
        field.setSingleLine(true);
        field.setGravity(Gravity.CENTER);
        field.setPadding(0, 0, 0, 0);
        field.setTextColor(SettingsStyle.PRIMARY_TEXT);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
        field.setTypeface(this.style.fonts.semibold);
        field.setBackground(this.style.rounded(SettingsStyle.FIELD_SURFACE, SettingsStyle.FIELD_RADIUS_DP));
        field.setSelectAllOnFocus(true);
        field.setContentDescription("Retake count");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(SettingsStyle.PRIMARY_TEXT);
            cursor.setSize(dp(2), 0);
            field.setTextCursorDrawable(cursor);
        }
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                onCountTyped(text.toString());
            }
        });
        return field;
    }

    private void onCountTyped(String text) {
        final boolean valid = !text.isEmpty();
        if (valid) {
            this.count = (int) Math.min(MAX_COUNT, Long.parseLong(text));
        }
        this.countInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, countTextSize(text.length()));
        this.applyButton.setEnabled(valid);
        this.applyButton.setAlpha(valid ? 1f : 0.4f);
    }

    private static float countTextSize(int digits) {
        if (digits <= 5) {
            return 32f;
        }
        return (digits <= 7) ? 26f : 20f;
    }

    private TextView stepButton(String label, String description, int delta) {
        final TextView button = this.style.text(label, 24f, SettingsStyle.PRIMARY_TEXT, this.style.fonts.medium);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(TARGET_DP));
        button.setMinHeight(dp(TARGET_DP));
        button.setBackground(ripple(SettingsStyle.FIELD_SURFACE, TARGET_DP / 2, SettingsStyle.RIPPLE));
        button.setContentDescription(description);
        button.setClickable(true);
        button.setOnClickListener((view) -> {
            this.count = (int) Math.max(0, Math.min(MAX_COUNT, (long) this.count + delta));
            refresh();
        });
        return button;
    }

    private TextView button(String label, boolean primary, View.OnClickListener listener) {
        final TextView button = this.style.text(label, 16f, primary ? BUTTON_PRIMARY_TEXT : SettingsStyle.PRIMARY_TEXT,
                this.style.fonts.semibold);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(TARGET_DP));
        button.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (primary) {
            button.setBackground(ripple(BUTTON_PRIMARY_SURFACE, BUTTON_RADIUS_DP, RIPPLE_ON_LIGHT));
        } else {
            final GradientDrawable outline = this.style.rounded(Color.TRANSPARENT, BUTTON_RADIUS_DP);
            outline.setStroke(dp(2), BUTTON_OUTLINE);
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(SettingsStyle.RIPPLE), outline,
                    this.style.rounded(Color.WHITE, BUTTON_RADIUS_DP)));
        }
        button.setClickable(true);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams buttonParams(int topMargin) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin;
        return params;
    }

    private void refresh() {
        this.countInput.setText(String.valueOf(this.count));
        this.countInput.setSelection(this.countInput.length());
    }

    private void finishWith(Integer value) {
        this.resolved = true;
        RetakeCount.choose(value);
        finish();
    }

    private Drawable ripple(int surface, int radiusDp, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), this.style.rounded(surface, radiusDp),
                this.style.rounded(Color.WHITE, radiusDp));
    }

    private int dp(float value) {
        return Dimensions.dp(this, value);
    }

}
