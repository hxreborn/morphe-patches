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

import android.app.Activity;
import android.app.Application;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.RequiresApi;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class NavigationBarBackdrop {

    public static final String BLUR_STRENGTH_KEY = "floating_navigation_bar_blur_strength";

    public static final int DEFAULT_BLUR_STRENGTH_DP = 7;

    public static final int MAX_BLUR_STRENGTH_DP = 25;

    public static final int MIN_GLASS_BLUR_STRENGTH_DP = 1;

    public static final String GLASS_KEY = "floating_navigation_bar_glass";

    private static final String LEGACY_BLUR_KEY = "floating_navigation_bar_blur";

    private static final String LOG_TAG = "hxProton";

    private static Renderer current;

    private NavigationBarBackdrop() {
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static void watch(Activity activity) {
        if (!isSupported() || !FloatingNavigationBar.isEnabled()) {
            return;
        }
        final int strength = blurStrengthDp();
        if (strength > 0) {
            current = new Renderer(strength, PatchSettings.isEnabled(GLASS_KEY, false));
            activity.registerActivityLifecycleCallbacks(current);
        }
    }

    public static int blurStrengthDp() {
        final int fallback = PatchSettings.isEnabled(LEGACY_BLUR_KEY, false) ? DEFAULT_BLUR_STRENGTH_DP : 0;
        int strength;
        try {
            strength = Integer.parseInt(PatchSettings.getString(BLUR_STRENGTH_KEY, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            Log.w(LOG_TAG, "Navigation blur strength is invalid, using default");
            strength = fallback;
        }
        strength = Math.max(0, Math.min(MAX_BLUR_STRENGTH_DP, strength));
        return (strength == 0 && PatchSettings.isEnabled(GLASS_KEY, false)) ? DEFAULT_BLUR_STRENGTH_DP : strength;
    }

    public static void setBlurStrengthDp(int strength) {
        final int minimum = PatchSettings.isEnabled(GLASS_KEY, false) ? MIN_GLASS_BLUR_STRENGTH_DP : 0;
        PatchSettings.setString(BLUR_STRENGTH_KEY,
                String.valueOf(Math.max(minimum, Math.min(MAX_BLUR_STRENGTH_DP, strength))));
    }

    public static Object source(Object original) {
        return (isSupported() && current != null) ? appendModifiers(original, current.sourceDraw) : original;
    }

    public static Object bar(Object original) {
        return (isSupported() && current != null) ? appendModifiers(original, current.barDraw) : original;
    }

    static long background(long color) {
        if (!isSupported() || current == null) {
            return color;
        }
        current.color = (int) (color >>> Integer.SIZE);
        return 0L;
    }

    public static long pageColor(long color) {
        if (isSupported() && current != null) {
            current.pageColor = (int) (color >>> Integer.SIZE);
        }
        return color;
    }

    private static Canvas canvas(Object scope) {
        return null;
    }

    private static Canvas replaceCanvas(Object scope, Canvas replacement) {
        return null;
    }

    private static void drawContent(Object scope) {
    }

    private static long size(Object scope) {
        return 0L;
    }

    private static boolean isAttached(Object coordinates) {
        return false;
    }

    private static long relativePosition(Object sourceCoordinates, Object barCoordinates) {
        return 0L;
    }

    private static Object coordinates(Object scope) {
        return null;
    }

    private static Object appendModifiers(Object original, Object drawCallback) {
        return original;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public static final class DrawCallback {

        private final Renderer renderer;

        private final boolean source;

        private DrawCallback(Renderer renderer, boolean source) {
            this.renderer = renderer;
            this.source = source;
        }

        public void accept(Object scope) {
            if (this.source) {
                this.renderer.drawSource(scope);
            } else {
                this.renderer.drawBar(scope);
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Renderer
            implements Application.ActivityLifecycleCallbacks, ViewTreeObserver.OnPreDrawListener {

        private final float blurRadiusPx;

        private final boolean glass;

        private final float density = Utils.getContext().getResources().getDisplayMetrics().density;

        private final RenderNode sourceNode = new RenderNode("navigation content");

        private final RenderNode blurNode = new RenderNode("navigation blur");

        private final NavigationBarAppearance appearance = new NavigationBarAppearance(this.density);

        private final DrawCallback sourceDraw = new DrawCallback(this, true);

        private final DrawCallback barDraw = new DrawCallback(this, false);

        private Object sourceCoordinates;

        private Object barCoordinates;

        private int color = Color.BLACK;

        private int pageColor = Color.BLACK;

        private float barWidth;

        private float barHeight;

        private boolean failed;

        private boolean released;

        private Renderer(int strengthDp, boolean glass) {
            this.glass = glass;
            this.blurRadiusPx = strengthDp * this.density;
            this.blurNode.setRenderEffect(
                    RenderEffect.createBlurEffect(this.blurRadiusPx, this.blurRadiusPx, Shader.TileMode.CLAMP));
        }

        private void drawSource(Object scope) {
            this.sourceCoordinates = coordinates(scope);
            final Canvas destination = canvas(scope);
            if (this.failed || this.released || !destination.isHardwareAccelerated()) {
                if (!destination.isHardwareAccelerated()) {
                    fail();
                }
                drawContent(scope);
                return;
            }
            final long packedSize = size(scope);
            final int width = (int) Math.ceil(Float.intBitsToFloat((int) (packedSize >>> Integer.SIZE)));
            final int height = (int) Math.ceil(Float.intBitsToFloat((int) packedSize));
            if (width <= 0 || height <= 0) {
                drawContent(scope);
                return;
            }
            this.sourceNode.setPosition(0, 0, width, height);
            final RecordingCanvas recording = this.sourceNode.beginRecording(width, height);
            final Canvas previous = replaceCanvas(scope, recording);
            try {
                drawContent(scope);
            } finally {
                replaceCanvas(scope, previous);
                this.sourceNode.endRecording();
            }
            if (this.barCoordinates != null && this.barWidth > 0f && this.barHeight > 0f) {
                recordBlur();
            }
            destination.drawRenderNode(this.sourceNode);
        }

        private void drawBar(Object scope) {
            this.barCoordinates = coordinates(scope);
            final Canvas destination = canvas(scope);
            final long packedSize = size(scope);
            final float width = Float.intBitsToFloat((int) (packedSize >>> Integer.SIZE));
            final float height = Float.intBitsToFloat((int) packedSize);
            if (!destination.isHardwareAccelerated()) {
                fail();
            }
            this.barWidth = width;
            this.barHeight = height;
            final boolean ready = !this.failed && !this.released && this.sourceNode.hasDisplayList()
                    && isAttached(this.sourceCoordinates) && isAttached(this.barCoordinates);
            if (ready) {
                recordBlur();
            }
            this.appearance.draw(destination, this.blurNode, width, height, this.color, this.glass, ready);
            drawContent(scope);
        }

        private void recordBlur() {
            if (!isAttached(this.sourceCoordinates) || !isAttached(this.barCoordinates)) {
                return;
            }
            final long position = relativePosition(this.sourceCoordinates, this.barCoordinates);
            final float x = Float.intBitsToFloat((int) (position >>> Integer.SIZE));
            final float y = Float.intBitsToFloat((int) position);
            final int outset = (int) Math.ceil(this.blurRadiusPx);
            final int nodeWidth = (int) Math.ceil(this.barWidth) + outset * 2;
            final int nodeHeight = (int) Math.ceil(this.barHeight) + outset * 2;
            this.blurNode.setPosition(-outset, -outset, nodeWidth - outset, nodeHeight - outset);
            final RecordingCanvas recording = this.blurNode.beginRecording(nodeWidth, nodeHeight);
            try {
                recording.drawColor(this.pageColor);
                recording.translate(outset - x, outset - y);
                recording.drawRenderNode(this.sourceNode);
            } finally {
                this.blurNode.endRecording();
            }
        }

        private void fail() {
            if (!this.failed) {
                this.failed = true;
                Log.w(LOG_TAG, "Navigation effects require a hardware canvas, using solid navigation bar");
            }
        }

        @Override
        public boolean onPreDraw() {
            if (!this.failed && !this.released && this.sourceNode.hasDisplayList() && this.barWidth > 0f
                    && this.barHeight > 0f) {
                recordBlur();
            }
            return true;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle state) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            current = this;
            activity.getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(this);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            activity.getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(this);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle state) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            this.released = true;
            this.sourceCoordinates = null;
            this.barCoordinates = null;
            this.sourceNode.discardDisplayList();
            this.blurNode.discardDisplayList();
            if (current == this) {
                current = null;
            }
        }

    }

}
