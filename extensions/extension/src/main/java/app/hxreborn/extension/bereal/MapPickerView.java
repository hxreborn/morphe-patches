/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import app.hxreborn.extension.shared.Dimensions;

final class MapPickerView extends View implements OsmTiles.Listener {

    private static final int TILE_DP = 256;

    private static final float MIN_ZOOM = 2f;

    private static final float MAX_ZOOM = 19f;

    private static final double MAX_LATITUDE = 85.0511287798;

    private static final int BACKGROUND = 0xFF2C2C2E;

    private static final int PIN = 0xFFFF3B30;

    private static final int PIN_DOT = 0xFFFFFFFF;

    private static final int ATTRIBUTION_SURFACE = 0xB3000000;

    private static final int ATTRIBUTION_TEXT = 0xFFFFFFFF;

    private static final String ATTRIBUTION = "© OpenStreetMap contributors";

    private static final int ANCESTOR_LEVELS = 3;

    private final OsmTiles tiles = new OsmTiles(this);

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path pinPath = new Path();

    private final Rect source = new Rect();

    private final RectF destination = new RectF();

    private final GestureDetector gestures;

    private final ScaleGestureDetector scaleGestures;

    private double centerX = 0.5;

    private double centerY = 0.4;

    private float zoom = MIN_ZOOM;

    private boolean pinMoved;

    private Listener listener;

