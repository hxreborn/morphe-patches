/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.view.Gravity;
import android.widget.Switch;

final class WebToggleSwitchStyle {

    private static final int TRACK_WIDTH_DP = 52;

    private static final int TRACK_HEIGHT_DP = 32;

    private static final int OUTLINE_DP = 2;

    private static final int THUMB_DP = 24;

    private static final int THUMB_INSET_DP = 4;

    private static final int ICON_DP = 16;

    private static final int INTERACTION_NORM = 0xFF6D4AFF;

    private static final int DARK_OUTLINE = 0xFFA7A4B5;

    private static final int LIGHT_OUTLINE = 0xFF5C5958;

    private static final int DARK_SURFACE = 0xFF292733;

    private static final int LIGHT_SURFACE = 0xFFF5F4F2;

    private static final int UNCHECKED_TRACK_ALPHA = 0x33;

    private static final int[] CHECKED = { android.R.attr.state_checked };

    private WebToggleSwitchStyle() {

    }

    static void apply(Switch control) {
        final Context context = control.getContext();
        final boolean night = PatchesTheme.isNightMode(context);
        final int outline = night ? DARK_OUTLINE : LIGHT_OUTLINE;
        final int surface = night ? DARK_SURFACE : LIGHT_SURFACE;
        final int travel = dp(context, TRACK_WIDTH_DP - THUMB_DP - 2 * THUMB_INSET_DP);
        final int thumbOverhang = (dp(context, THUMB_DP) - travel) / 2;

        final StateListDrawable trackStates = new StateListDrawable();
        trackStates.addState(CHECKED, track(context, INTERACTION_NORM, 0, INTERACTION_NORM));
        trackStates.addState(StateSet.WILD_CARD, track(context, (outline & 0x00FFFFFF) | (UNCHECKED_TRACK_ALPHA << 24),
                dp(context, OUTLINE_DP), outline));
        final LayerDrawable track = new LayerDrawable(new Drawable[] { trackStates });
        final int trackPadding = dp(context, THUMB_INSET_DP) + thumbOverhang;
        track.setPadding(trackPadding, 0, trackPadding, 0);

        final StateListDrawable thumb = new StateListDrawable();
        thumb.addState(CHECKED, thumb(context, 0xFFFFFFFF, new MarkDrawable(INTERACTION_NORM, true), thumbOverhang));
        thumb.addState(StateSet.WILD_CARD, thumb(context, outline, new MarkDrawable(surface, false), thumbOverhang));

        control.setShowText(false);
        control.setSplitTrack(false);
        control.setSwitchMinWidth(dp(context, TRACK_WIDTH_DP));
        control.setThumbDrawable(thumb);
        control.setTrackDrawable(track);
        control.refreshDrawableState();
    }

    private static Drawable track(Context context, int fill, int strokeWidth, int strokeColor) {
        final GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dp(context, TRACK_HEIGHT_DP) / 2f);
        shape.setColor(fill);
        shape.setStroke(strokeWidth, strokeColor);
        shape.setSize(dp(context, TRACK_WIDTH_DP), dp(context, TRACK_HEIGHT_DP));
        return shape;
    }

    private static Drawable thumb(Context context, int fill, Drawable mark, int overhang) {
        final GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(fill);

        final LayerDrawable thumb = new LayerDrawable(new Drawable[] { circle, mark });
        final int thumbSize = dp(context, THUMB_DP);
        final int iconSize = dp(context, ICON_DP);
        thumb.setLayerSize(0, thumbSize, thumbSize);
        thumb.setLayerSize(1, iconSize, iconSize);
        for (int layer = 0; layer < 2; layer++) {
            thumb.setLayerGravity(layer, Gravity.CENTER);
            thumb.setLayerInset(layer, -overhang, 0, -overhang, 0);
        }
        return thumb;
    }

    private static int dp(Context context, int value) {
        return PatchesTheme.dpToPx(context, value);
    }

    private static final class MarkDrawable extends Drawable {

        private static final float VIEWPORT = 16f;

        private static final float STROKE = 1.5f;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Path path = new Path();

        private final boolean check;

        MarkDrawable(int color, boolean check) {
            this.check = check;
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            final float scale = bounds.width() / VIEWPORT;
            this.paint.setStrokeWidth(STROKE * scale);
            this.path.reset();
            if (this.check) {
                this.path.moveTo(3.5f, 8.5f);
                this.path.lineTo(6.5f, 11.5f);
                this.path.lineTo(12.5f, 4.5f);
            } else {
                this.path.moveTo(4.5f, 4.5f);
                this.path.lineTo(11.5f, 11.5f);
                this.path.moveTo(11.5f, 4.5f);
                this.path.lineTo(4.5f, 11.5f);
            }
            final Matrix matrix = new Matrix();
            matrix.setScale(scale, scale);
            matrix.postTranslate(bounds.left, bounds.top);
            this.path.transform(matrix);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawPath(this.path, this.paint);
        }

        @Override
        public void setAlpha(int alpha) {
            this.paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

    }

}
