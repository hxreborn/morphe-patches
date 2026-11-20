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
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import app.hxreborn.extension.shared.Dimensions;

final class TileMapView extends View {

    static final int TILE_SIZE = 256;

    static final int MIN_ZOOM = 0;

    static final int MAX_ZOOM = 19;

    static final double MAX_LATITUDE = 85.05112878;

    private static final String ATTRIBUTION = "\u00a9 OpenStreetMap contributors";

    private static final String COPYRIGHT_URL = "https://www.openstreetmap.org/copyright";

    private static final double HALF = 0.5;

    private static final double HALF_TURN_DEGREES = 180.0;

    private static final double FULL_TURN_DEGREES = 360.0;

    private static final double LOG_TWO = Math.log(2);

    private static final double INITIAL_ZOOM = 1;

    private static final double SINGLE_MARKER_ZOOM = 8;

    private static final double MAX_FIT_ZOOM = 12;

    private static final int FIT_PADDING_DP = 48;

    private static final int ANIMATION_MILLIS = 400;

    private static final int GRATICULE_STEP = 30;

    private static final int GRATICULE_LAT_MAX = 60;

    private static final int GRATICULE_LON_MAX = 180;

    private static final float GRATICULE_STROKE_DP = 0.5f;

    private static final float ROUTE_STROKE_DP = 2f;

    private static final float MARKER_RADIUS_DP = 7f;

    private static final float MARKER_OUTLINE_DP = 2f;

    private static final float HIT_RADIUS_DP = 24f;

    private static final float ATTRIBUTION_TEXT_SP = 10f;

    private static final float ATTRIBUTION_PADDING_DP = 4f;

    private static final float SEAM_OVERLAP_PX = 0.5f;

    private static final int ROUTE_COLOR = 0xFFF44336;

    private static final int HOP_COLOR = 0xFFF44336;

    private static final int DESTINATION_COLOR = 0xFF4CAF50;

    private static final int USER_COLOR = 0xFF00BCD4;

    private static final int OUTLINE_COLOR = 0xFFFFFFFF;

    private static final int ATTRIBUTION_BACKGROUND_COLOR = 0xB3FFFFFF;

    private static final int ATTRIBUTION_TEXT_COLOR = 0xFF333333;

    private static final float DARK_DIM = 0.8f;

    private static final float GRAY_AXIS_SHARE = 2f / 3f;

    private static final float CHANNEL_MAX = 255f;

    private static final float TILE_DENSITY_FACTOR = 0.5f;

    private final HostPalette palette;

    private final TileCache cache;

    private final Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final Paint graticulePaint = new Paint();

    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint markerFill = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint markerOutline = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint attributionText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint attributionBackground = new Paint();

    private final Path route = new Path();

    private final RectF tileBounds = new RectF();

    private final RectF attributionBounds = new RectF();

    private final Set<String> wanted = new HashSet<>();

    private final GestureDetector gestures;

    private final ScaleGestureDetector scaler;

    private List<MapMarker> markers = Collections.emptyList();

    private MarkerClickListener markerClickListener;

    private ValueAnimator animator;

    private double centerU = HALF;

    private double centerV = HALF;

    private double zoom = INITIAL_ZOOM;

    private boolean fitPending;

    private final float tileScale;

    TileMapView(Context context, HostPalette palette) {
        super(context);
        this.palette = palette;
        this.cache = new TileCache(context, this::postInvalidate);
        this.tileScale = Math.max(1f, displayMetrics().density * TILE_DENSITY_FACTOR);
        if (palette.dark) {
            this.tilePaint.setColorFilter(new ColorMatrixColorFilter(lightnessInversion()));
        }
        this.graticulePaint.setColor(palette.divider);
        this.graticulePaint.setStrokeWidth(dp(GRATICULE_STROKE_DP));
        this.routePaint.setColor(ROUTE_COLOR);
        this.routePaint.setStyle(Paint.Style.STROKE);
        this.routePaint.setStrokeWidth(dp(ROUTE_STROKE_DP));
        this.routePaint.setStrokeJoin(Paint.Join.ROUND);
        this.markerFill.setStyle(Paint.Style.FILL);
        this.markerOutline.setStyle(Paint.Style.STROKE);
        this.markerOutline.setColor(OUTLINE_COLOR);
        this.markerOutline.setStrokeWidth(dp(MARKER_OUTLINE_DP));
        this.attributionText.setColor(ATTRIBUTION_TEXT_COLOR);
        this.attributionText.setTextSize(Dimensions.spExact(context, ATTRIBUTION_TEXT_SP));
        this.attributionBackground.setColor(ATTRIBUTION_BACKGROUND_COLOR);
        this.gestures = new GestureDetector(context, new GestureListener());
        this.scaler = new ScaleGestureDetector(context, new ScaleListener());
    }

