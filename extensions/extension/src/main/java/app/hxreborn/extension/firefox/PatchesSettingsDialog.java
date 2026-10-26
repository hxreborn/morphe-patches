/*
 * Copyright (C) 2026 BlazeFTL
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from BlazeFTL/FTL-Patches:
 * https://github.com/BlazeFTL/FTL-Patches/commit/dd13beaf9ad004d59b5107a33304606581b0687f
 * Commit dd13beaf9ad004d59b5107a33304606581b0687f (2026-10-25),
 * extensions/firefox/src/main/java/app/ftl/extension/firefox/ModSettingsDialog.java
 */
package app.hxreborn.extension.firefox;

import java.util.ArrayList;
import java.util.List;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.firefox.PatchCatalog.Group;
import app.hxreborn.extension.firefox.PatchCatalog.Row;
import app.hxreborn.extension.firefox.SettingsRows.RowView;
import app.hxreborn.extension.shared.Dimensions;
import app.hxreborn.extension.shared.NightMode;

final class PatchesSettingsDialog implements SettingsRows.Listener {

    private static final String TAG = "FirefoxPatches";

    private static final int MAX_WIDTH_DP = 420;

    private static final int TOP_MARGIN_DP = 32;

    private static final int SHEET_RADIUS_DP = 28;

    private static final int SIDE_DP = 12;

    private static final int CHIP_HEIGHT_DP = 32;

    private static final int TOUCH_DP = 48;

    private static final long SLIDE_MS = 260;

    private static final long EXIT_MS = 180;

    private final Activity activity;

    private final Context app;

    private final boolean night;

    private final boolean animateIn;

    private final SettingsStyle palette;

    private final SettingsRows rows;

    private final TimeInterpolator easing = new PathInterpolator(0.23f, 1f, 0.32f, 1f);

    private final List<RowView> rowViews = new ArrayList<>();

    private final List<View> sections = new ArrayList<>();

    private final List<TextView> chips = new ArrayList<>();

    private Dialog dialog;

    private ScrollView body;

    private HorizontalScrollView chipScroll;

    private View sheet;

    private int current = -1;

    private PatchesSettingsDialog(Activity activity, boolean animateIn) {
        this.activity = activity;
        this.animateIn = animateIn;
        this.app = (activity.getApplicationContext() != null) ? activity.getApplicationContext() : activity;
        this.night = NightMode.isActive(activity);
        this.palette = SettingsStyle.of(activity, this.night);
        this.rows = new SettingsRows(activity, this.palette, this);
    }

    static void show(Activity activity) {
        new PatchesSettingsDialog(activity, true).build().show();
    }

    @Override
    public void toggled(RowView view, boolean on) {
        if (view.row.flag == PatchSettings.TAB_NEXT && !on) {
            RowView end = find(PatchSettings.TAB_END);
            if (end != null && end.toggle.isChecked()) {
                this.rows.setChecked(end, false);
            }
        } else if (view.row.flag == PatchSettings.TAB_END && on) {
            RowView next = find(PatchSettings.TAB_NEXT);
            if (next != null && !next.toggle.isChecked()) {
                this.rows.setChecked(next, true);
            }
        } else if (view.row.flag == PatchSettings.AMOLED && this.night) {
            restyle();
        }
    }

    private void restyle() {
        int scroll = this.body.getScrollY();
        Dialog previous = this.dialog;
        PatchesSettingsDialog next = new PatchesSettingsDialog(this.activity, false);
        next.build().show();
        next.body.post(() -> next.body.scrollTo(0, scroll));
        previous.dismiss();
    }

    private RowView find(PatchSettings.Flag flag) {
        for (RowView view : this.rowViews) {
            if (view.row.flag == flag && view.row.kind != PatchCatalog.Kind.MENU_STYLE) {
                return view;
            }
        }
        return null;
    }

