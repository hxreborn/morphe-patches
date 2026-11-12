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
package app.hxreborn.extension.protonvpn;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.RequiresApi;
import app.morphe.extension.shared.Utils;

import app.hxreborn.extension.proton.AccentColor;

@SuppressWarnings("unused")
public final class FrostedGlass {

    private static final float BLUR_RADIUS_DP = 12f;

    private static final float SURFACE_ALPHA = 0.35f;

    private static final int HOST_TRANSLUCENT_ALPHA = Math.round(0.86f * 255);

    private static boolean drawingMask;

    private static boolean drawingFade;

    private static WeakReference<DrawCallback> current = new WeakReference<>(null);

    private FrostedGlass() {
    }

    public static Object source(Object original) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return original;
        }
        DrawCallback callback = current.get();
        if (callback == null || !original.equals(callback.original.get())
                || (callback.renderer.mapCoordinates != null && !isAttached(callback.renderer.mapCoordinates))) {
            callback = new DrawCallback(original);
            current = new WeakReference<>(callback);
        }
        return appendModifiers(original, callback);
    }

    public static boolean isDrawingMask() {
        return drawingMask;
    }

    public static boolean draw(Object node, Object scope) {
        if (drawingMask || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isRounded(node)) {
            return false;
        }
        final Canvas destination = canvas(scope);
        if (!destination.isHardwareAccelerated()) {
            return false;
        }
        final long color = color(node);
        final int alpha = Color.alpha((int) (color >>> Integer.SIZE));
        if ((color & 0xffffffffL) != 0L || (alpha != Color.alpha(Color.WHITE) && alpha != HOST_TRANSLUCENT_ALPHA)) {
            return false;
        }
        final DrawCallback callback = current.get();
        return callback != null
                && callback.renderer.drawSurface(node, scope, destination, (int) (color >>> Integer.SIZE));
    }

    public static boolean fade(Object callback, Object scope) {
        if (drawingFade || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        final DrawCallback source = current.get();
        return source != null && source.renderer.drawFade(callback, scope);
    }

    private static float fadeHeight(Object callback, Object scope) {
        return 0f;
    }

    private static void drawFade(Object callback, Object scope) {
    }

    private static int[] brandColors() {
        return new int[0];
    }

    private static long color(Object node) {
        return 0L;
    }

    private static void setColor(Object node, long replacement) {
    }

    private static boolean isRounded(Object node) {
        return false;
    }

    private static void drawBackgroundMask(Object node, Object scope) {
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

    private static long relativePosition(Object sourceCoordinates, Object surfaceCoordinates) {
        return 0L;
    }

    private static long screenPosition(Object coordinates) {
        return 0L;
    }

    private static View view(Object coordinates) {
        return null;
    }

    private static Object coordinates(Object scope) {
        return null;
    }

    private static Object appendModifiers(Object original, Object drawCallback) {
        return original;
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public static final class DrawCallback {

        private final Renderer renderer = new Renderer();

        private final WeakReference<Object> original;

        private DrawCallback(Object original) {
            this.original = new WeakReference<>(original);
        }

        public void accept(Object scope) {
            this.renderer.drawMap(scope);
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Renderer {

        private final int[] accentColors = brandColors();

        private final int lightAccent = AccentColor.getAccentColor(false);

        private final int darkAccent = AccentColor.getAccentColor(true);

        private final float radius = BLUR_RADIUS_DP * Utils.getContext().getResources().getDisplayMetrics().density;

        private final RenderNode map = new RenderNode("map backdrop");

        private final RenderNode fade = new RenderNode("map fade");

        private final Map<Object, Surface> surfaces = new WeakHashMap<>();

        private final Map<View, BackdropObserver> observers = new WeakHashMap<>();

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint fadeMask = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Object mapCoordinates;

        private Object fadeCoordinates;

        private boolean fadeCrossRoot;

        private float fadeHeight;

        private long fadeVersion;

        private Renderer() {
            for (int index = 0; index < this.accentColors.length; index++) {
                this.accentColors[index] = AccentColor.transformBrandColorArgb(this.accentColors[index]);
            }
            this.mask.setBlendMode(BlendMode.DST_IN);
            this.fadeMask.setBlendMode(BlendMode.DST_OUT);
        }

        private void drawMap(Object scope) {
            final Object mapCoordinates = coordinates(scope);
            if (this.mapCoordinates != mapCoordinates) {
                this.mapCoordinates = mapCoordinates;
                this.fadeCrossRoot = false;
            }
            final Canvas destination = canvas(scope);
            final long packedSize = size(scope);
            final int width = (int) Math.ceil(Float.intBitsToFloat((int) (packedSize >>> Integer.SIZE)));
            final int height = (int) Math.ceil(Float.intBitsToFloat((int) packedSize));
            if (!destination.isHardwareAccelerated() || width <= 0 || height <= 0) {
                drawContent(scope);
                return;
            }
            this.map.setPosition(0, 0, width, height);
            final RecordingCanvas recording = this.map.beginRecording(width, height);
            final Canvas previous = replaceCanvas(scope, recording);
            try {
                drawContent(scope);
            } finally {
                replaceCanvas(scope, previous);
                this.map.endRecording();
            }
            destination.drawRenderNode(this.map);
            final View source = view(this.mapCoordinates);
            for (View owner : this.observers.keySet()) {
                if (owner != source && owner.isAttachedToWindow()) {
                    owner.postInvalidateOnAnimation();
                }
            }
        }

        private boolean drawFade(Object callback, Object scope) {
            if (!isAttached(this.mapCoordinates)) {
                return false;
            }
            final Canvas destination = canvas(scope);
            final long packedSize = size(scope);
            final int width = (int) Math.ceil(Float.intBitsToFloat((int) (packedSize >>> Integer.SIZE)));
            final int height = (int) Math.ceil(Float.intBitsToFloat((int) packedSize));
            if (!destination.isHardwareAccelerated() || width <= 0 || height <= 0) {
                return false;
            }
            final Object fadeCoordinates = coordinates(scope);
            if (this.fadeCoordinates != fadeCoordinates) {
                this.fadeCoordinates = fadeCoordinates;
                this.fadeCrossRoot = false;
            }
            this.fadeHeight = FrostedGlass.fadeHeight(callback, scope);
            this.fade.setPosition(0, 0, width, height);
            final RecordingCanvas recording = this.fade.beginRecording(width, height);
            final Canvas previous = replaceCanvas(scope, recording);
            try {
                drawingFade = true;
                FrostedGlass.drawFade(callback, scope);
            } finally {
                drawingFade = false;
                replaceCanvas(scope, previous);
                this.fade.endRecording();
            }
            this.fadeVersion++;
            destination.drawRenderNode(this.fade);
            observe(view(this.fadeCoordinates));
            updatePositions(view(this.fadeCoordinates));
            return true;
        }

        private boolean drawSurface(Object node, Object scope, Canvas destination, int color) {
            if (!this.map.hasDisplayList() || !isAttached(this.mapCoordinates)
                    || isAccentColor(color)) {
                return false;
            }
            final long packedSize = size(scope);
            final float width = Float.intBitsToFloat((int) (packedSize >>> Integer.SIZE));
            final float height = Float.intBitsToFloat((int) packedSize);
            if (width <= 0f || height <= 0f) {
                return false;
            }
            final Object surfaceCoordinates = coordinates(scope);
            if (!isAttached(surfaceCoordinates)) {
                return false;
            }
            Surface surface = this.surfaces.get(node);
            if (surface == null) {
                surface = new Surface(this.radius);
                this.surfaces.put(node, surface);
            }
            surface.coordinates = new WeakReference<>(surfaceCoordinates);
            surface.owner = new WeakReference<>(view(surfaceCoordinates));
            surface.crossRoot = surface.owner.get() != view(this.mapCoordinates);
            surface.width = width;
            surface.height = height;
            surface.color = color;
            surface.position = Long.MIN_VALUE;
            recordBackground(surface.original, node, scope, color, surface);
            recordBackground(surface.mask, node, scope, color | Color.BLACK, surface);
            recordSurface(surface);
            observe(surface.owner.get());
            observe(view(this.mapCoordinates));
            destination.drawRenderNode(surface.background);
            drawContent(scope);
            return true;
        }

        private void recordBackground(RenderNode target, Object node, Object scope, int color, Surface surface) {
            final int width = (int) Math.ceil(surface.width);
            final int height = (int) Math.ceil(surface.height);
            target.setPosition(0, 0, width, height);
            final RecordingCanvas recording = target.beginRecording(width, height);
            final Canvas previous = replaceCanvas(scope, recording);
            final long original = FrostedGlass.color(node);
            try {
                setColor(node, ((long) color & 0xffffffffL) << Integer.SIZE);
                drawingMask = true;
                drawBackgroundMask(node, scope);
            } finally {
                drawingMask = false;
                setColor(node, original);
                replaceCanvas(scope, previous);
                target.endRecording();
            }
        }

        private boolean isAccentColor(int color) {
            if (color == this.lightAccent || color == this.darkAccent) {
                return true;
            }
            for (int accent : this.accentColors) {
                if (color == accent) {
                    return true;
                }
            }
            return false;
        }

        private void observe(View view) {
            if (!this.observers.containsKey(view)) {
                final BackdropObserver observer = new BackdropObserver(this, view);
                this.observers.put(view, observer);
                view.getViewTreeObserver().addOnPreDrawListener(observer);
                view.addOnAttachStateChangeListener(observer);
            }
        }

        private void updatePositions(View drawingView) {
            if (!isAttached(this.mapCoordinates)) {
                return;
            }
            for (Surface surface : this.surfaces.values()) {
                if (isAttached(surface.coordinates.get()) && recordSurface(surface)) {
                    final View owner = surface.owner.get();
                    if (owner != null && owner != drawingView) {
                        owner.postInvalidateOnAnimation();
                    }
                }
            }
        }

        private long surfacePosition(Surface surface) {
            final Object surfaceCoordinates = surface.coordinates.get();
            if (!surface.crossRoot) {
                try {
                    return relativePosition(this.mapCoordinates, surfaceCoordinates);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    surface.crossRoot = true;
                }
            }
            return screenOffset(surfaceCoordinates);
        }

        private long fadePosition() {
            if (!this.fadeCrossRoot) {
                try {
                    return relativePosition(this.mapCoordinates, this.fadeCoordinates);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    this.fadeCrossRoot = true;
                }
            }
            return screenOffset(this.fadeCoordinates);
        }

        private long screenOffset(Object coordinates) {
            final long source = screenPosition(this.mapCoordinates);
            final long destination = screenPosition(coordinates);
            final float x = Float.intBitsToFloat((int) (destination >>> Integer.SIZE))
                    - Float.intBitsToFloat((int) (source >>> Integer.SIZE));
            final float y = Float.intBitsToFloat((int) destination) - Float.intBitsToFloat((int) source);
            return ((long) Float.floatToRawIntBits(x) << Integer.SIZE) | (Float.floatToRawIntBits(y) & 0xffffffffL);
        }

        private boolean recordSurface(Surface surface) {
            final long position = surfacePosition(surface);
            final boolean fadeReady = this.fade.hasDisplayList() && isAttached(this.fadeCoordinates);
            final long fadePosition = fadeReady ? fadePosition() : Long.MIN_VALUE;
            if (surface.position == position && surface.fadePosition == fadePosition
                    && surface.fadeVersion == this.fadeVersion) {
                return false;
            }
            final float x = Float.intBitsToFloat((int) (position >>> Integer.SIZE));
            final float y = Float.intBitsToFloat((int) position);
            final float fadeX = Float.intBitsToFloat((int) (fadePosition >>> Integer.SIZE));
            final float fadeY = Float.intBitsToFloat((int) fadePosition);
            final boolean useOriginalBackground = !fadeReady || (y >= fadeY + this.fadeHeight && x >= fadeX
                    && x + surface.width <= fadeX + this.fade.getWidth());
            final int width = (int) Math.ceil(surface.width);
            final int height = (int) Math.ceil(surface.height);
            surface.background.setPosition(0, 0, width, height);
            if (!useOriginalBackground) {
                recordBlur(surface, x, y, fadeX, fadeY);
            }
            final RecordingCanvas recording = surface.background.beginRecording(width, height);
            try {
                recording.drawRenderNode(surface.original);
                if (!useOriginalBackground) {
                    final int layer = recording.saveLayer(0f, 0f, surface.width, surface.height, null);
                    recording.drawRenderNode(surface.blur);
                    this.paint.setColor(surface.color);
                    this.paint.setAlpha(Math.round(Color.alpha(surface.color) * SURFACE_ALPHA));
                    recording.drawRect(0f, 0f, surface.width, surface.height, this.paint);
                    final int maskLayer = recording.saveLayer(0f, 0f, surface.width, surface.height, this.mask);
                    recording.drawRenderNode(surface.mask);
                    recording.restoreToCount(maskLayer);
                    final int fadeLayer = recording.saveLayer(0f, 0f, surface.width, surface.height, this.fadeMask);
                    recording.translate(fadeX - x, fadeY - y);
                    recording.drawRenderNode(this.fade);
                    recording.restoreToCount(fadeLayer);
                    recording.restoreToCount(layer);
                }
            } finally {
                surface.background.endRecording();
            }
            surface.position = position;
            surface.fadePosition = fadePosition;
            surface.fadeVersion = this.fadeVersion;
            return true;
        }

        private void recordBlur(Surface surface, float x, float y, float fadeX, float fadeY) {
            final int outset = (int) Math.ceil(this.radius);
            final int width = (int) Math.ceil(surface.width) + outset * 2;
            final int height = (int) Math.ceil(surface.height) + outset * 2;
            surface.blur.setPosition(-outset, -outset, width - outset, height - outset);
            final RecordingCanvas recording = surface.blur.beginRecording(width, height);
            try {
                recording.drawColor(surface.color | Color.BLACK);
                recording.translate(outset - x, outset - y);
                recording.drawRenderNode(this.map);
                recording.translate(fadeX, fadeY);
                recording.drawRenderNode(this.fade);
            } finally {
                surface.blur.endRecording();
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class Surface {

        private final RenderNode blur = new RenderNode("frosted backdrop");

        private final RenderNode original = new RenderNode("original background");

        private final RenderNode mask = new RenderNode("background outline");

        private final RenderNode background = new RenderNode("glass background");

        private WeakReference<Object> coordinates = new WeakReference<>(null);

        private WeakReference<View> owner = new WeakReference<>(null);

        private boolean crossRoot;

        private float width;

        private float height;

        private int color;

        private long position = Long.MIN_VALUE;

        private long fadePosition = Long.MIN_VALUE;

        private long fadeVersion = Long.MIN_VALUE;

        private Surface(float radius) {
            this.blur.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static final class BackdropObserver
            implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {

        private final WeakReference<Renderer> renderer;

        private final WeakReference<View> view;

        private BackdropObserver(Renderer renderer, View view) {
            this.renderer = new WeakReference<>(renderer);
            this.view = new WeakReference<>(view);
        }

        @Override
        public boolean onPreDraw() {
            final Renderer renderer = this.renderer.get();
            if (renderer == null) {
                detach();
            } else {
                renderer.updatePositions(this.view.get());
            }
            return true;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            final Renderer renderer = this.renderer.get();
            if (renderer != null) {
                renderer.observers.remove(view);
            }
            detach();
        }

        private void detach() {
            final View view = this.view.get();
            if (view != null) {
                final ViewTreeObserver observer = view.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                view.removeOnAttachStateChangeListener(this);
            }
        }

    }

}
