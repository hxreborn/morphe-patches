/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hxreborn.extension.photoeditorpro.AiTrace.LiveRow;

final class LiveOverlay {

    private static final String TAG = "hxreborn/overlay";

    private static final int PANEL_TAG = TAG.hashCode();

    private static final Object PULSING = new Object();

    private static final int PANEL_BG = Color.parseColor("#A60C0C0F");

    private static final int PATH_FG = Color.parseColor("#D6D8DB");

    private static final float PATH_SP = 10f;

    private static final float CHIP_SP = 8.5f;

    private static final int PANEL_RADIUS_DP = 10;

    private static final int PANEL_PAD_DP = 10;

    private static final int PANEL_MARGIN_DP = 12;

    private static final int CHIP_RADIUS_DP = 5;

    private static final int CHIP_PAD_H_DP = 5;

    private static final int ROW_GAP_DP = 3;

    private static final int CHIP_GAP_DP = 4;

    private static final int PATH_GAP_DP = 6;

    private static final int CHIP_STROKE_DP = 1;

    private static final int METHOD_COL_DP = 40;

    private static final long PULSE_MS = 350;

    private static final String[] PULSE = { "·  ", "·· ", "···" };

    private static final String UNOBSERVED = "···";

    private static final Handler PULSE_HANDLER = new Handler(Looper.getMainLooper());

    private static final Runnable HIDE = LiveOverlay::hide;

    private static WeakReference<LinearLayout> panel;

    private static String rendered;

    private static String structure;

    private static int pulseStep;

    private static final Runnable PULSE_TICK = new Runnable() {
        @Override
        public void run() {
            LinearLayout view = attachedPanel();
            if (view == null) {
                return;
            }
            pulseStep = (pulseStep + 1) % PULSE.length;
            boolean pulsing = false;
            for (int i = 0; i < view.getChildCount(); i++) {
                if (!(view.getChildAt(i) instanceof LinearLayout row)) {
                    continue;
                }
                TextView status = statusOf(row);
                if (status.getTag() == PULSING) {
                    status.setText(PULSE[pulseStep]);
                    pulsing = true;
                }
            }
            if (pulsing) {
                PULSE_HANDLER.postDelayed(this, PULSE_MS);
            }
        }
    };

    private LiveOverlay() {
    }

    static void render(View anchor, List<LiveRow> rows, String note) {
        try {
            LinearLayout view = panelFor(anchor);
            if (view == null) {
                return;
            }
            String signature = signature(rows, note);
            if (signature.equals(rendered) && view.getVisibility() == View.VISIBLE) {
                return;
            }
            rendered = signature;
            PULSE_HANDLER.removeCallbacks(HIDE);
            Context context = view.getContext();

            String shape = structure(rows, note);
            if (shape.equals(structure)) {
                for (int i = 0; i < rows.size(); i++) {
                    update((LinearLayout) view.getChildAt(i), rows.get(i));
                }
            } else {
                structure = shape;
                view.removeAllViews();
                if (rows.isEmpty()) {
                    view.addView(mono(context, note, PatchPanel.DIM, PATH_SP));
                }
                for (LiveRow call : rows) {
                    view.addView(row(context, call), rowParams(context, view.getChildCount() > 0));
                }
            }
            view.setVisibility(View.VISIBLE);
            schedulePulse(anyInFlight(rows));
        } catch (Exception ex) {
            Log.w(TAG, "render", ex);
        }
    }

    static void linger(long delayMs) {
        PULSE_HANDLER.removeCallbacks(HIDE);
        PULSE_HANDLER.postDelayed(HIDE, delayMs);
    }

