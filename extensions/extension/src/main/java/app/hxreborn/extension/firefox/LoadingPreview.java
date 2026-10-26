/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.firefox;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import app.hxreborn.extension.firefox.PreviewView.Scene;
import app.hxreborn.extension.shared.Dimensions;
import app.hxreborn.extension.shared.LoadingIndicatorView;

final class LoadingPreview extends FrameLayout implements SettingsRows.Stateful {

    private static final int INDICATOR_DP = 40;

    private static final long FADE_MS = 160;

    private static final float STATIC_PROGRESS = 0.5f;

    private final PreviewView frame;

    private final LoadingIndicatorView indicator;

    private final TimeInterpolator easing;

    private final Rect visible = new Rect();

    private final ViewTreeObserver.OnScrollChangedListener scrolled = this::update;

    private ViewTreeObserver observer;

    private boolean on;

    LoadingPreview(Context context, SettingsStyle palette, TimeInterpolator easing, Scene scene) {
        super(context);
        this.easing = easing;
        this.frame = new PreviewView(context, palette, easing, scene, false);
        addView(this.frame, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.indicator = new LoadingIndicatorView(context);
        this.indicator.setIndicatorColor(palette.accent);
        this.indicator.setAlpha(0f);
        int size = Dimensions.dp(context, INDICATOR_DP);
        addView(this.indicator, new LayoutParams(size, size, Gravity.CENTER));
    }

    @Override
    public void setState(boolean on, boolean animate) {
        this.on = on;
        this.frame.setState(on, animate);
        float alpha = on ? 1f : 0f;
        if (animate && !PreviewView.animationsDisabled(getContext())) {
            this.indicator.animate().alpha(alpha).setDuration(FADE_MS).setInterpolator(this.easing).start();
        } else {
            this.indicator.animate().cancel();
            this.indicator.setAlpha(alpha);
        }
        update();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.observer = getViewTreeObserver();
        this.observer.addOnScrollChangedListener(this.scrolled);
        post(this::update);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.observer != null && this.observer.isAlive()) {
            this.observer.removeOnScrollChangedListener(this.scrolled);
        }
        this.indicator.setIndeterminate(false);
        super.onDetachedFromWindow();
    }

    private void update() {
        if (PreviewView.animationsDisabled(getContext())) {
            this.indicator.setIndeterminate(false);
            this.indicator.setProgress(STATIC_PROGRESS);
            return;
        }
        boolean showing = this.on && isShown() && getGlobalVisibleRect(this.visible) && this.visible.height() > 0;
        this.indicator.setIndeterminate(showing);
    }

}