    private Dialog build() {
        this.dialog = new Dialog(this.activity);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        List<Group> groups = PatchCatalog.groups(this.app);

        FrameLayout root = new FrameLayout(this.activity);
        root.setOnClickListener((ignored) -> dismissAnimated());
        LinearLayout sheetView = new LinearLayout(this.activity);
        sheetView.setOrientation(LinearLayout.VERTICAL);
        sheetView.setClickable(true);
        float radius = dp(SHEET_RADIUS_DP);
        GradientDrawable background = new GradientDrawable();
        background.setColor(this.palette.sheet);
        background.setCornerRadii(new float[] { radius, radius, radius, radius, 0, 0, 0, 0 });
        sheetView.setBackground(background);
        sheetView.addView(handle());
        sheetView.addView(header(PatchCatalog.patchCount(groups)));
        sheetView.addView(chipRow(groups));
        sheetView.addView(bodyContent(groups),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        int screen = this.activity.getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(Math.min(dp(MAX_WIDTH_DP), screen),
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        sheetParams.topMargin = dp(TOP_MARGIN_DP);
        root.addView(sheetView, sheetParams);
        this.sheet = sheetView;
        this.dialog.setContentView(root);

        Window window = this.dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            window.setDimAmount(this.night ? 0.5f : 0.32f);
            window.setWindowAnimations(0);
        }
        this.dialog.setOnShowListener((ignored) -> slideIn());
        this.dialog.setOnDismissListener((ignored) -> this.rows.hideKeyboard(this.body));
        return this.dialog;
    }

    private void slideIn() {
        if (!this.animateIn || PreviewView.animationsDisabled(this.activity)) {
            return;
        }
        this.sheet.setTranslationY(this.activity.getResources().getDisplayMetrics().heightPixels);
        this.sheet.animate().translationY(0f).setDuration(SLIDE_MS).setInterpolator(this.easing).start();
    }

    private void dismissAnimated() {
        if (PreviewView.animationsDisabled(this.activity)) {
            this.dialog.dismiss();
            return;
        }
        this.sheet.animate()
            .translationY(this.sheet.getHeight())
            .setDuration(EXIT_MS)
            .setInterpolator(this.easing)
            .withEndAction(() -> this.dialog.dismiss())
            .start();
    }

    private View handle() {
        LinearLayout holder = new LinearLayout(this.activity);
        holder.setGravity(Gravity.CENTER_HORIZONTAL);
        holder.setPadding(0, dp(10), 0, dp(4));
        View bar = new View(this.activity);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SettingsStyle.fade(this.palette.weak, 0.4f));
        shape.setCornerRadius(dp(2));
        bar.setBackground(shape);
        holder.addView(bar, new LinearLayout.LayoutParams(dp(32), dp(4)));
        return holder;
    }

