/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.hxreborn.extension.shared.Dimensions;

final class GalleryAlbumDialog {

    private static final String TAG = "GalleryAlbumDialog";

    private static final int CACHE_BYTES = 8 * 1024 * 1024;

    private static final int SURFACE = 0xFF1C1C1E;

    private static final int TILE_FILL = 0xFF2C2C2E;

    private static final int HANDLE_FILL = 0xFF48484A;

    private static final int GREY = 0xFF8E8E93;

    private static final int LABEL_SCRIM = 0xCC000000;

    private static final int CORNER_DP = 20;

    private static final int TILE_RADIUS_DP = 8;

    private static final int SIDE_DP = 12;

    private static final int GAP_DP = 4;

    private static final int HANDLE_ZONE_DP = 24;

    private static final int HEADER_DP = 48;

    private static final int WIDE_SCREEN_DP = 600;

    private static final float MAX_HEIGHT_FRACTION = 0.55f;

    private final Activity activity;

    private final GalleryButton owner;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ExecutorService loader = Executors.newFixedThreadPool(2);

    private final List<View> tileViews = new ArrayList<>();

    private final LruCache<Uri, Bitmap> cache = new LruCache<Uri, Bitmap>(CACHE_BYTES) {
        @Override
        protected int sizeOf(Uri key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private Dialog dialog;

    private LinearLayout sheet;

    private LinearLayout body;

    private TextView step;

    private TextView title;

    private TextView clear;

    private boolean released;

    GalleryAlbumDialog(Activity activity, GalleryButton owner) {
        this.activity = activity;
        this.owner = owner;
    }

    void show() {
        if (this.released || isShowing() || this.activity.isFinishing() || this.activity.isDestroyed()) {
            return;
        }
        this.dialog = new Dialog(this.activity);
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.dialog.setCanceledOnTouchOutside(true);
        this.dialog.setContentView(buildSheet(),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.dialog.setOnDismissListener((dialogInterface) -> {
            this.dialog = null;
            this.tileViews.clear();
        });
        final Window window = this.dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        this.dialog.show();
        rebuild();
    }

    boolean isShowing() {
        return this.dialog != null && this.dialog.isShowing();
    }

    void dismiss() {
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
    }

    void release() {
        this.released = true;
        dismiss();
        this.loader.shutdownNow();
        this.cache.evictAll();
    }

    void rebuild() {
        if (!isShowing()) {
            return;
        }
        this.body.removeAllViews();
        this.tileViews.clear();
        final int columns = (this.activity.getResources().getConfiguration().screenWidthDp >= WIDE_SCREEN_DP) ? 4 : 3;
        final int gap = dp(GAP_DP);
        final int available = this.activity.getResources().getDisplayMetrics().widthPixels - 2 * dp(SIDE_DP);
        final int side = (available - (columns - 1) * gap) / columns;

        final List<Uri> photos = this.owner.tiles();
        final int count = photos.size() + 1;
        LinearLayout row = null;
        for (int index = 0; index < count; index++) {
            if (index % columns == 0) {
                row = new LinearLayout(this.activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = gap;
                this.body.addView(row, rowParams);
            }
            final View tile = (index == 0) ? allPhotosTile() : photoTile(photos.get(index - 1), side);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(side, side);
            if (index % columns != columns - 1) {
                params.setMarginEnd(gap);
            }
            row.addView(tile, params);
        }
        if (!RecentPhotos.hasAccess(this.activity)) {
            final TextView note = label(13, Typeface.NORMAL, GREY);
            note.setSingleLine(false);
            note.setText("Allow photo access in Settings to see recent photos here.");
            note.setPadding(0, dp(8), 0, dp(8));
            this.body.addView(note);
        }
        refresh();
    }

    void refresh() {
        if (!isShowing()) {
            return;
        }
        final PickSelection<Uri> selection = this.owner.selection();
        switch (selection.step()) {
            case BACK:
                this.step.setText("1/2");
                this.title.setText("Back photo");
                break;
            case FRONT:
                this.step.setText("2/2");
                this.title.setText("Front photo");
                break;
            default:
                this.step.setText("");
                this.title.setText(this.owner.isSaving() ? "Saving photos" : "Back and front set");
                break;
        }
        this.step.setVisibility((selection.step() == PickSelection.Step.DONE) ? View.GONE : View.VISIBLE);
        this.clear.setVisibility(selection.isEmpty() ? View.GONE : View.VISIBLE);
        this.clear.setEnabled(!this.owner.isSaving());
        for (View tile : this.tileViews) {
            final Object tag = tile.getTag();
            final TextView badge = (TextView) ((FrameLayout) tile).getChildAt(1);
            final boolean isBack = tag != null && tag.equals(selection.back());
            final boolean isFront = tag != null && tag.equals(selection.front());
            badge.setVisibility((isBack || isFront) ? View.VISIBLE : View.GONE);
            badge.setText(isBack ? "Back" : "Front");
            tile.setForeground((isBack || isFront) ? border() : null);
        }
    }

    private LinearLayout buildSheet() {
        this.sheet = new LinearLayout(this.activity);
        this.sheet.setOrientation(LinearLayout.VERTICAL);
        final float corner = Dimensions.dpExact(this.activity, CORNER_DP);
        final GradientDrawable background = new GradientDrawable();
        background.setColor(SURFACE);
        background.setCornerRadii(new float[] { corner, corner, corner, corner, 0, 0, 0, 0 });
        this.sheet.setBackground(background);
        this.sheet.setPadding(0, 0, 0, dp(8));

        this.sheet.addView(handleZone(),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(HANDLE_ZONE_DP)));
        this.sheet.addView(header(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(HEADER_DP)));

        this.body = new LinearLayout(this.activity);
        this.body.setOrientation(LinearLayout.VERTICAL);
        this.body.setPadding(dp(SIDE_DP), 0, dp(SIDE_DP), 0);
        final int screenHeight = this.activity.getResources().getDisplayMetrics().heightPixels;
        final int chrome = dp(HANDLE_ZONE_DP) + dp(HEADER_DP) + dp(8);
        final MaxHeightScrollView scroller = new MaxHeightScrollView(this.activity,
                (int) (screenHeight * MAX_HEIGHT_FRACTION) - chrome);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.addView(this.body,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.sheet.addView(scroller, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return this.sheet;
    }

    private View handleZone() {
        final FrameLayout zone = new FrameLayout(this.activity);
        final View handle = new View(this.activity);
        final GradientDrawable fill = new GradientDrawable();
        fill.setColor(HANDLE_FILL);
        fill.setCornerRadius(Dimensions.dpExact(this.activity, 2));
        handle.setBackground(fill);
        zone.addView(handle, new FrameLayout.LayoutParams(dp(36), dp(4), Gravity.CENTER));
        zone.setContentDescription("Close");
        final float dismissDistance = Dimensions.dpExact(this.activity, 72);
        final float[] downY = new float[1];
        zone.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY[0] = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    this.sheet.setTranslationY(Math.max(0, event.getRawY() - downY[0]));
                    return true;
                case MotionEvent.ACTION_UP:
                    final float distance = event.getRawY() - downY[0];
                    if (Math.abs(distance) < Dimensions.dpExact(this.activity, 8) || distance > dismissDistance) {
                        dismiss();
                    } else {
                        this.sheet.animate().translationY(0).setDuration(150).start();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    this.sheet.animate().translationY(0).setDuration(150).start();
                    return true;
                default:
                    return false;
            }
        });
        return zone;
    }

    private View header() {
        final LinearLayout header = new LinearLayout(this.activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(SIDE_DP + 4), 0, dp(SIDE_DP), 0);
        this.step = label(17, Typeface.BOLD, GREY);
        this.step.setPadding(0, 0, dp(8), 0);
        this.title = label(17, Typeface.BOLD, Color.WHITE);
        this.clear = label(15, Typeface.BOLD, Color.WHITE);
        this.clear.setText("Clear");
        this.clear.setGravity(Gravity.CENTER);
        this.clear.setMinWidth(dp(64));
        this.clear.setOnClickListener((view) -> this.owner.clearSelection());
        header.addView(this.step, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        header.addView(this.title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(this.clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return header;
    }

    private FrameLayout photoTile(Uri photo, int side) {
        final FrameLayout tile = roundedTile();
        tile.setTag(photo);
        tile.setContentDescription("Photo");
        final ImageView image = new ImageView(this.activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tile.addView(image,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final TextView badge = label(11, Typeface.BOLD, Color.WHITE);
        badge.setBackgroundColor(LABEL_SCRIM);
        badge.setGravity(Gravity.CENTER);
        badge.setVisibility(View.GONE);
        tile.addView(badge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(20), Gravity.BOTTOM));
        tile.setOnClickListener((view) -> this.owner.onTileClick(photo));
        this.tileViews.add(tile);
        loadThumbnail(photo, image, side);
        return tile;
    }

    private FrameLayout allPhotosTile() {
        final FrameLayout tile = roundedTile();
        final TextView text = label(13, Typeface.BOLD, Color.WHITE);
        text.setText("All photos");
        text.setGravity(Gravity.CENTER);
        tile.addView(text,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tile.setOnClickListener((view) -> this.owner.openAllPhotos());
        return tile;
    }

    private FrameLayout roundedTile() {
        final FrameLayout tile = new FrameLayout(this.activity);
        tile.setBackgroundColor(TILE_FILL);
        final float radius = Dimensions.dpExact(this.activity, TILE_RADIUS_DP);
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        tile.setClipToOutline(true);
        return tile;
    }

    private GradientDrawable border() {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(Dimensions.dpExact(this.activity, TILE_RADIUS_DP));
        drawable.setStroke(dp(2), Color.WHITE);
        return drawable;
    }

    private TextView label(float sp, int style, int color) {
        final TextView text = new TextView(this.activity);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setTextColor(color);
        text.setSingleLine(true);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private void loadThumbnail(Uri photo, ImageView image, int side) {
        final Bitmap cached = this.cache.get(photo);
        if (cached != null) {
            image.setImageBitmap(cached);
            return;
        }
        final Context context = this.activity.getApplicationContext();
        this.loader.execute(() -> {
            final Bitmap bitmap = decodeThumbnail(context, photo, side);
            if (bitmap == null) {
                return;
            }
            this.handler.post(() -> {
                if (this.released) {
                    return;
                }
                this.cache.put(photo, bitmap);
                image.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap decodeThumbnail(Context context, Uri photo, int side) {
        try {
            if ("file".equals(photo.getScheme())) {
                final BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(photo.getPath(), bounds);
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = Math.max(1, Math.min(bounds.outWidth, bounds.outHeight) / (side * 2));
                final Bitmap bitmap = BitmapFactory.decodeFile(photo.getPath(), options);
                return (bitmap != null) ? GalleryPhotos.rotate(new File(photo.getPath()), bitmap) : null;
            }
            return RecentPhotos.thumbnail(context.getContentResolver(), photo, side);
        } catch (IOException | SecurityException | IllegalArgumentException ex) {
            Log.e(TAG, "Thumbnail not loaded: " + photo, ex);
            return null;
        }
    }

    private int dp(float value) {
        return Dimensions.dp(this.activity, value);
    }

    private static final class MaxHeightScrollView extends ScrollView {

        private final int maxHeight;

        private MaxHeightScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(this.maxHeight, MeasureSpec.AT_MOST));
        }

    }

}
