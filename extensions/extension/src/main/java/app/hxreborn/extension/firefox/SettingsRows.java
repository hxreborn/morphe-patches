/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import app.hxreborn.extension.firefox.PatchCatalog.Kind;
import app.hxreborn.extension.firefox.PatchCatalog.Row;
import app.hxreborn.extension.firefox.PreviewView.Scene;
import app.hxreborn.extension.shared.Dimensions;

final class SettingsRows {

    private static final String TAG = "FirefoxPatches";

    private static final int ROW_PADDING_DP = 16;

    private static final int ROW_VERTICAL_DP = 14;

    private static final int PREVIEW_GAP_DP = 12;

    private static final int FIELD_HEIGHT_DP = 56;

    private static final int FIELD_RADIUS_DP = 4;

    private static final float DISABLED_ALPHA = 0.38f;

    private static final String HELP = "Starts with https:// or http://";

    private static final String HELP_ERROR = "Enter an address starting with https:// or http://";

    private final Activity activity;

    private final Context appContext;

    private final SettingsStyle palette;

    private final TimeInterpolator easing = new PathInterpolator(0.23f, 1f, 0.32f, 1f);

    private final Listener listener;

    SettingsRows(Activity activity, SettingsStyle palette, Listener listener) {
        this.activity = activity;
        this.appContext = (activity.getApplicationContext() != null) ? activity.getApplicationContext() : activity;
        this.palette = palette;
        this.listener = listener;
    }

    RowView build(Row row) {
        RowView view = new RowView(row);
        LinearLayout root = new LinearLayout(this.activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(ROW_PADDING_DP), dp(ROW_VERTICAL_DP), dp(ROW_PADDING_DP), dp(ROW_VERTICAL_DP));
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.root = root;
        if (row.kind == Kind.MENU_STYLE) {
            menuStyle(view, root);
        } else {
            switchRow(view, root);
        }
        return view;
    }

    void applyShape(RowView view, boolean first, boolean last) {
        float top = dp(first ? 28 : 0);
        float bottom = dp(last ? 28 : 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(this.palette.card);
        shape.setCornerRadii(new float[] { top, top, top, top, bottom, bottom, bottom, bottom });
        if (view.row.kind == Kind.MENU_STYLE) {
            view.root.setBackground(shape);
            return;
        }
        view.root.setBackground(
                new RippleDrawable(ColorStateList.valueOf(SettingsStyle.fade(this.palette.text, 0.12f)), shape, shape));
    }

    void setChecked(RowView view, boolean checked) {
        view.syncing = true;
        view.toggle.setChecked(checked);
        view.preview.setState(checked, true);
        view.syncing = false;
    }

    private void switchRow(final RowView view, LinearLayout root) {
        final Row row = view.row;
        LinearLayout top = new LinearLayout(this.activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setMinimumHeight(dp(48));

        LinearLayout column = new LinearLayout(this.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(textView(row.title, 16, this.palette.text, false));
        TextView effect = textView(row.effect, 14, this.palette.weak, false);
        effect.setSingleLine(true);
        effect.setEllipsize(TextUtils.TruncateAt.END);
        column.addView(effect);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        columnParams.setMarginEnd(dp(16));
        top.addView(column, columnParams);

        view.toggle = newSwitch();
        boolean checked = checkedState(row);
        view.toggle.setChecked(checked);
        view.toggle.setContentDescription(row.title + ". " + row.spoken);
        top.addView(view.toggle);
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        View preview;
        if (row.kind == Kind.LOADING) {
            LoadingPreview loading = new LoadingPreview(this.activity, this.palette, this.easing, row.scene);
            view.preview = loading;
            preview = loading;
        } else {
            PreviewView plain = new PreviewView(this.activity, this.palette, this.easing, row.scene, false);
            view.preview = plain;
            preview = plain;
        }
        view.preview.setState(checked, false);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        previewParams.topMargin = dp(PREVIEW_GAP_DP);
        root.addView(preview, previewParams);

        if (row.kind == Kind.HOMEPAGE) {
            root.addView(homepageField(view, (PreviewScenes.HomepageScene) row.scene, (PreviewView) preview));
            applyFieldState(view, checked);
        }
        view.toggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (view.syncing) {
                return;
            }
            row.change.apply(this.appContext, isChecked);
            view.preview.setState(isChecked, true);
            applyFieldState(view, isChecked);
            this.listener.toggled(view, isChecked);
        });
        root.setClickable(true);
        root.setOnClickListener((ignored) -> view.toggle.toggle());
    }

    private boolean checkedState(Row row) {
        if (row.flag == PatchSettings.TAB_END) {
            return PatchSettings.TAB_END.saved(this.appContext) && PatchSettings.TAB_NEXT.saved(this.appContext);
        }
        return row.flag.saved(this.appContext);
    }

    private void applyFieldState(RowView view, boolean enabled) {
        if (view.field != null) {
            view.field.setEnabled(enabled);
            view.fieldGroup.setAlpha(enabled ? 1f : DISABLED_ALPHA);
        }
    }

    private View homepageField(final RowView view, final PreviewScenes.HomepageScene scene, final PreviewView preview) {
        LinearLayout group = new LinearLayout(this.activity);
        group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        groupParams.topMargin = dp(PREVIEW_GAP_DP);
        group.setLayoutParams(groupParams);
        group.addView(textView("Homepage URL", 12, this.palette.weak, false));

        final EditText field = new EditText(this.activity);
        field.setText(PatchSettings.savedHomepageUrl(this.appContext));
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        field.setTextColor(this.palette.text);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        field.setHighlightColor(SettingsStyle.fade(this.palette.accent, 0.3f));
        field.setPadding(dp(ROW_PADDING_DP), 0, dp(ROW_PADDING_DP), 0);
        field.setContentDescription("Homepage URL");
        field.setBackground(outline(this.palette.outline, 1));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(this.palette.accent);
            cursor.setSize(dp(2), 0);
            field.setTextCursorDrawable(cursor);
        }
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                dp(FIELD_HEIGHT_DP));
        fieldParams.topMargin = dp(4);
        group.addView(field, fieldParams);

