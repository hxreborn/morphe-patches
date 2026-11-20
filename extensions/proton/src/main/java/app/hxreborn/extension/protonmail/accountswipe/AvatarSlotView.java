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
package app.hxreborn.extension.protonmail.accountswipe;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.PathInterpolator;

import app.hxreborn.extension.shared.Dimensions;

@SuppressLint("ViewConstructor")
final class AvatarSlotView extends View {

    static final long TRANSITION_MS = 300L;

    static final TimeInterpolator EASING = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private static final float CORNER_RADIUS_DP = 12f;

    private static final float INITIALS_TEXT_SP = 14f;

    private static final float ZOOM_SCALE = 0.6f;

    private static final float CUBE_DEGREES = 90f;

    private static final float SPIN_DEGREES = 180f;

    private final int background;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    private final Camera camera = new Camera();

    private final Matrix matrix = new Matrix();

    private final Layer outgoing = new Layer();

    private final Layer incoming = new Layer();

    AvatarSlotView(Context context, int background) {
        super(context);
        this.background = background;
        this.incoming.alpha = 0f;
    }

    void setAvatars(Bitmap outgoing, Bitmap incoming) {
        this.outgoing.bitmap = outgoing;
        this.incoming.bitmap = incoming;
    }

    void setFrame(AvatarSwitchStyle style, float progress, int direction) {
        final float height = getHeight();
        final float firstHalf = Math.min(1f, 2f * progress);
        final float secondHalf = Math.max(0f, 2f * progress - 1f);
        this.outgoing.reset(1f);
        this.incoming.reset(progress);
        switch (style) {
            case SLIDE:
                this.outgoing.offset = progress * direction * height;
                this.incoming.offset = (progress - 1f) * direction * height;
                this.outgoing.alpha = 1f - progress;
                break;
            case CAROUSEL:
                this.outgoing.offset = progress * direction * height;
                this.incoming.offset = (progress - 1f) * direction * height;
                this.incoming.alpha = 1f;
                break;
            case FADE:
                break;
            case ZOOM:
                this.outgoing.scaleX = 1f - (1f - ZOOM_SCALE) * progress;
                this.outgoing.scaleY = this.outgoing.scaleX;
                this.outgoing.alpha = 1f - progress;
                this.incoming.scaleX = ZOOM_SCALE + (1f - ZOOM_SCALE) * progress;
                this.incoming.scaleY = this.incoming.scaleX;
                this.incoming.alpha = firstHalf;
                break;
            case FLIP:
                this.outgoing.scaleY = 1f - firstHalf;
                this.incoming.scaleY = secondHalf;
                this.incoming.alpha = 1f;
                break;
            case CUBE:
                setCubeFace(this.outgoing, progress * direction, height);
                setCubeFace(this.incoming, (progress - 1f) * direction, height);
                this.incoming.alpha = 1f;
                break;
            case SPIN:
                this.outgoing.rotation = progress * direction * SPIN_DEGREES;
                this.outgoing.alpha = 1f - progress;
                this.incoming.rotation = (progress - 1f) * direction * SPIN_DEGREES;
                this.incoming.alpha = firstHalf;
                break;
            default:
                this.outgoing.alpha = (progress < 0.5f) ? 1f : 0f;
                this.incoming.alpha = 1f - this.outgoing.alpha;
                break;
        }
        invalidate();
    }

    void setBounceOffset(float offset) {
        this.outgoing.offset = offset;
        invalidate();
    }

    static Bitmap drawAvatar(Context context, int width, int height, int inset, int fillColor, int textColor,
            String initials) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(fillColor);
        final float radius = Dimensions.dpExact(context, CORNER_RADIUS_DP);
        canvas.drawRoundRect(new RectF(inset, inset, width - inset, height - inset), radius, radius, fill);

        if (initials != null && !initials.isEmpty()) {
            final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(textColor);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            text.setTextSize(Dimensions.spExact(context, INITIALS_TEXT_SP));
            text.setTextAlign(Paint.Align.CENTER);
            final Paint.FontMetrics metrics = text.getFontMetrics();
            canvas.drawText(initials, width / 2f, height / 2f - (metrics.ascent + metrics.descent) / 2f, text);
        }
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipRect(0, 0, getWidth(), getHeight());
        canvas.drawColor(this.background);
        drawLayer(canvas, this.outgoing);
        drawLayer(canvas, this.incoming);
    }

    private static void setCubeFace(Layer layer, float position, float height) {
        layer.offset = position * height;
        layer.rotationX = -position * CUBE_DEGREES;
        layer.pivotY = (position < 0f) ? height : 0f;
    }

    private void drawLayer(Canvas canvas, Layer layer) {
        if (layer.bitmap == null || layer.alpha <= 0f || layer.scaleX <= 0f || layer.scaleY <= 0f) {
            return;
        }
        final float centerX = getWidth() / 2f;
        final float centerY = getHeight() / 2f;
        this.paint.setAlpha(Math.round(layer.alpha * 255));
        final int saved = canvas.save();
        canvas.translate(0f, layer.offset);
        if (layer.rotationX != 0f) {
            final float pivotY = (layer.pivotY >= 0f) ? layer.pivotY : centerY;
            this.camera.save();
            this.camera.rotateX(layer.rotationX);
            this.camera.getMatrix(this.matrix);
            this.camera.restore();
            this.matrix.preTranslate(-centerX, -pivotY);
            this.matrix.postTranslate(centerX, pivotY);
            canvas.concat(this.matrix);
        }
        canvas.rotate(layer.rotation, centerX, centerY);
        canvas.scale(layer.scaleX, layer.scaleY, centerX, centerY);
        canvas.drawBitmap(layer.bitmap, 0f, 0f, this.paint);
        canvas.restoreToCount(saved);
    }

    private static final class Layer {

        private Bitmap bitmap;

        private float offset;

        private float alpha = 1f;

        private float scaleX = 1f;

        private float scaleY = 1f;

        private float rotation;

        private float rotationX;

        private float pivotY = -1f;

        void reset(float alpha) {
            this.offset = 0f;
            this.alpha = alpha;
            this.scaleX = 1f;
            this.scaleY = 1f;
            this.rotation = 0f;
            this.rotationX = 0f;
            this.pivotY = -1f;
        }

    }

}
