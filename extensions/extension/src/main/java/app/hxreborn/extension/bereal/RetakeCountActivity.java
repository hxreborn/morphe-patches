/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    private static final int MAX_COUNT = 99;

    private SettingsStyle style;

    private TextView countView;

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
        buttons.addView(button("Apply", true, (view) -> finishWith(Integer.valueOf(this.count))), buttonParams(0));
        buttons.addView(button("Use real count", false, (view) -> finishWith(null)), buttonParams(dp(8)));
        card.addView(buttons);

        final FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(dp(320),
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        root.addView(card, cardParams);
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
        this.countView = this.style.text("", 32f, SettingsStyle.PRIMARY_TEXT, this.style.fonts.semibold);
        this.countView.setGravity(Gravity.CENTER);
        this.countView.setMinWidth(dp(96));
        row.addView(this.countView);
        row.addView(stepButton("+", "Increase", 1));
        return row;
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
            this.count = Math.max(0, Math.min(MAX_COUNT, this.count + delta));
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
        this.countView.setText(String.valueOf(this.count));
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