    private View header(int count) {
        LinearLayout row = new LinearLayout(this.activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(6), dp(16), dp(8));

        LinearLayout tile = new LinearLayout(this.activity);
        tile.setGravity(Gravity.CENTER);
        GradientDrawable tileShape = new GradientDrawable();
        tileShape.setColor(this.palette.accentContainer);
        tileShape.setCornerRadius(dp(12));
        tile.setBackground(tileShape);
        ImageView icon = new ImageView(this.activity);
        try {
            icon.setImageDrawable(this.activity.getPackageManager().getApplicationIcon(this.activity.getPackageName()));
        } catch (Throwable ex) {
            Log.e(TAG, "app icon failed", ex);
        }
        tile.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));
        row.addView(tile, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this.activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = this.rows.textView("hxreborn patches", 20, this.palette.text, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            title.setAccessibilityHeading(true);
        }
        titles.addView(title);
        titles.addView(this.rows.textView(count + ((count == 1) ? " patch applied" : " patches applied"), 12,
                this.palette.weak, false));
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        titlesParams.setMarginStart(dp(12));
        row.addView(titles, titlesParams);

        TextView done = this.rows.textView("Done", 14, this.palette.accent, true);
        done.setGravity(Gravity.CENTER);
        done.setMinWidth(dp(64));
        done.setPadding(dp(12), 0, dp(12), 0);
        done.setClickable(true);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Color.TRANSPARENT);
        pill.setCornerRadius(dp(20));
        done.setBackground(
                new RippleDrawable(ColorStateList.valueOf(SettingsStyle.fade(this.palette.accent, 0.12f)), pill, pill));
        done.setOnClickListener((ignored) -> dismissAnimated());
        row.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        return row;
    }

    private View chipRow(List<Group> groups) {
        this.chipScroll = new HorizontalScrollView(this.activity);
        this.chipScroll.setHorizontalScrollBarEnabled(false);
        this.chipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout strip = new LinearLayout(this.activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(dp(8), 0, dp(8), dp(4));
        for (int index = 0; index < groups.size(); index++) {
            final int target = index;
            FrameLayout touch = new FrameLayout(this.activity);
            touch.setClickable(true);
            touch.setMinimumHeight(dp(TOUCH_DP));
            touch.setPadding(dp(4), 0, dp(4), 0);
            TextView chip = this.rows.textView(groups.get(index).title, 13, this.palette.weak, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12), 0, dp(12), 0);
            touch.addView(chip, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(CHIP_HEIGHT_DP),
                    Gravity.CENTER_VERTICAL));
            touch.setOnClickListener((ignored) -> jumpTo(target));
            strip.addView(touch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            this.chips.add(chip);
        }
        this.chipScroll.addView(strip);
        return this.chipScroll;
    }

    private void styleChip(int index, boolean selected) {
        TextView chip = this.chips.get(index);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(8));
        if (selected) {
            shape.setColor(this.palette.selected);
            chip.setTextColor(this.palette.onSelected);
        } else {
            shape.setColor(Color.TRANSPARENT);
            shape.setStroke(dp(1), this.palette.outline);
            chip.setTextColor(this.palette.weak);
        }
        chip.setBackground(shape);
    }

    private void jumpTo(int index) {
        int top = this.sections.get(index).getTop();
        if (PreviewView.animationsDisabled(this.activity)) {
            this.body.scrollTo(0, top);
        } else {
            this.body.smoothScrollTo(0, top);
        }
    }

    private View bodyContent(List<Group> groups) {
        this.body = new ScrollView(this.activity);
        this.body.setVerticalScrollBarEnabled(false);
        this.body.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        this.body.setFocusableInTouchMode(true);
        LinearLayout content = new LinearLayout(this.activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(SIDE_DP), 0, dp(SIDE_DP), dp(24));
        for (Group group : groups) {
            LinearLayout section = new LinearLayout(this.activity);
            section.setOrientation(LinearLayout.VERTICAL);
            TextView label = this.rows.textView(group.title, 14, this.palette.accent, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                label.setAccessibilityHeading(true);
            }
            label.setPadding(dp(16), dp(16), dp(16), dp(8));
            section.addView(label);
            for (int index = 0; index < group.rows.size(); index++) {
                Row row = group.rows.get(index);
                RowView view = this.rows.build(row);
                this.rows.applyShape(view, index == 0, index == group.rows.size() - 1);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = (index == 0) ? 0 : dp(2);
                section.addView(view.root, params);
                this.rowViews.add(view);
            }
            content.addView(section);
            this.sections.add(section);
        }
        this.body.addView(content);
        this.body.setOnScrollChangeListener((view, x, y, oldX, oldY) -> spy(y));
        this.body.post(() -> spy(0));
        return this.body;
    }

    private void spy(int scrollY) {
        int active = 0;
        for (int index = 0; index < this.sections.size(); index++) {
            if (this.sections.get(index).getTop() <= scrollY + dp(24)) {
                active = index;
            }
        }
        if (active == this.current) {
            return;
        }
        this.current = active;
        for (int index = 0; index < this.chips.size(); index++) {
            styleChip(index, index == active);
        }
        View chip = (View) this.chips.get(active).getParent();
        this.chipScroll.smoothScrollTo(Math.max(0, chip.getLeft() - dp(16)), 0);
    }

    private int dp(float value) {
        return Dimensions.dp(this.activity, value);
    }

}
