/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.readera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

@SuppressWarnings("unused")
public final class HighlightColorPicker {

    private static final String VIEW_TAG = "hx_highlight_color_picker";

    private static final String[] COLOR_NAMES = { "Gray", "Red", "Orange", "Green", "Blue" };

    private static final int[][] COLLAPSED_ORDER = { { 0, 1, 2 }, { 1, 3, 2 }, { 2, 4, 1 }, { 3, 1, 4 }, { 4, 1, 2 } };

    private static final int LIGHT_BACKGROUND = Color.parseColor("#eeeeee");

    private static final int DARK_BACKGROUND = Color.parseColor("#1c3239");

    private final HighlightColorHost host;

    private final FrameLayout root;

    private final View hiddenWhileExpanded;

    private final View shiftedWhileCollapsed;

    private final int collapsedShift;

    private final GradientDrawable background;

    private final FrameLayout collapsed;

    private final Swatch[] collapsedSwatches = new Swatch[3];

    private final LinearLayout expanded;

    private final Swatch[] swatches = new Swatch[COLOR_NAMES.length];

    private HighlightColorPicker(Activity activity, ViewGroup container, View hiddenWhileExpanded,
            View shiftedWhileCollapsed, HighlightColorHost host) {
        this.host = host;
        this.hiddenWhileExpanded = hiddenWhileExpanded;
        this.shiftedWhileCollapsed = shiftedWhileCollapsed;
        this.collapsedShift = dp(activity, 90);
        this.background = new GradientDrawable();
        float radius = dp(activity, 20);
        this.background.setCornerRadii(new float[] { radius, radius, 0, 0, 0, 0, radius, radius });
        this.background.setColor(LIGHT_BACKGROUND);

        this.root = new FrameLayout(activity);
        this.root.setTag(VIEW_TAG);
        this.root.setBackground(this.background);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(activity, 40), Gravity.BOTTOM | Gravity.RIGHT);
        rootParams.rightMargin = dp(activity, 40);
        rootParams.bottomMargin = dp(activity, 23);

