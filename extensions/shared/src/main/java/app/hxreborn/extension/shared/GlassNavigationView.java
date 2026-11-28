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
package app.hxreborn.extension.shared;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public final class GlassNavigationView extends FrameLayout {

    private static final float BLUR_RADIUS_DP = 7f;

    private static final int TINT_ALPHA = 150;

    private final Backdrop backdrop;

    private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::invalidate;

    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::invalidate;

    public GlassNavigationView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setWillNotDraw(false);
        this.backdrop = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? new Backdrop(BLUR_RADIUS_DP * getResources().getDisplayMetrics().density) : null;
    }

    private static int sourceId() {
        return 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(this.scrollListener);
        getViewTreeObserver().addOnGlobalLayoutListener(this.layoutListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnScrollChangedListener(this.scrollListener);
        getViewTreeObserver().removeOnGlobalLayoutListener(this.layoutListener);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final View navigation = getChildAt(0);
        final Drawable background = navigation.getBackground();
        if (background != null) {
            final Drawable tint = background.mutate();
            tint.setAlpha(TINT_ALPHA);
            navigation.setBackground(tint);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canvas.isHardwareAccelerated()) {
            final View source = getRootView().findViewById(sourceId());
            if (source != null && getWidth() > 0 && getHeight() > 0) {
                this.backdrop.draw(canvas, source, this);
            }
        }
        super.dispatchDraw(canvas);
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static final class Backdrop {

        private final Path outline = new Path();

        private final int[] sourcePosition = new int[2];

        private final int[] barPosition = new int[2];

        private final RenderNode blur = new RenderNode("navigation blur");

        private Backdrop(float radius) {
            this.blur.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }

        private void draw(Canvas canvas, View source, View bar) {
            final int width = bar.getWidth();
            final int height = bar.getHeight();
            source.getLocationOnScreen(this.sourcePosition);
            bar.getLocationOnScreen(this.barPosition);
            this.blur.setPosition(0, 0, width, height);
            final RecordingCanvas recording = this.blur.beginRecording(width, height);
            try {
                recording.translate(this.sourcePosition[0] - this.barPosition[0],
                        this.sourcePosition[1] - this.barPosition[1]);
                source.draw(recording);
            } finally {
                this.blur.endRecording();
            }
            this.outline.reset();
            this.outline.addRoundRect(0, 0, width, height, height / 2f, height / 2f, Path.Direction.CW);
            final int save = canvas.save();
            canvas.clipPath(this.outline);
            canvas.drawRenderNode(this.blur);
            canvas.restoreToCount(save);
        }

    }

}