        final TextView support = textView(HELP, 12, this.palette.weak, false);
        support.setPadding(dp(ROW_PADDING_DP), dp(4), dp(ROW_PADDING_DP), 0);
        group.addView(support);

        field.setOnFocusChangeListener((focused, hasFocus) -> {
            boolean valid = PatchSettings.isWebUrl(field.getText().toString());
            field.setBackground(outline(fieldColor(valid, hasFocus), hasFocus ? 2 : 1));
            if (!hasFocus) {
                hideKeyboard(focused);
            }
        });
        field.setOnEditorActionListener((editor, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                editor.clearFocus();
                hideKeyboard(editor);
            }
            return false;
        });
        field.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String value = editable.toString();
                boolean valid = PatchSettings.isWebUrl(value);
                if (valid) {
                    PatchSettings.saveHomepageUrl(SettingsRows.this.appContext, value);
                    scene.setHost(PatchSettings.hostOf(value));
                    preview.refresh();
                }
                support.setText(valid ? HELP : HELP_ERROR);
                support.setTextColor(valid ? SettingsRows.this.palette.weak : SettingsRows.this.palette.error);
                field.setBackground(outline(fieldColor(valid, true), 2));
            }

        });
        view.field = field;
        view.fieldGroup = group;
        return group;
    }

    private int fieldColor(boolean valid, boolean focused) {
        if (!valid) {
            return this.palette.error;
        }
        return focused ? this.palette.accent : this.palette.outline;
    }

    private GradientDrawable outline(int color, int widthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(dp(FIELD_RADIUS_DP));
        drawable.setStroke(dp(widthDp), color);
        return drawable;
    }

    void hideKeyboard(View view) {
        InputMethodManager inputMethods = (InputMethodManager) this.activity
            .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethods != null) {
            inputMethods.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void menuStyle(final RowView view, LinearLayout root) {
        boolean old = PatchSettings.OLD_MENU.saved(this.appContext);
        root.addView(textView(view.row.title, 16, this.palette.text, false));
        final TextView effect = textView(menuText(old), 14, this.palette.weak, false);
        root.addView(effect);
        LinearLayout thumbs = new LinearLayout(this.activity);
        thumbs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams thumbsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        thumbsParams.topMargin = dp(PREVIEW_GAP_DP);
        final Thumb[] choices = new Thumb[2];
        for (int index = 0; index < 2; index++) {
            final boolean classic = index == 0;
            final Thumb thumb = new Thumb(classic ? "Classic popup" : "Bottom sheet",
                    PreviewScenes.menuThumbnail(!classic));
            thumb.setChecked(classic == old, false);
            choices[index] = thumb;
            thumb.setOnClickListener((ignored) -> {
                if (PatchSettings.OLD_MENU.saved(this.appContext) == classic) {
                    return;
                }
                PatchSettings.saveOldMenu(this.appContext, classic);
                effect.setText(menuText(classic));
                choices[0].setChecked(classic, true);
                choices[1].setChecked(!classic, true);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f);
            if (!classic) {
                params.setMarginStart(dp(8));
            }
            thumbs.addView(thumb, params);
        }
        root.addView(thumbs, thumbsParams);
    }

    private static String menuText(boolean old) {
        return old ? "The compact Chrome-style menu from Firefox 141 and earlier"
                : "The redesigned menu since Firefox 142 (Aug 2025)";
    }

    CompoundButton newSwitch() {
        int[][] states = { { android.R.attr.state_checked }, {} };
        ColorStateList thumb = new ColorStateList(states, new int[] { this.palette.onAccent, this.palette.outline });
        ColorStateList track = new ColorStateList(states, new int[] { this.palette.accent, this.palette.barHigh });
        try {
            int layout = this.activity.getResources()
                .getIdentifier("preference_material_switch", "layout", this.activity.getPackageName());
            View view = LayoutInflater.from(this.activity).inflate(layout, null, false);
            Class<?> type = view.getClass();
            type.getMethod("setThumbTintList", ColorStateList.class).invoke(view, thumb);
            type.getMethod("setTrackTintList", ColorStateList.class).invoke(view, track);
            type.getMethod("setTrackDecorationTintList", ColorStateList.class)
                .invoke(view, new ColorStateList(states, new int[] { this.palette.accent, this.palette.outline }));
            return (CompoundButton) view;
        } catch (Throwable ex) {
            Log.e(TAG, "material switch failed", ex);
        }
        Switch toggle = new Switch(this.activity);
        toggle.setThumbTintList(thumb);
        toggle.setTrackTintList(track);
        return toggle;
    }

    TextView textView(String value, int sizeSp, int color, boolean medium) {
        TextView view = new TextView(this.activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (medium) {
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return view;
    }

    private int dp(float value) {
        return Dimensions.dp(this.activity, value);
    }

    interface Listener {

        void toggled(RowView view, boolean on);

    }

    interface Stateful {

        void setState(boolean on, boolean animate);

    }

    static final class RowView {

        final Row row;

        LinearLayout root;

        CompoundButton toggle;

        Stateful preview;

        EditText field;

        LinearLayout fieldGroup;

        boolean syncing;

        RowView(Row row) {
            this.row = row;
        }

    }

    private final class Thumb extends LinearLayout {

        private final PreviewView preview;

        private final RadioDot dot;

        Thumb(String label, Scene scene) {
            super(SettingsRows.this.activity);
            setOrientation(VERTICAL);
            setClickable(true);
            this.preview = new PreviewView(SettingsRows.this.activity, SettingsRows.this.palette,
                    SettingsRows.this.easing, scene, true);
            addView(this.preview,
                    new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout caption = new LinearLayout(SettingsRows.this.activity);
            caption.setGravity(Gravity.CENTER_VERTICAL);
            this.dot = new RadioDot(SettingsRows.this.activity);
            caption.addView(this.dot, new LayoutParams(dp(18), dp(18)));
            TextView name = textView(label, 14, SettingsRows.this.palette.text, true);
            name.setPadding(dp(8), 0, 0, 0);
            caption.addView(name);
            caption.setMinimumHeight(dp(48));
            caption.setPadding(dp(4), 0, 0, 0);
            addView(caption);
            setContentDescription(label + ". Menu style");
        }

        void setChecked(boolean checked, boolean animate) {
            this.preview.setState(checked, animate);
            this.dot.set(checked, animate);
            setSelected(checked);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setCheckable(true);
            info.setChecked(isSelected());
            info.setClassName("android.widget.RadioButton");
        }

    }

    private final class RadioDot extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float progress;

        private ValueAnimator animator;

        RadioDot(Context context) {
            super(context);
        }

        void set(boolean checked, boolean animate) {
            float target = checked ? 1f : 0f;
            if (this.animator != null) {
                this.animator.cancel();
            }
            if (!animate || PreviewView.animationsDisabled(getContext())) {
                this.progress = target;
                invalidate();
                return;
            }
            this.animator = ValueAnimator.ofFloat(this.progress, target);
            this.animator.setDuration(200);
            this.animator.setInterpolator(SettingsRows.this.easing);
            this.animator.addUpdateListener((animation) -> {
                this.progress = (float) animation.getAnimatedValue();
                invalidate();
            });
            this.animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float stroke = dp(2);
            float outer = Math.min(cx, cy) - stroke / 2f;
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(stroke);
            this.paint.setColor(SettingsStyle.mix(SettingsRows.this.palette.accent, SettingsRows.this.palette.outline,
                    this.progress));
            canvas.drawCircle(cx, cy, outer, this.paint);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(SettingsRows.this.palette.accent);
            canvas.drawCircle(cx, cy, dp(4) * this.progress, this.paint);
        }

    }

}