        this.collapsed = new FrameLayout(activity);
        this.collapsed.setPadding(dp(activity, 8), 0, 0, 0);
        this.collapsed.setOnClickListener((view) -> expand());
        for (int i = this.collapsedSwatches.length - 1; i >= 0; i--) {
            Swatch swatch = new Swatch(activity, this.host);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(activity, 35), dp(activity, 35),
                    Gravity.CENTER);
            params.leftMargin = dp(activity, 4 * i);
            this.collapsed.addView(swatch, params);
            this.collapsedSwatches[i] = swatch;
        }
        this.root.addView(this.collapsed, new FrameLayout.LayoutParams(dp(activity, 78), dp(activity, 40)));

        this.expanded = new LinearLayout(activity);
        this.expanded.setOrientation(LinearLayout.HORIZONTAL);
        this.expanded.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        this.expanded.setPadding(dp(activity, 8), 0, dp(activity, 20), 0);
        for (int color = 0; color < this.swatches.length; color++) {
            Swatch swatch = new Swatch(activity, this.host);
            swatch.setColor(color);
            swatch.setContentDescription(COLOR_NAMES[color]);
            int selectedColor = color;
            swatch.setOnClickListener((view) -> select(selectedColor));
            this.expanded.addView(swatch, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 40)));
            this.swatches[color] = swatch;
        }
        this.root.addView(this.expanded,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 40)));

        this.root.setVisibility(View.GONE);
        View previous = container.findViewWithTag(VIEW_TAG);
        if (previous != null) {
            container.removeView(previous);
        }
        container.addView(this.root, 0, rootParams);
        collapse();
    }

    public static HighlightColorPicker forQuotes(Activity activity, ViewGroup quoteGroup, ViewGroup dictionaryGroup,
            HighlightColorHost host) {
        return new HighlightColorPicker(activity, quoteGroup, dictionaryGroup.getChildAt(0), null, host);
    }

    public static HighlightColorPicker forDictionary(Activity activity, ViewGroup quoteGroup, ViewGroup dictionaryGroup,
            HighlightColorHost host) {
        ViewGroup dictionaryButtons = (ViewGroup) dictionaryGroup.getChildAt(0);
        return new HighlightColorPicker(activity, dictionaryButtons, null, dictionaryButtons, host);
    }

    public static int readDefaultColor(SharedPreferences preferences, String key) {
        try {
            int color = Integer.parseInt(preferences.getString(key, "0"));
            return Math.max(0, Math.min(COLOR_NAMES.length - 1, color));
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public static void saveDefaultColor(SharedPreferences preferences, String key, int color) {
        preferences.edit().putString(key, Integer.toString(color)).apply();
    }

    public void show() {
        refresh();
        this.root.setVisibility(View.VISIBLE);
    }

    public void hide(boolean selectionCleared) {
        this.root.setVisibility(View.GONE);
        if (selectionCleared) {
            collapse();
        }
    }

    public void setNight(boolean night) {
        this.background.setColor(night ? DARK_BACKGROUND : LIGHT_BACKGROUND);
    }

    public void refresh() {
        int current = this.host.getHighlightColor();
        int[] order = COLLAPSED_ORDER[Math.max(0, Math.min(COLLAPSED_ORDER.length - 1, current))];
        for (int i = 0; i < this.collapsedSwatches.length; i++) {
            this.collapsedSwatches[i].setColor(order[i]);
        }
        for (int color = 0; color < this.swatches.length; color++) {
            this.swatches[color].setChecked(color == current);
        }
    }

    private void select(int color) {
        this.host.setHighlightColor(color);
        refresh();
        collapse();
    }

    private void expand() {
        if (this.hiddenWhileExpanded != null) {
            this.hiddenWhileExpanded.setVisibility(View.GONE);
        }
        setRightMargin(this.shiftedWhileCollapsed, 0);
        this.collapsed.setVisibility(View.GONE);
        this.expanded.setVisibility(View.VISIBLE);
    }

    private void collapse() {
        if (this.hiddenWhileExpanded != null) {
            this.hiddenWhileExpanded.setVisibility(View.VISIBLE);
        }
        setRightMargin(this.shiftedWhileCollapsed, this.collapsedShift);
        this.expanded.setVisibility(View.GONE);
        this.collapsed.setVisibility(View.VISIBLE);
    }

    private static void setRightMargin(View view, int margin) {
        if (view == null) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (params.rightMargin != margin) {
            params.rightMargin = margin;
            view.setLayoutParams(params);
        }
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    @SuppressLint("ViewConstructor")
    private static final class Swatch extends View {

        private final HighlightColorHost host;

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

        private boolean checked;

        Swatch(Context context, HighlightColorHost host) {
            super(context);
            this.host = host;
            this.fill.setColor(Color.WHITE);
            this.outline.setStyle(Paint.Style.STROKE);
            this.outline.setStrokeWidth(dp(context, 1));
            this.outline.setColor(0x33000000);
            this.ring.setColor(Color.WHITE);
            this.ring.setStyle(Paint.Style.STROKE);
            this.ring.setStrokeWidth(dp(context, 2));
        }

        void setColor(int color) {
            this.fill.setColorFilter(this.host.getHighlightColorFilter(color));
            this.ring.setColorFilter(this.host.getHighlightColorFilter(color));
            invalidate();
        }

        void setChecked(boolean checked) {
            this.checked = checked;
            setSelected(checked);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = dp(getContext(), 8);
            canvas.drawCircle(centerX, centerY, radius, this.fill);
            canvas.drawCircle(centerX, centerY, radius, this.outline);
            if (this.checked) {
                canvas.drawCircle(centerX, centerY, dp(getContext(), 14), this.ring);
            }
        }

    }

}