    MapPickerView(Context context) {
        super(context);
        this.textPaint.setTextSize(Dimensions.spExact(context, 11f));
        this.gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent start, MotionEvent event, float distanceX, float distanceY) {
                final double world = worldSize();
                setCenter(MapPickerView.this.centerX + distanceX / world,
                        MapPickerView.this.centerY + distanceY / world);
                MapPickerView.this.pinMoved = true;
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                final double world = worldSize();
                setCenter(MapPickerView.this.centerX + (event.getX() - getWidth() / 2f) / world,
                        MapPickerView.this.centerY + (event.getY() - getHeight() / 2f) / world);
                MapPickerView.this.pinMoved = true;
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent event) {
                zoomAround(event.getX(), event.getY(), MapPickerView.this.zoom + 1f);
                MapPickerView.this.pinMoved = true;
                return true;
            }
        });
        this.scaleGestures = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                final float delta = (float) (Math.log(detector.getScaleFactor()) / Math.log(2));
                zoomAround(detector.getFocusX(), detector.getFocusY(), MapPickerView.this.zoom + delta);
                MapPickerView.this.pinMoved = true;
                return true;
            }
        });
        setContentDescription("Map. Drag or tap to move the pin.");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void moveTo(double latitude, double longitude, float targetZoom) {
        this.zoom = clampZoom(targetZoom);
        this.centerX = (longitude + 180.0) / 360.0;
        final double clamped = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, latitude));
        final double sin = Math.sin(Math.toRadians(clamped));
        this.centerY = 0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI);
        setCenter(this.centerX, this.centerY);
    }

    void zoomBy(float delta) {
        zoomAround(getWidth() / 2f, getHeight() / 2f, this.zoom + delta);
    }

    double latitude() {
        final double n = Math.PI * (1 - 2 * this.centerY);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    double longitude() {
        return this.centerX * 360.0 - 180.0;
    }

    @Override
    public void onTileLoaded() {
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        this.scaleGestures.onTouchEvent(event);
        if (!this.scaleGestures.isInProgress()) {
            this.gestures.onTouchEvent(event);
        }
        final int action = event.getActionMasked();
        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && this.pinMoved) {
            this.pinMoved = false;
            if (this.listener != null) {
                this.listener.onChosen(latitude(), longitude());
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.tiles.shutdown();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BACKGROUND);
        final double world = worldSize();
        final int level = (int) Math.floor(this.zoom);
        final int count = 1 << level;
        final double tileSize = world / count;
        final double left = this.centerX * world - getWidth() / 2.0;
        final double top = this.centerY * world - getHeight() / 2.0;
        final int firstX = (int) Math.floor(left / tileSize);
        final int lastX = (int) Math.floor((left + getWidth()) / tileSize);
        final int firstY = Math.max(0, (int) Math.floor(top / tileSize));
        final int lastY = Math.min(count - 1, (int) Math.floor((top + getHeight()) / tileSize));
        this.tiles.setWindow(level, firstX, lastX, firstY, lastY);
        for (int x = firstX; x <= lastX; x++) {
            for (int y = firstY; y <= lastY; y++) {
                final int wrapped = OsmTiles.wrap(x, count);
                this.destination.set((float) (x * tileSize - left), (float) (y * tileSize - top),
                        (float) ((x + 1) * tileSize - left), (float) ((y + 1) * tileSize - top));
                final Bitmap tile = this.tiles.get(level, wrapped, y);
                if (tile != null) {
                    canvas.drawBitmap(tile, null, this.destination, this.bitmapPaint);
                } else {
                    drawAncestor(canvas, level, wrapped, y);
                    this.tiles.request(level, wrapped, y);
                }
            }
        }
        drawPin(canvas);
        drawAttribution(canvas);
    }

    private void drawAncestor(Canvas canvas, int level, int x, int y) {
        for (int up = 1; up <= ANCESTOR_LEVELS && up <= level; up++) {
            final Bitmap parent = this.tiles.get(level - up, x >> up, y >> up);
            if (parent == null) {
                continue;
            }
            final int span = parent.getWidth() >> up;
            final int mask = (1 << up) - 1;
            this.source.set((x & mask) * span, (y & mask) * span, ((x & mask) + 1) * span, ((y & mask) + 1) * span);
            canvas.drawBitmap(parent, this.source, this.destination, this.bitmapPaint);
            return;
        }
    }

    private void drawPin(Canvas canvas) {
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        final float radius = Dimensions.dpExact(getContext(), 11f);
        final float headY = cy - radius * 2.2f;
        this.pinPath.reset();
        this.pinPath.moveTo(cx, cy);
        this.pinPath.lineTo(cx - radius * 0.87f, headY + radius * 0.5f);
        this.pinPath.lineTo(cx + radius * 0.87f, headY + radius * 0.5f);
        this.pinPath.close();
        this.fillPaint.setColor(PIN);
        canvas.drawPath(this.pinPath, this.fillPaint);
        canvas.drawCircle(cx, headY, radius, this.fillPaint);
        this.fillPaint.setColor(PIN_DOT);
        canvas.drawCircle(cx, headY, radius * 0.38f, this.fillPaint);
    }

    private void drawAttribution(Canvas canvas) {
        final float padding = Dimensions.dpExact(getContext(), 6f);
        final float width = this.textPaint.measureText(ATTRIBUTION);
        final float height = this.textPaint.getTextSize();
        final float right = Dimensions.dpExact(getContext(), 8f) + width + padding * 2;
        final float bottom = getHeight() - Dimensions.dpExact(getContext(), 8f);
        this.destination.set(right - width - padding * 2, bottom - height - padding * 2, right, bottom);
        this.fillPaint.setColor(ATTRIBUTION_SURFACE);
        canvas.drawRoundRect(this.destination, padding, padding, this.fillPaint);
        this.textPaint.setColor(ATTRIBUTION_TEXT);
        canvas.drawText(ATTRIBUTION, this.destination.left + padding, bottom - padding - this.textPaint.descent(),
                this.textPaint);
    }

    private double worldSize() {
        return Dimensions.dpExact(getContext(), TILE_DP) * Math.pow(2, this.zoom);
    }

    private void setCenter(double x, double y) {
        this.centerX = x - Math.floor(x);
        this.centerY = Math.max(0.0, Math.min(1.0, y));
        invalidate();
    }

    private void zoomAround(float focusX, float focusY, float targetZoom) {
        final float clamped = clampZoom(targetZoom);
        final double before = worldSize();
        final double worldX = this.centerX + (focusX - getWidth() / 2.0) / before;
        final double worldY = this.centerY + (focusY - getHeight() / 2.0) / before;
        this.zoom = clamped;
        final double after = worldSize();
        setCenter(worldX - (focusX - getWidth() / 2.0) / after, worldY - (focusY - getHeight() / 2.0) / after);
    }

    private static float clampZoom(float value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }

    interface Listener {

        void onChosen(double latitude, double longitude);

    }

}
