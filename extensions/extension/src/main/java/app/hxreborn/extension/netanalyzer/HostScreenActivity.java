/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.S_V2)
abstract class HostScreenActivity extends Activity {

    static final TextStyle TITLE_LARGE = new TextStyle(22, 28, 0f, TextStyle.MEDIUM);

    static final TextStyle TITLE_SMALL = new TextStyle(14, 20, 0.1f, TextStyle.MEDIUM);

    static final TextStyle BODY_LARGE = new TextStyle(16, 24, 0.5f, TextStyle.NORMAL);

    static final TextStyle BODY_MEDIUM = new TextStyle(14, 20, 0.2f, TextStyle.NORMAL);

    static final TextStyle TOOLBAR_ACTION = new TextStyle(17, 20, 0.1f, TextStyle.MEDIUM);

    private static final TextStyle BODY_SMALL = new TextStyle(12, 16, 0.4f, TextStyle.NORMAL);

    private static final TextStyle LABEL_LARGE_SEMIBOLD = new TextStyle(14, 20, 0.1f, TextStyle.SEMIBOLD);

    private static final int POSITIVE_DOT = 0xFF4CAF50;

    private static final int NEGATIVE_DOT = 0xFFF44336;

    private static final float DOT_FONT_SCALE = 0.7f;

    private static final float CHEVRON_ALPHA = 0.6f;

    private static final float RIPPLE_ALPHA = 0.12f;

    private static final Object ROW_TAG = new Object();

    private static final int INSET_DP = 16;

    private static final int ROW_VERTICAL_DP = 7;

    private static final int ROW_SPACING_DP = 16;

    private static final int VALUE_SPACING_DP = 8;

    private static final int DOT_CHEVRON_GAP_DP = 2;

    private static final int CHEVRON_END_DP = 8;

    private static final int ICON_DP = 24;

    private static final int CLICKABLE_MIN_DP = 48;

    private static final int SECTION_TOP_DP = 12;

    private static final int SECTION_BOTTOM_DP = 8;

    private static final int SUMMARY_SPACING_DP = 4;

    private static final int SWITCH_GAP_DP = 12;

    private static final int TOP_BAR_DP = 64;

    private static final int BACK_BUTTON_INSET_DP = 4;

    private static final int TITLE_GAP_DP = 4;

    private static final int TOOLBAR_END_INSET_DP = 4;

    private static final int TOOLBAR_TEXT_PADDING_DP = 12;

    private static final int SEGMENT_HEIGHT_DP = 40;

    private static final int SEGMENT_BORDER_DP = 1;

    private static final int INTRO_WIDTH_DP = 280;

    private static final int INTRO_VERTICAL_DP = 8;

    HostPalette palette;

    LinearLayout list;

    private ScrollView scroll;

    private LinearLayout topBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        this.palette = HostPalette.of(this);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(this.palette.bar);
        root.addView(topBar(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(TOP_BAR_DP)));

