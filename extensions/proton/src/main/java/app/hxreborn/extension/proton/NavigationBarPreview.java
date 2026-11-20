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
package app.hxreborn.extension.proton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.S)
final class NavigationBarPreview extends android.widget.FrameLayout {

    private static final int PREVIEW_HEIGHT_DP = 228;

    private static final int PREVIEW_RADIUS_DP = 12;

    private static final int CONTENT_INSET_DP = 12;

    private static final int CONTENT_BOTTOM_PADDING_DP = 96;

    private static final int CONTENT_ROW_HEIGHT_DP = 68;

    private static final int CONTENT_ROW_GAP_DP = 8;

    private static final int CONTENT_TEXT_SP = 16;

    private static final int BAR_HEIGHT_DP = 56;

    private static final int BAR_MARGIN_DP = 22;

    private static final int BAR_RADIUS_DP = 28;

    private static final int ICON_SIZE_DP = 24;

    private static final int LABEL_SIZE_SP = 12;

    private static final int LABEL_GAP_DP = 4;

    private final float density;

    private final ScrollView content;

    private final Drawable[] icons;

    private final String[] labels;

    private final int backgroundColor;

    private final int surfaceColor;

    private final int unselectedColor;

    private final int accentColor;

    private final NavigationBarAppearance appearance;

    private final RenderNode sourceNode = new RenderNode("navigation preview content");

    private final RenderNode blurNode = new RenderNode("navigation preview blur");

    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private StaticLayout[] labelLayouts = new StaticLayout[0];

    private final RectF barBounds = new RectF();

    private final Path barClip = new Path();

    private int strengthDp = -1;

    private boolean glass;

