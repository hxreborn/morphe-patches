/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
final class FilledField extends LinearLayout {

    private static final int HORIZONTAL_DP = 16;

    private static final int TOP_DP = 8;

    private static final int BOTTOM_DP = 8;

    private static final int UNLABELED_VERTICAL_DP = 16;

    private static final int CORNER_DP = 4;

    private static final int INDICATOR_DP = 1;

    private static final int FOCUSED_INDICATOR_DP = 2;

    private static final int LABEL_SP = 12;

    private static final int INPUT_SP = 16;

    private static final float HINT_ALPHA = 0.6f;

    private final HostPalette palette;

    private final TextView label;

    private final EditText input;

    private final LinearLayout.LayoutParams indicatorParams;

    private final View indicator;

    private boolean error;

    FilledField(Context context, HostPalette palette, String labelText, String hint, int inputType) {
        super(context);
        this.palette = palette;
        setOrientation(VERTICAL);
        final float density = context.getResources().getDisplayMetrics().density;
        final GradientDrawable container = new GradientDrawable();
        container.setColor(palette.surfaceVariant);
        final float corner = CORNER_DP * density;
        container.setCornerRadii(new float[] { corner, corner, corner, corner, 0, 0, 0, 0 });
        setBackground(container);

        this.label = new TextView(context);
        this.label.setText(labelText);
        this.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SP);
        this.label.setPaddingRelative(Math.round(HORIZONTAL_DP * density), Math.round(TOP_DP * density),
                Math.round(HORIZONTAL_DP * density), 0);
        addView(this.label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        this.input = new EditText(context);
        this.input.setBackground(null);
        this.input.setSingleLine(true);
        this.input.setInputType(inputType);
        this.input.setHint(hint);
        this.input.setTextSize(TypedValue.COMPLEX_UNIT_SP, INPUT_SP);
        this.input.setTextColor(palette.keyText);
        this.input.setHintTextColor(HostScreenActivity.withAlpha(palette.onSurfaceVariant, HINT_ALPHA));
        this.input.setPaddingRelative(Math.round(HORIZONTAL_DP * density), 0, Math.round(HORIZONTAL_DP * density),
                Math.round(BOTTOM_DP * density));
        addView(this.input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        if (labelText.isEmpty()) {
            this.label.setVisibility(GONE);
            final int vertical = Math.round(UNLABELED_VERTICAL_DP * density);
            this.input.setPaddingRelative(Math.round(HORIZONTAL_DP * density), vertical,
                    Math.round(HORIZONTAL_DP * density), vertical);
        }

        this.indicator = new View(context);
        this.indicatorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(INDICATOR_DP * density));
        addView(this.indicator, this.indicatorParams);
        this.input.setOnFocusChangeListener((view, focused) -> refreshIndicator());
        refreshIndicator();
    }

    EditText input() {
        return this.input;
    }

    void setError(boolean error) {
        this.error = error;
        refreshIndicator();
    }

    void onChange(Runnable action) {
        this.input.addTextChangedListener(new AfterTextChanged((text) -> action.run()));
    }

    private int indicatorColor(boolean focused) {
        if (this.error) {
            return this.palette.red;
        }
        return focused ? this.palette.accent : this.palette.onSurfaceVariant;
    }

    private void refreshIndicator() {
        final boolean focused = this.input.isFocused();
        final float density = getResources().getDisplayMetrics().density;
        this.indicatorParams.height = Math.round((focused ? FOCUSED_INDICATOR_DP : INDICATOR_DP) * density);
        this.indicator.setLayoutParams(this.indicatorParams);
        final int color = indicatorColor(focused);
        this.indicator.setBackgroundColor(color);
        this.label.setTextColor(color);
    }

}