        this.scroll = new ScrollView(this);
        this.scroll.setBackgroundColor(this.palette.header);
        this.list = new LinearLayout(this);
        this.list.setOrientation(LinearLayout.VERTICAL);
        this.scroll.addView(this.list,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(this.scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            final Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        getWindow().setStatusBarColor(this.palette.bar);
        getWindow().setNavigationBarColor(this.palette.bar);
        final int lightBars = this.palette.dark ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(lightBars);
        setContentView(root);
        buildContent();
    }

    abstract String screenTitle();

    abstract void buildContent();

    final void replaceBody(View body) {
        final ViewGroup root = (ViewGroup) this.scroll.getParent();
        final int index = root.indexOfChild(this.scroll);
        root.removeView(this.scroll);
        body.setBackgroundColor(this.palette.background);
        root.addView(body, index, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    final void section(String title) {
        this.list.addView(sectionHeader(title), matchWidth());
    }

    final TextView sectionHeader(String title) {
        final TextView header = text(title, TITLE_SMALL, this.palette.sectionText);
        header.setBackgroundColor(this.palette.header);
        header.setPaddingRelative(dp(INSET_DP), dp(SECTION_TOP_DP) + header.getPaddingTop(), dp(INSET_DP),
                dp(SECTION_BOTTOM_DP) + header.getPaddingBottom());
        return header;
    }

    final TextView introRow(CharSequence message) {
        final TextView intro = text("", BODY_MEDIUM, this.palette.valueText);
        intro.setText(message);
        intro.setGravity(Gravity.END);
        intro.setWidth(dp(INTRO_WIDTH_DP));
        final FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(this.palette.header);
        box.setPaddingRelative(0, dp(INTRO_VERTICAL_DP), dp(INSET_DP), dp(INTRO_VERTICAL_DP));
        box.addView(intro, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END));
        this.list.addView(box, matchWidth());
        return intro;
    }

    final TextView simpleRow(String key, String value) {
        final LinearLayout row = keyRow(key);
        final TextView valueView = value(value);
        row.addView(valueView, weighted(ROW_SPACING_DP));
        addRow(row);
        return valueView;
    }

    final TextView onOffRow(String key, String value, Status status) {
        final LinearLayout row = keyRow(key);
        final TextView valueView = value(value);
        row.addView(valueView, weighted(ROW_SPACING_DP));
        row.addView(dot(status.color(this.palette)), dotParams(VALUE_SPACING_DP));
        addRow(row);
        return valueView;
    }

    final TextView onOffRow(String key, String value) {
        return onOffRow(key, (value != null) ? value : Status.NOT_AVAILABLE,
                (value != null) ? Status.POSITIVE : Status.NEUTRAL);
    }

    final TextView clickableRow(String key, String value, View.OnClickListener onClick) {
        return navigableRow(key, value, null, onClick);
    }

    final TextView onOffClickableRow(String key, String value, View.OnClickListener onClick) {
        return navigableRow(key, value, Status.POSITIVE, onClick);
    }

    final TextView buttonRow(String key, String value, String button, View.OnClickListener onClick) {
        final LinearLayout row = keyRow(key);
        final TextView valueView = value(value);
        row.addView(valueView, weighted(ROW_SPACING_DP));
        final TextView action = text(button, LABEL_LARGE_SEMIBOLD, this.palette.accent);
        action.setSingleLine(true);
        action.setOnClickListener(onClick);
        final LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionParams.setMarginStart(dp(VALUE_SPACING_DP));
        row.addView(action, actionParams);
        addRow(row);
        return valueView;
    }

    final LinearLayout controlRow(String label) {
        final LinearLayout row = keyRow(label);
        addRow(row);
        return row;
    }

    final void multiValueRow(String key, List<String> values) {
        final LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(this.palette.background);
        column.setPaddingRelative(dp(INSET_DP), dp(ROW_VERTICAL_DP), dp(INSET_DP), dp(ROW_VERTICAL_DP));
        final LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.setGravity(Gravity.CENTER_VERTICAL);
        first.addView(text(key, BODY_MEDIUM, this.palette.keyText));
        first.addView(value(values.isEmpty() ? Status.NOT_AVAILABLE : values.get(0)), weighted(0));
        column.addView(first, matchWidth());
        for (int i = 1; i < values.size(); i++) {
            column.addView(value(values.get(i)), matchWidth());
        }
        addRow(column);
    }

    final MaterialSwitch switchRow(String title, String summary, boolean checked, SwitchListener listener) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPaddingRelative(dp(INSET_DP), dp(ROW_VERTICAL_DP), dp(INSET_DP), dp(ROW_VERTICAL_DP));
        row.setBackground(rowBackground());
        final LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, BODY_MEDIUM, this.palette.keyText));
        if (!TextUtils.isEmpty(summary)) {
            final TextView summaryView = text(summary, BODY_SMALL, this.palette.onSurfaceVariant);
            final LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryParams.topMargin = dp(SUMMARY_SPACING_DP);
            texts.addView(summaryView, summaryParams);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final MaterialSwitch toggle = new MaterialSwitch(this, this.palette);
        toggle.setChecked(checked);
        toggle.setListener(listener);
        final LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleParams.setMarginStart(dp(SWITCH_GAP_DP));
        row.addView(toggle, toggleParams);
        row.setOnClickListener((view) -> toggle.toggle());
        addRow(row);
        return toggle;
    }