    NavigationBarPreview(Context context) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
        final int background = PatchesTheme.backgroundNorm(context);
        final boolean dark = PatchesTheme.isDark(background);
        this.backgroundColor = (dark && AmoledTheme.isEnabled()) ? Color.BLACK : background;
        this.surfaceColor = FloatingNavigationBar.surfaceColor(dark);
        this.unselectedColor = showLabels() ? PatchesTheme.textWeak(context) : PatchesTheme.textNorm(context);
        this.accentColor = AccentColor.getAccentColor(dark);
        this.appearance = new NavigationBarAppearance(this.density);
        final int[] iconIds = iconResources();
        final int[] labelIds = labelResources();
        if (iconIds.length == 0 || iconIds.length != labelIds.length) {
            throw new IllegalStateException("Navigation preview resources are incomplete");
        }
        this.icons = new Drawable[iconIds.length];
        this.labels = new String[labelIds.length];
        for (int index = 0; index < iconIds.length; index++) {
            this.icons[index] = context.getDrawable(iconIds[index]).mutate();
            this.labels[index] = context.getString(labelIds[index]);
        }
        this.labelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SIZE_SP,
                getResources().getDisplayMetrics()));
        this.labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        this.content = new ScrollView(context);
        this.content.setVerticalScrollBarEnabled(false);
        this.content.setContentDescription("Preview content. Scroll to compare blur.");
        this.content.setOnScrollChangeListener((view, scrollX, scrollY, oldX, oldY) -> invalidate());
        this.content.addView(sampleContent(context, iconIds),
                new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(this.content,
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setBackground(PatchesTheme.createRoundedRectangle(context, this.backgroundColor, PREVIEW_RADIUS_DP));
        setClipToOutline(true);
        setMinimumHeight(dp(PREVIEW_HEIGHT_DP));
        updateAppearance();
    }

    private LinearLayout sampleContent(Context context, int[] iconIds) {
        final LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(CONTENT_INSET_DP), dp(CONTENT_ROW_GAP_DP), dp(CONTENT_INSET_DP),
                dp(CONTENT_BOTTOM_PADDING_DP));
        for (int index = 0; index < this.labels.length; index++) {
            final LinearLayout row = new LinearLayout(context);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(CONTENT_INSET_DP), 0, dp(CONTENT_INSET_DP), 0);
            row.setMinimumHeight(dp(CONTENT_ROW_HEIGHT_DP));
            row.setBackground(PatchesTheme.createRoundedRectangle(context, PatchesTheme.backgroundSecondary(context),
                    CONTENT_INSET_DP));
            row.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            final ImageView icon = new ImageView(context);
            icon.setImageDrawable(context.getDrawable(iconIds[index]).mutate());
            icon.setColorFilter(this.unselectedColor);
            row.addView(icon, new LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)));
            final TextView label = new TextView(context);
            label.setText(this.labels[index]);
            label.setTextColor(PatchesTheme.textNorm(context));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, CONTENT_TEXT_SP);
            final LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.setMarginStart(dp(CONTENT_INSET_DP));
            row.addView(label, textParams);
            final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(CONTENT_ROW_GAP_DP);
            column.addView(row, rowParams);
        }
        return column;
    }

    void updateAppearance() {
        final int strength = NavigationBarBackdrop.blurStrengthDp();
        this.glass = PatchSettings.isEnabled(NavigationBarBackdrop.GLASS_KEY, false);
        if (strength != this.strengthDp) {
            this.strengthDp = strength;
            final float radius = strength * this.density;
            this.blurNode.setRenderEffect(
                    (strength == 0) ? null : RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
        setContentDescription("Navigation bar preview. Blur strength " + strength + " of "
                + NavigationBarBackdrop.MAX_BLUR_STRENGTH_DP + ". Glass " + (this.glass ? "on." : "off."));
        invalidate();
    }

    void scrollContent() {
        final int bottom = Math.max(0, this.content.getChildAt(0).getHeight() - this.content.getHeight());
        this.content.smoothScrollTo(0, (this.content.getScrollY() > bottom / 2) ? 0 : bottom);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        final int action = event.getActionMasked();
        getParent()
            .requestDisallowInterceptTouchEvent(action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL);
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas destination) {
        final boolean hardware = destination.isHardwareAccelerated();
        if (hardware) {
            this.sourceNode.setPosition(0, 0, getWidth(), getHeight());
            final RecordingCanvas recording = this.sourceNode.beginRecording(getWidth(), getHeight());
            try {
                recording.drawColor(this.backgroundColor);
                super.dispatchDraw(recording);
            } finally {
                this.sourceNode.endRecording();
            }
            destination.drawRenderNode(this.sourceNode);
        } else {
            super.dispatchDraw(destination);
        }
        final float margin = dp(BAR_MARGIN_DP);
        final float height = dp(BAR_HEIGHT_DP);
        final float width = Math.max(0f, getWidth() - margin * 2f);
        final float top = getHeight() - margin - height;
        this.barBounds.set(margin, top, margin + width, top + height);
        this.barClip.reset();
        this.barClip.addRoundRect(this.barBounds, dp(BAR_RADIUS_DP), dp(BAR_RADIUS_DP), Path.Direction.CW);
        final int save = destination.save();
        destination.clipPath(this.barClip);
        destination.translate(margin, top);
        final boolean blur = hardware && this.strengthDp > 0;
        if (blur) {
            final int outset = (int) Math.ceil(this.strengthDp * this.density);
            final int nodeWidth = (int) Math.ceil(width) + outset * 2;
            final int nodeHeight = (int) height + outset * 2;
            this.blurNode.setPosition(-outset, -outset, nodeWidth - outset, nodeHeight - outset);
            final RecordingCanvas recording = this.blurNode.beginRecording(nodeWidth, nodeHeight);
            try {
                recording.drawColor(this.backgroundColor);
                recording.translate(outset - margin, outset - top);
                recording.drawRenderNode(this.sourceNode);
            } finally {
                this.blurNode.endRecording();
            }
        }
        this.appearance.draw(destination, this.blurNode, width, height, this.surfaceColor, this.glass, blur);
        drawIcons(destination, width, height);
        destination.restoreToCount(save);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (!showLabels()) {
            return;
        }
        final int slotWidth = Math.max(1, (width - dp(BAR_MARGIN_DP) * 2) / this.labels.length);
        this.labelLayouts = new StaticLayout[this.labels.length];
        for (int index = 0; index < this.labels.length; index++) {
            final String label = this.labels[index];
            this.labelLayouts[index] = StaticLayout.Builder.obtain(label, 0, label.length(), this.labelPaint, slotWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
        }
    }

    private void drawIcons(Canvas canvas, float width, float height) {
        final float slotWidth = width / this.icons.length;
        final int iconSize = dp(ICON_SIZE_DP);
        int labelHeight = 0;
        for (StaticLayout label : this.labelLayouts) {
            labelHeight = Math.max(labelHeight, label.getHeight());
        }
        final float contentHeight = iconSize + ((labelHeight > 0) ? dp(LABEL_GAP_DP) + labelHeight : 0f);
        final float top = (height - contentHeight) / 2f;
        for (int index = 0; index < this.icons.length; index++) {
            final int color = (index == this.icons.length - 1) ? this.accentColor : this.unselectedColor;
            final float center = (index + 0.5f) * slotWidth;
            final int left = Math.round(center - iconSize / 2f);
            this.icons[index].setTint(color);
            this.icons[index].setBounds(left, Math.round(top), left + iconSize, Math.round(top) + iconSize);
            this.icons[index].draw(canvas);
            if (this.labelLayouts.length > 0) {
                this.labelPaint.setColor(color);
                final int save = canvas.save();
                canvas.translate(index * slotWidth, top + iconSize + dp(LABEL_GAP_DP));
                this.labelLayouts[index].draw(canvas);
                canvas.restoreToCount(save);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        this.sourceNode.discardDisplayList();
        this.blurNode.discardDisplayList();
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * this.density);
    }

    private static int[] iconResources() {
        return new int[0];
    }

    private static int[] labelResources() {
        return new int[0];
    }

    private static boolean showLabels() {
        return false;
    }

}