    static void hide() {
        try {
            schedulePulse(false);
            PULSE_HANDLER.removeCallbacks(HIDE);
            rendered = null;
            structure = null;
            LinearLayout view = (panel != null) ? panel.get() : null;
            if (view != null) {
                view.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            Log.w(TAG, "hide", ex);
        }
    }

    private static void schedulePulse(boolean pulsing) {
        PULSE_HANDLER.removeCallbacks(PULSE_TICK);
        if (pulsing) {
            PULSE_HANDLER.postDelayed(PULSE_TICK, PULSE_MS);
        }
    }

    private static boolean anyInFlight(List<LiveRow> rows) {
        for (LiveRow row : rows) {
            if (row.inFlight()) {
                return true;
            }
        }
        return false;
    }

    private static LinearLayout attachedPanel() {
        LinearLayout view = (panel != null) ? panel.get() : null;
        if (view == null || !view.isAttachedToWindow() || view.getVisibility() != View.VISIBLE) {
            return null;
        }
        return view;
    }

    private static String signature(List<LiveRow> rows, String note) {
        StringBuilder key = new StringBuilder(note);
        for (LiveRow row : rows) {
            key.append('\0')
                .append(row.method)
                .append('\0')
                .append(row.path)
                .append('\0')
                .append(row.status())
                .append('\0')
                .append(row.count)
                .append('\0')
                .append(row.statusCode);
        }
        return key.toString();
    }

    private static String structure(List<LiveRow> rows, String note) {
        StringBuilder key = new StringBuilder((rows.isEmpty()) ? note : "");
        for (LiveRow row : rows) {
            key.append('\0').append(row.method).append('\0').append(row.path);
        }
        return key.toString();
    }

    private static LinearLayout panelFor(View anchor) {
        LinearLayout existing = (panel != null) ? panel.get() : null;
        if (existing != null && existing.isAttachedToWindow()) {
            return existing;
        }
        if (!(anchor.getContext() instanceof Activity activity)) {
            return null;
        }
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            Log.w(TAG, "no content root in " + activity.getClass().getName());
            return null;
        }

        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View child = root.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals(PANEL_TAG)) {
                root.removeViewAt(i);
            }
        }

        structure = null;
        LinearLayout view = new LinearLayout(activity);
        view.setTag(PANEL_TAG);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setClickable(false);
        view.setFocusable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setBackground(rounded(activity, PANEL_BG, PANEL_RADIUS_DP));
        int pad = PatchPanel.dp(activity, PANEL_PAD_DP);
        view.setPadding(pad, pad, pad, pad);
        view.setLayoutParams(panelParams(activity));

        root.addView(view);
        applyNavigationBarMargin(view);
        panel = new WeakReference<>(view);
        return view;
    }

    private static FrameLayout.LayoutParams panelParams(Context context) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        Position position = PatchSettings.OVERLAY_POSITION.get();
        int margin = PatchPanel.dp(context, PANEL_MARGIN_DP);
        params.gravity = position.gravity | Gravity.START;
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = (position != Position.TOP) ? margin : statusBarHeight(context) + margin;
        params.bottomMargin = margin;
        return params;
    }

    private static void applyNavigationBarMargin(LinearLayout view) {
        view.post(() -> {
            WindowInsets insets = view.getRootWindowInsets();
            if (insets == null) {
                return;
            }
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.bottomMargin = PatchPanel.bottomInset(insets) + PatchPanel.dp(view.getContext(), PANEL_MARGIN_DP);
            view.setLayoutParams(params);
        });
    }

    private static int statusBarHeight(Context context) {
        int id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return (id > 0) ? context.getResources().getDimensionPixelSize(id) : PatchPanel.dp(context, 24);
    }

    private static LinearLayout row(Context context, LiveRow call) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = (PatchPanel.isHttpMethod(call.method))
                ? chip(context, call.method, PatchPanel.methodColour(call.method))
                : outlinedChip(context, call.method, PatchPanel.methodColour(call.method));
        badge.setMinWidth(PatchPanel.dp(context, METHOD_COL_DP));
        badge.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        row.addView(badge, wrap(0));

        TextView path = mono(context, call.path, PATH_FG, PATH_SP);
        path.setSingleLine(true);
        path.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        int gap = PatchPanel.dp(context, PATH_GAP_DP);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        pathParams.leftMargin = gap;
        pathParams.rightMargin = gap;
        row.addView(path, pathParams);

        int chipGap = PatchPanel.dp(context, CHIP_GAP_DP);
        row.addView(chip(context, "", PatchPanel.NEUTRAL), wrap(chipGap));
        row.addView(chip(context, "", PatchPanel.NEUTRAL), wrap(chipGap));
        update(row, call);
        return row;
    }

    private static void update(LinearLayout row, LiveRow call) {
        TextView count = (TextView) row.getChildAt(2);
        boolean repeated = call.count > 1;
        count.setVisibility((repeated) ? View.VISIBLE : View.GONE);
        if (repeated) {
            count.setText("×" + call.count);
        }

        TextView status = statusOf(row);
        status.setTag((call.inFlight()) ? PULSING : null);
        if (call.unobserved()) {
            fill(status, UNOBSERVED, PatchPanel.NEUTRAL);
        } else if (call.inFlight()) {
            fill(status, PULSE[pulseStep], PatchPanel.NEUTRAL);
        } else {
            fill(status, call.status(), PatchPanel.statusColour(call.statusCode));
        }
    }

    private static TextView statusOf(LinearLayout row) {
        return (TextView) row.getChildAt(row.getChildCount() - 1);
    }

    private static void fill(TextView chip, String text, int background) {
        chip.setText(text);
        chip.setTextColor(PatchPanel.chipTextColour(background));
        chip.setBackground(rounded(chip.getContext(), background, CHIP_RADIUS_DP));
    }

    private static LinearLayout.LayoutParams wrap(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = leftMargin;
        return params;
    }

    private static LinearLayout.LayoutParams rowParams(Context context, boolean stacked) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (stacked) ? PatchPanel.dp(context, ROW_GAP_DP) : 0;
        return params;
    }

    private static TextView mono(Context context, String text, int colour, float sp) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(colour);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTypeface(Typeface.MONOSPACE);
        return view;
    }

    private static TextView chip(Context context, String text, int background) {
        TextView view = chipLabel(context, text, PatchPanel.chipTextColour(background));
        view.setBackground(rounded(context, background, CHIP_RADIUS_DP));
        return view;
    }

    private static TextView outlinedChip(Context context, String text, int outline) {
        TextView view = chipLabel(context, text, outline);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.TRANSPARENT);
        shape.setStroke(PatchPanel.dp(context, CHIP_STROKE_DP), outline);
        shape.setCornerRadius(PatchPanel.dp(context, CHIP_RADIUS_DP));
        view.setBackground(shape);
        return view;
    }

    private static TextView chipLabel(Context context, String text, int colour) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(colour);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, CHIP_SP);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        int padH = PatchPanel.dp(context, CHIP_PAD_H_DP);
        view.setPadding(padH, PatchPanel.dp(context, 1), padH, PatchPanel.dp(context, 2));
        return view;
    }

    private static GradientDrawable rounded(Context context, int colour, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(PatchPanel.dp(context, radiusDp));
        return shape;
    }

    enum Position {

        TOP(Gravity.TOP, "Top"), MIDDLE(Gravity.CENTER_VERTICAL, "Middle"), BOTTOM(Gravity.BOTTOM, "Bottom");

        final int gravity;

        private final String label;

        Position(int gravity, String label) {
            this.gravity = gravity;
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }

    }

    enum Linger {

        BRIEF(1500, "1.5 seconds"), NORMAL(3000, "3 seconds"), LONG(6000, "6 seconds"), EXTENDED(12000, "12 seconds");

        final long millis;

        private final String label;

        Linger(long millis, String label) {
            this.millis = millis;
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }

    }

}