    final LinearLayout segmentedRow(String[] options, int selected, SegmentListener listener) {
        final LinearLayout row = new LinearLayout(this);
        row.setBackgroundColor(this.palette.background);
        row.setPaddingRelative(dp(INSET_DP), dp(ROW_VERTICAL_DP), dp(INSET_DP), dp(ROW_VERTICAL_DP));
        final LinearLayout segments = segmentedButtons(options, selected, listener);
        row.addView(segments,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(SEGMENT_HEIGHT_DP)));
        addRow(row);
        return segments;
    }

    final LinearLayout segmentedButtons(String[] options, int selected, SegmentListener listener) {
        final LinearLayout segments = new LinearLayout(this);
        segments.setOrientation(LinearLayout.HORIZONTAL);
        final float radius = dp(SEGMENT_HEIGHT_DP) / 2f;
        for (int i = 0; i < options.length; i++) {
            final int index = i;
            final TextView segment = text(options[i], LABEL_LARGE_SEMIBOLD, this.palette.accent);
            segment.setGravity(Gravity.CENTER);
            segment.setSingleLine(true);
            final GradientDrawable shape = new GradientDrawable();
            shape.setStroke(dp(SEGMENT_BORDER_DP), this.palette.accent);
            final float start = (i == 0) ? radius : 0;
            final float end = (i == options.length - 1) ? radius : 0;
            shape.setCornerRadii(new float[] { start, start, end, end, end, end, start, start });
            segment.setBackground(shape);
            segment.setOnClickListener((view) -> {
                selectSegment(segments, index);
                listener.onSelected(index);
            });
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) {
                params.setMarginStart(-dp(SEGMENT_BORDER_DP));
            }
            segments.addView(segment, params);
        }
        selectSegment(segments, selected);
        return segments;
    }

    private void selectSegment(LinearLayout segments, int selected) {
        for (int i = 0; i < segments.getChildCount(); i++) {
            final TextView segment = (TextView) segments.getChildAt(i);
            final boolean active = i == selected;
            ((GradientDrawable) segment.getBackground()).setColor(active ? this.palette.accent : Color.TRANSPARENT);
            segment.setTextColor(active ? this.palette.onAccent : this.palette.accent);
        }
    }

    final TextView addToolbarText(String label, int color, View.OnClickListener onClick) {
        final TextView button = text(label, TOOLBAR_ACTION, color);
        button.setGravity(Gravity.CENTER);
        button.setPaddingRelative(dp(TOOLBAR_TEXT_PADDING_DP), 0, dp(TOOLBAR_TEXT_PADDING_DP), 0);
        button.setBackground(ripple(null));
        button.setOnClickListener(onClick);
        this.topBar.addView(button,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(CLICKABLE_MIN_DP)));
        return button;
    }

    final void showRunState(TextView toggle, boolean running, boolean startable) {
        toggle.setText(running ? "Stop" : "Start");
        toggle.setTextColor(running ? this.palette.stopAction : this.palette.startAction);
        toggle.setEnabled(running || startable);
    }

    final ImageButton addToolbarIcon(String pathData, String description, View.OnClickListener onClick) {
        final ImageButton button = iconButton(pathData, description, onClick);
        this.topBar.addView(button, new LinearLayout.LayoutParams(dp(CLICKABLE_MIN_DP), dp(CLICKABLE_MIN_DP)));
        return button;
    }

    final TextView text(String value, TextStyle style, int color) {
        final TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        style.applyTo(view);
        return view;
    }

    final int dp(float value) {
        return Math
            .round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    final RippleDrawable rowBackground() {
        return ripple(new ColorDrawable(this.palette.background));
    }

    private View chevron() {
        final ImageView icon = new ImageView(this);
        icon.setImageDrawable(
                new VectorIcon(MaterialIcons.CHEVRON, withAlpha(this.palette.onSurfaceVariant, CHEVRON_ALPHA)));
        return icon;
    }

    private LinearLayout.LayoutParams chevronParams() {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP));
        params.setMarginEnd(dp(CHEVRON_END_DP));
        return params;
    }

    final View dot(int color) {
        final View dot = new View(this);
        final GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        dot.setBackground(shape);
        return dot;
    }

    final LinearLayout.LayoutParams dotParams(int marginStartDp) {
        final int size = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                BODY_MEDIUM.sizeSp * DOT_FONT_SCALE, getResources().getDisplayMetrics()));
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(dp(marginStartDp));
        return params;
    }

    final void addRow(View row) {
        if (this.list.getChildCount() > 0 && isRow(this.list.getChildAt(this.list.getChildCount() - 1))) {
            this.list.addView(new Divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        row.setTag(ROW_TAG);
        this.list.addView(row, matchWidth());
    }

    static TextView singleLine(TextView view) {
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha), Color.red(color), Color.green(color),
                Color.blue(color));
    }

    private boolean isRow(View view) {
        return view.getTag() == ROW_TAG;
    }

    private LinearLayout row() {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(this.palette.background);
        row.setPaddingRelative(dp(INSET_DP), dp(ROW_VERTICAL_DP), dp(INSET_DP), dp(ROW_VERTICAL_DP));
        return row;
    }

    private LinearLayout keyRow(String key) {
        final LinearLayout row = row();
        final TextView keyView = text(key, BODY_MEDIUM, this.palette.keyText);
        keyView.setSingleLine(true);
        row.addView(keyView);
        return row;
    }

    private TextView navigableRow(String key, String value, Status status, View.OnClickListener onClick) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rowBackground());
        final TextView keyView = text(key, BODY_MEDIUM, this.palette.keyText);
        keyView.setSingleLine(true);
        keyView.setPaddingRelative(dp(INSET_DP), keyView.getPaddingTop() + dp(ROW_VERTICAL_DP), dp(INSET_DP),
                keyView.getPaddingBottom() + dp(ROW_VERTICAL_DP));
        row.addView(keyView);
        final TextView valueView = value(value);
        valueView.setTextIsSelectable(false);
        row.addView(valueView, weighted(0));
        if (status != null) {
            row.addView(dot(status.color(this.palette)), dotParams(VALUE_SPACING_DP));
        }
        final LinearLayout.LayoutParams chevronParams = chevronParams();
        chevronParams.setMarginStart(dp((status != null) ? DOT_CHEVRON_GAP_DP : 0));
        row.addView(chevron(), chevronParams);
        row.setOnClickListener(onClick);
        addRow(row);
        return valueView;
    }

    private TextView value(String value) {
        final TextView view = text(value, BODY_MEDIUM, this.palette.valueText);
        view.setGravity(Gravity.END);
        view.setTextIsSelectable(true);
        return view;
    }

    private LinearLayout.LayoutParams weighted(int marginStartDp) {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMarginStart(dp(marginStartDp));
        return params;
    }

    private ImageButton iconButton(String pathData, String description, View.OnClickListener onClick) {
        final ImageButton button = new ImageButton(this);
        final int inset = (dp(CLICKABLE_MIN_DP) - dp(ICON_DP)) / 2;
        button.setImageDrawable(new InsetDrawable(new VectorIcon(pathData, this.palette.title), inset));
        button.setScaleType(ImageView.ScaleType.FIT_XY);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(ripple(null));
        button.setContentDescription(description);
        button.setOnClickListener(onClick);
        return button;
    }

    private RippleDrawable ripple(ColorDrawable content) {
        return new RippleDrawable(ColorStateList.valueOf(withAlpha(this.palette.keyText, RIPPLE_ALPHA)), content, null);
    }

    private View topBar() {
        final LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(this.palette.bar);
        bar.setPaddingRelative(0, 0, dp(TOOLBAR_END_INSET_DP), 0);
        this.topBar = bar;

        final LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(CLICKABLE_MIN_DP),
                dp(CLICKABLE_MIN_DP));
        backParams.setMarginStart(dp(BACK_BUTTON_INSET_DP));
        bar.addView(iconButton(MaterialIcons.BACK, "Back", (view) -> finish()), backParams);

        final TextView title = text(screenTitle(), TITLE_LARGE, this.palette.title);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        final LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp(TITLE_GAP_DP));
        bar.addView(title, titleParams);
        return bar;
    }

    enum Status {

        POSITIVE, NEGATIVE, NEUTRAL;

        static final String NOT_AVAILABLE = "N/A";

        int color(HostPalette palette) {
            switch (this) {
                case POSITIVE:
                    return POSITIVE_DOT;
                case NEGATIVE:
                    return NEGATIVE_DOT;
                default:
                    return palette.grey;
            }
        }

        static Status of(boolean positive, boolean gray) {
            if (positive) {
                return POSITIVE;
            }
            return gray ? NEUTRAL : NEGATIVE;
        }

    }

    interface SwitchListener {

        void onChanged(boolean checked);

    }

    interface SegmentListener {

        void onSelected(int index);

    }

    static final class TextStyle {

        static final int NORMAL = 400;

        static final int MEDIUM = 500;

        static final int SEMIBOLD = 600;

        final int sizeSp;

        private final int lineHeightSp;

        private final float trackingSp;

        private final int weight;

        TextStyle(int sizeSp, int lineHeightSp, float trackingSp, int weight) {
            this.sizeSp = sizeSp;
            this.lineHeightSp = lineHeightSp;
            this.trackingSp = trackingSp;
            this.weight = weight;
        }

        void applyTo(TextView view) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sizeSp);
            view.setIncludeFontPadding(false);
            view.setLetterSpacing(this.trackingSp / this.sizeSp);
            view.setTypeface(Typeface.create(Typeface.DEFAULT, this.weight, false));
            final int lineHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.lineHeightSp,
                    view.getResources().getDisplayMetrics()));
            view.setLineHeight(lineHeight);
            final int leading = lineHeight - view.getPaint().getFontMetricsInt(null);
            view.setPadding(0, leading / 2, 0, leading - leading / 2);
        }

    }

    private final class Divider extends View {

        private final Paint line = new Paint();

        Divider() {
            super(HostScreenActivity.this);
            this.line.setColor(HostScreenActivity.this.palette.divider);
            setBackgroundColor(HostScreenActivity.this.palette.background);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawLine(dp(INSET_DP), 0, getWidth() - dp(INSET_DP), 0, this.line);
        }

    }

}