    private static ColorMatrix lightnessInversion() {
        final float own = DARK_DIM * (1 - GRAY_AXIS_SHARE);
        final float other = -DARK_DIM * GRAY_AXIS_SHARE;
        final float offset = DARK_DIM * CHANNEL_MAX;
        return new ColorMatrix(new float[] { own, other, other, 0, offset, other, own, other, 0, offset, other, other,
                own, 0, offset, 0, 0, 0, 1, 0 });
    }

    static double worldSize(double zoom) {
        return TILE_SIZE * Math.pow(2, zoom);
    }

    static double mercatorU(double longitude) {
        return (longitude + HALF_TURN_DEGREES) / FULL_TURN_DEGREES;
    }

    static double mercatorV(double latitude) {
        final double clamped = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, latitude));
        final double sine = Math.sin(Math.toRadians(clamped));
        return HALF - Math.log((1 + sine) / (1 - sine)) / (4 * Math.PI);
    }

    void setMarkers(List<MapMarker> markers, boolean animate) {
        final boolean moved = !samePositions(this.markers, markers);
        this.markers = new ArrayList<>(markers);
        if (moved && !markers.isEmpty()) {
            fit(animate);
        }
        invalidate();
    }

    void setOnMarkerClickListener(MarkerClickListener listener) {
        this.markerClickListener = listener;
    }

    void release() {
        cancelAnimation();
        this.cache.release();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (this.fitPending) {
            fit(false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(this.palette.background);
        final double size = displaySize(this.zoom);
        drawGraticule(canvas, size);
        drawTiles(canvas);
        final float[] points = project(size);
        drawRoute(canvas, points);
        drawMarkers(canvas, points);
        drawAttribution(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cancelAnimation();
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        final boolean scaled = this.scaler.onTouchEvent(event);
        final boolean gestured = this.gestures.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return scaled || gestured;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void drawGraticule(Canvas canvas, double size) {
        final float halfWidth = getWidth() / 2f;
        final float halfHeight = getHeight() / 2f;
        for (int latitude = -GRATICULE_LAT_MAX; latitude <= GRATICULE_LAT_MAX; latitude += GRATICULE_STEP) {
            final float y = (float) (halfHeight + (mercatorV(latitude) - this.centerV) * size);
            canvas.drawLine(0, y, getWidth(), y, this.graticulePaint);
        }
        for (int longitude = -GRATICULE_LON_MAX; longitude < GRATICULE_LON_MAX; longitude += GRATICULE_STEP) {
            double x = halfWidth + (mercatorU(longitude) - this.centerU) * size;
            x -= Math.floor(x / size) * size;
            for (; x < getWidth(); x += size) {
                canvas.drawLine((float) x, 0, (float) x, getHeight(), this.graticulePaint);
            }
        }
    }

    private void drawTiles(Canvas canvas) {
        final int tileZoom = (int) Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.round(this.zoom)));
        final int count = 1 << tileZoom;
        final float scale = (float) Math.pow(2, this.zoom - tileZoom);
        final double tilePixels = TILE_SIZE * this.tileScale;
        final double centerX = this.centerU * tilePixels * count;
        final double centerY = this.centerV * tilePixels * count;
        final float halfWidth = getWidth() / 2f;
        final float halfHeight = getHeight() / 2f;
        final int firstX = (int) Math.floor((centerX - halfWidth / scale) / tilePixels);
        final int lastX = (int) Math.floor((centerX + halfWidth / scale) / tilePixels);
        final int firstY = Math.max(0, (int) Math.floor((centerY - halfHeight / scale) / tilePixels));
        final int lastY = Math.min(count - 1, (int) Math.floor((centerY + halfHeight / scale) / tilePixels));
        this.wanted.clear();
        for (int tileX = firstX; tileX <= lastX; tileX++) {
            final int wrappedX = Math.floorMod(tileX, count);
            for (int tileY = firstY; tileY <= lastY; tileY++) {
                this.wanted.add(TileCache.key(tileZoom, wrappedX, tileY));
                this.cache.request(tileZoom, wrappedX, tileY);
                final Bitmap bitmap = this.cache.get(tileZoom, wrappedX, tileY);
                if (bitmap != null) {
                    final float left = (float) (halfWidth + (tileX * tilePixels - centerX) * scale);
                    final float top = (float) (halfHeight + (tileY * tilePixels - centerY) * scale);
                    final float side = (float) (tilePixels * scale);
                    this.tileBounds.set(left, top, left + side + SEAM_OVERLAP_PX, top + side + SEAM_OVERLAP_PX);
                    canvas.drawBitmap(bitmap, null, this.tileBounds, this.tilePaint);
                }
            }
        }
        for (int tileX = firstX - 1; tileX <= lastX + 1; tileX++) {
            final int wrappedX = Math.floorMod(tileX, count);
            for (int tileY = Math.max(0, firstY - 1); tileY <= Math.min(count - 1, lastY + 1); tileY++) {
                if (this.wanted.add(TileCache.key(tileZoom, wrappedX, tileY))) {
                    this.cache.request(tileZoom, wrappedX, tileY);
                }
            }
        }
        this.cache.retain(this.wanted);
    }

    private void drawRoute(Canvas canvas, float[] points) {
        if (points.length < 4) {
            return;
        }
        this.route.rewind();
        this.route.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            this.route.lineTo(points[i], points[i + 1]);
        }
        canvas.drawPath(this.route, this.routePaint);
    }

    private void drawMarkers(Canvas canvas, float[] points) {
        final float radius = dp(MARKER_RADIUS_DP);
        final float outlineRadius = radius - dp(MARKER_OUTLINE_DP) / 2f;
        for (int i = 0; i < this.markers.size(); i++) {
            this.markerFill.setColor(colorOf(this.markers.get(i).type));
            canvas.drawCircle(points[2 * i], points[2 * i + 1], radius, this.markerFill);
            canvas.drawCircle(points[2 * i], points[2 * i + 1], outlineRadius, this.markerOutline);
        }
    }

    private void drawAttribution(Canvas canvas) {
        final float padding = dp(ATTRIBUTION_PADDING_DP);
        final float width = this.attributionText.measureText(ATTRIBUTION) + 2 * padding;
        final float height = this.attributionText.getFontSpacing() + 2 * padding;
        this.attributionBounds.set(getWidth() - width, getHeight() - height, getWidth(), getHeight());
        canvas.drawRect(this.attributionBounds, this.attributionBackground);
        canvas.drawText(ATTRIBUTION, this.attributionBounds.left + padding,
                this.attributionBounds.bottom - padding - this.attributionText.descent(), this.attributionText);
    }

    private float[] project(double size) {
        final float[] points = new float[2 * this.markers.size()];
        double previousU = this.centerU;
        for (int i = 0; i < this.markers.size(); i++) {
            final MapMarker marker = this.markers.get(i);
            double offset = mercatorU(marker.longitude) - previousU;
            offset -= Math.rint(offset);
            previousU += offset;
            points[2 * i] = (float) (getWidth() / 2f + (previousU - this.centerU) * size);
            points[2 * i + 1] = (float) (getHeight() / 2f + (mercatorV(marker.latitude) - this.centerV) * size);
        }
        return points;
    }

    private static int colorOf(int type) {
        switch (type) {
            case MapMarker.TYPE_DESTINATION:
                return DESTINATION_COLOR;
            case MapMarker.TYPE_USER_LOCATION:
                return USER_COLOR;
            default:
                return HOP_COLOR;
        }
    }

    private static boolean samePositions(List<MapMarker> first, List<MapMarker> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).latitude != second.get(i).latitude || first.get(i).longitude != second.get(i).longitude) {
                return false;
            }
        }
        return true;
    }

    private void fit(boolean animate) {
        if (getWidth() == 0 || getHeight() == 0) {
            this.fitPending = true;
            return;
        }
        this.fitPending = false;
        final double firstU = mercatorU(this.markers.get(0).longitude);
        double minOffset = 0;
        double maxOffset = 0;
        double previousU = firstU;
        double minV = Double.MAX_VALUE;
        double maxV = -Double.MAX_VALUE;
        for (MapMarker marker : this.markers) {
            double offset = mercatorU(marker.longitude) - previousU;
            offset -= Math.rint(offset);
            previousU += offset;
            minOffset = Math.min(minOffset, previousU - firstU);
            maxOffset = Math.max(maxOffset, previousU - firstU);
            final double v = mercatorV(marker.latitude);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        final double spanU = maxOffset - minOffset;
        final double spanV = maxV - minV;
        double targetZoom = SINGLE_MARKER_ZOOM;
        if (spanU > 0 || spanV > 0) {
            final double padding = 2 * dp(FIT_PADDING_DP);
            final double availableWidth = Math.max(1, getWidth() - padding);
            final double availableHeight = Math.max(1, getHeight() - padding);
            final double zoomU = (spanU > 0) ? Math.log(availableWidth / (TILE_SIZE * this.tileScale * spanU)) / LOG_TWO
                    : MAX_FIT_ZOOM;
            final double zoomV = (spanV > 0)
                    ? Math.log(availableHeight / (TILE_SIZE * this.tileScale * spanV)) / LOG_TWO : MAX_FIT_ZOOM;
            targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_FIT_ZOOM, Math.min(zoomU, zoomV)));
        }
        moveTo(firstU + (minOffset + maxOffset) * HALF, (minV + maxV) * HALF, targetZoom, animate);
    }

    private void moveTo(double targetU, double targetV, double targetZoom, boolean animate) {
        cancelAnimation();
        if (!animate) {
            this.centerU = targetU;
            this.centerV = targetV;
            this.zoom = targetZoom;
            invalidate();
            return;
        }
        final double startU = this.centerU;
        final double startV = this.centerV;
        final double startZoom = this.zoom;
        final double offset = targetU - startU - Math.rint(targetU - startU);
        this.animator = ValueAnimator.ofFloat(0f, 1f);
        this.animator.setDuration(ANIMATION_MILLIS);
        this.animator.addUpdateListener((animation) -> {
            final double fraction = (float) animation.getAnimatedValue();
            this.centerU = startU + offset * fraction;
            this.centerV = startV + (targetV - startV) * fraction;
            this.zoom = startZoom + (targetZoom - startZoom) * fraction;
            invalidate();
        });
        this.animator.start();
    }

    private void cancelAnimation() {
        if (this.animator != null) {
            this.animator.cancel();
            this.animator = null;
        }
    }

    private void zoomAbout(float focusX, float focusY, double newZoom) {
        final double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        final double oldSize = displaySize(this.zoom);
        final double newSize = displaySize(clamped);
        final double offsetX = focusX - getWidth() / 2f;
        final double offsetY = focusY - getHeight() / 2f;
        this.centerU += offsetX / oldSize - offsetX / newSize;
        this.centerV += offsetY / oldSize - offsetY / newSize;
        this.zoom = clamped;
        clampCenter();
        invalidate();
    }

    private void clampCenter() {
        this.centerU -= Math.floor(this.centerU);
        this.centerV = Math.max(0, Math.min(1, this.centerV));
    }

    private MapMarker markerAt(float x, float y) {
        final float[] points = project(displaySize(this.zoom));
        final float reach = dp(HIT_RADIUS_DP);
        float best = reach * reach;
        MapMarker nearest = null;
        for (int i = 0; i < this.markers.size(); i++) {
            final float dx = points[2 * i] - x;
            final float dy = points[2 * i + 1] - y;
            final float distance = dx * dx + dy * dy;
            if (distance <= best) {
                best = distance;
                nearest = this.markers.get(i);
            }
        }
        return nearest;
    }

    private void openCopyright() {
        try {
            getContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(COPYRIGHT_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException ex) {
            return;
        }
    }

    private double displaySize(double zoom) {
        return worldSize(zoom) * this.tileScale;
    }

    private android.util.DisplayMetrics displayMetrics() {
        return getResources().getDisplayMetrics();
    }

    private float dp(float value) {
        return Dimensions.dpExact(getContext(), value);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent second, float distanceX, float distanceY) {
            final double size = displaySize(TileMapView.this.zoom);
            TileMapView.this.centerU += distanceX / size;
            TileMapView.this.centerV += distanceY / size;
            clampCenter();
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            zoomAbout(event.getX(), event.getY(), TileMapView.this.zoom + 1);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (TileMapView.this.attributionBounds.contains(event.getX(), event.getY())) {
                openCopyright();
                return true;
            }
            final MapMarker marker = markerAt(event.getX(), event.getY());
            if (marker != null && TileMapView.this.markerClickListener != null) {
                TileMapView.this.markerClickListener.onMarkerClick(marker);
            }
            return marker != null;
        }

    }

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            zoomAbout(detector.getFocusX(), detector.getFocusY(),
                    TileMapView.this.zoom + Math.log(detector.getScaleFactor()) / LOG_TWO);
            return true;
        }

    }

}
