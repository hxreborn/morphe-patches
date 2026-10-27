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
package app.hxreborn.extension.bereal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import app.hxreborn.extension.shared.Dimensions;

final class GalleryButton {

    private static final String TAG = "GalleryButton";

    private static final String VIEW_TAG = "hxreborn_gallery_button";

    private static final String HOST_ICON = "ic_photo";

    private static final long HOST_STATE_CHECK_MS = 500;

    private static final int RECENT_LIMIT = 60;

    private static final int BUTTON_DP = 44;

    private static final int TITLE_ROW_CENTER_DP = 32;

    private static final int END_DP = 12;

    private static final int ICON_DP = 22;

    private static final int BADGE_DP = 14;

    private static final int FILL = 0x66000000;

    private static final int RIPPLE = 0x33FFFFFF;

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor();

    private static boolean permissionRequested;

    private static volatile GalleryButton active;

    private final Activity activity;

    private final ViewGroup content;

    private final Object lifecycleOwner;

    private final Object cameraViewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final PickSelection<Uri> selection = new PickSelection<>();

    private final List<Uri> tiles = new ArrayList<>();

    private final List<Uri> staged = new ArrayList<>();

    private final List<Uri> recent = new ArrayList<>();

    private final ExecutorService loader = Executors.newSingleThreadExecutor();

    private final Uri[] stored = new Uri[2];

    private final FrameLayout button;

    private final TextView badge;

    private final GalleryAlbumDialog album;

    private Bitmap backPreview;

    private Bitmap frontPreview;

    private boolean insetsApplied;

    private boolean recentLoaded;

    private volatile int pendingWrites;

    private boolean removed;

    private final Runnable hostStateCheck = new Runnable() {
        @Override
        public void run() {
            GalleryButton.this.checkHostState();
        }
    };

    private GalleryButton(Activity activity, ViewGroup content, Object lifecycleOwner, Object cameraViewModel) {
        this.activity = activity;
        this.content = content;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraViewModel = cameraViewModel;
        this.album = new GalleryAlbumDialog(activity, this);

        this.button = new FrameLayout(activity);
        this.button.setTag(VIEW_TAG);
        this.button.setContentDescription("Pick photos from your gallery");
        this.button.setClickable(true);
        this.button.setBackground(circleRipple());
        this.button.setOnClickListener((view) -> this.album.show());
        this.button.setOnApplyWindowInsetsListener((view, insets) -> {
            if (!this.insetsApplied && insets.getSystemWindowInsetTop() > 0) {
                this.insetsApplied = true;
                topMargin(insets.getSystemWindowInsetTop());
            }
            return insets;
        });

        final View icon = icon();
        final int iconSize = dp(ICON_DP);
        this.button.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));

        this.badge = new TextView(activity);
        this.badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        this.badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        this.badge.setGravity(Gravity.CENTER);
        this.badge.setSingleLine(true);
        this.badge.setIncludeFontPadding(false);
        this.badge.setMinWidth(dp(BADGE_DP));
        this.badge.setPadding(dp(3), 0, dp(3), 0);
        this.badge.setVisibility(View.GONE);
        this.button.addView(this.badge, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(BADGE_DP),
                Gravity.BOTTOM | Gravity.END));
    }

    static void show(Activity activity, Object lifecycleOwner, Object cameraViewModel) {
        if (activity == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            final ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null || activity.isFinishing()) {
                return;
            }
            if (active != null && active.content == content && active.cameraViewModel == cameraViewModel
                    && active.lifecycleOwner == lifecycleOwner && content.findViewWithTag(VIEW_TAG) != null) {
                return;
            }
            if (active != null) {
                active.remove();
            }
            active = new GalleryButton(activity, content, lifecycleOwner, cameraViewModel);
            active.attach();
        });
    }

    static void picked(List<Uri> photos) {
        if (active != null && !photos.isEmpty()) {
            active.addStaged(photos);
        }
    }

    PickSelection<Uri> selection() {
        return this.selection;
    }

    List<Uri> tiles() {
        return this.tiles;
    }

    Bitmap preview(PickSelection.Side side) {
        return (side == PickSelection.Side.BACK) ? this.backPreview : this.frontPreview;
    }

    boolean isSaving() {
        return this.pendingWrites > 0;
    }

    static boolean hasPendingWrites() {
        final GalleryButton button = active;
        return button != null && button.pendingWrites > 0;
    }

    private void attach() {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(BUTTON_DP), dp(BUTTON_DP),
                Gravity.TOP | Gravity.END);
        params.setMarginEnd(dp(END_DP));
        params.topMargin = titleRowTop(statusBarHeight());
        this.content.addView(this.button, params);
        this.button.requestApplyInsets();
        final Context context = this.activity.getApplicationContext();
        WRITER.execute(() -> GalleryPhotos.clear(context));
        refresh();
        if (RecentPhotos.hasAccess(this.activity)) {
            loadRecent();
        } else if (!permissionRequested) {
            permissionRequested = true;
            this.activity.requestPermissions(RecentPhotos.permissions(), 0);
        }
        this.handler.postDelayed(this.hostStateCheck, HOST_STATE_CHECK_MS);
    }

    private void remove() {
        if (this.removed) {
            return;
        }
        this.removed = true;
        this.handler.removeCallbacks(this.hostStateCheck);
        this.album.release();
        this.content.removeView(this.button);
        this.loader.shutdownNow();
        if (active == this) {
            active = null;
        }
    }

    private void checkHostState() {
        if (this.removed) {
            return;
        }
        if (this.activity.isFinishing() || this.activity.isDestroyed()
                || CameraScreenState.lifecycleDestroyed(this.lifecycleOwner)) {
            remove();
            return;
        }
        if (!CameraScreenState.lifecycleActive(this.lifecycleOwner)) {
            this.album.dismiss();
            this.button.setVisibility(View.GONE);
            this.handler.postDelayed(this.hostStateCheck, HOST_STATE_CHECK_MS);
            return;
        }
        if (!this.recentLoaded && RecentPhotos.hasAccess(this.activity)) {
            loadRecent();
        }
        dropMissingFiles();
        showWhileCapturing();
        this.handler.postDelayed(this.hostStateCheck, HOST_STATE_CHECK_MS);
    }

    private void showWhileCapturing() {
        final boolean capturing = CameraScreenState.previewLive(this.cameraViewModel)
                && CameraScreenState.hasVisibleViewfinder(this.content);
        if (!capturing) {
            this.album.dismiss();
        }
        this.button.setVisibility(capturing ? View.VISIBLE : View.GONE);
    }

    private void dropMissingFiles() {
        boolean changed = false;
        final Iterator<Uri> iterator = this.staged.iterator();
        while (iterator.hasNext()) {
            if (!new File(iterator.next().getPath()).isFile()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            this.selection.retain(allTiles());
            combineTiles();
            this.album.rebuild();
            refresh();
        }
        if (this.pendingWrites == 0 && !this.selection.isEmpty() && !filesMatchSelection()) {
            this.selection.clear();
            refresh();
        }
    }

    private boolean filesMatchSelection() {
        return (this.selection.back() == null || GalleryPhotos.file(this.activity, false).isFile())
                && (this.selection.front() == null || GalleryPhotos.file(this.activity, true).isFile());
    }

    private List<Uri> allTiles() {
        final List<Uri> all = new ArrayList<>(this.staged);
        all.addAll(this.recent);
        return all;
    }

    private void combineTiles() {
        this.tiles.clear();
        this.tiles.addAll(allTiles());
    }

    private void loadRecent() {
        this.recentLoaded = true;
        final Context context = this.activity.getApplicationContext();
        this.loader.execute(() -> {
            final List<Uri> photos = RecentPhotos.recent(context.getContentResolver(), RECENT_LIMIT);
            this.handler.post(() -> {
                if (this.removed) {
                    return;
                }
                this.recent.clear();
                this.recent.addAll(photos);
                combineTiles();
                this.album.rebuild();
                refresh();
            });
        });
    }

    private void addStaged(List<Uri> photos) {
        if (photos.size() >= 2) {
            this.selection.clear();
        }
        this.staged.addAll(0, photos);
        for (Uri photo : photos) {
            this.selection.add(photo);
        }
        combineTiles();
        this.album.rebuild();
        refresh();
        sync();
    }

    void swap() {
        if (isSaving() || this.selection.isEmpty()) {
            return;
        }
        this.selection.swap();
        refresh();
        sync();
    }

    void target(PickSelection.Side side) {
        this.selection.target(side);
        refresh();
    }

    void onTileClick(Uri photo) {
        if (isSaving()) {
            return;
        }
        this.selection.toggle(photo);
        refresh();
        sync();
    }

    void useCamera(PickSelection.Side side) {
        final Uri photo = (side == PickSelection.Side.BACK) ? this.selection.back() : this.selection.front();
        if (isSaving() || photo == null) {
            return;
        }
        this.selection.toggle(photo);
        refresh();
        sync();
    }

    void openAllPhotos() {
        this.album.dismiss();
        this.activity.startActivity(new Intent(this.activity, GalleryPickerActivity.class));
    }

    private void sync() {
        final Uri back = this.selection.back();
        final Uri front = this.selection.front();
        final Context context = this.activity.getApplicationContext();
        this.pendingWrites++;
        final int previewWidth = this.album.slotWidth();
        WRITER.execute(() -> {
            write(context, back, false);
            write(context, front, true);
            final Bitmap backBitmap = (back != null)
                    ? GalleryPhotos.preview(GalleryPhotos.file(context, false), previewWidth) : null;
            final Bitmap frontBitmap = (front != null)
                    ? GalleryPhotos.preview(GalleryPhotos.file(context, true), previewWidth) : null;
            this.handler.post(() -> {
                this.backPreview = backBitmap;
                this.frontPreview = frontBitmap;
                this.pendingWrites--;
                refresh();
            });
        });
    }

    private void write(Context context, Uri photo, boolean front) {
        final File target = GalleryPhotos.file(context, front);
        final int slot = front ? 1 : 0;
        if (photo == null) {
            target.delete();
            this.stored[slot] = null;
            return;
        }
        if (photo.equals(this.stored[slot]) && target.isFile()) {
            return;
        }
        try {
            GalleryPhotos.store(context, photo, target);
            this.stored[slot] = photo;
        } catch (IOException | RuntimeException ex) {
            Log.e(TAG, "Gallery photo not saved", ex);
            target.delete();
            this.stored[slot] = null;
            this.handler.post(() -> unreadable(photo));
        }
    }

    private void unreadable(Uri photo) {
        if (this.removed) {
            return;
        }
        if (photo.equals(this.selection.back()) || photo.equals(this.selection.front())) {
            this.selection.toggle(photo);
            sync();
        }
        Toast.makeText(this.activity, "Couldn't read this photo", Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void refresh() {
        if (this.selection.isEmpty()) {
            this.badge.setVisibility(View.GONE);
        } else {
            this.badge.setText(this.selection.isComplete() ? "✓" : "1");
            this.badge.setTextColor(Color.BLACK);
            this.badge.setBackground(pill(Color.WHITE, BADGE_DP / 2f));
            this.badge.setVisibility(View.VISIBLE);
        }
        this.album.refresh();
    }

    private void topMargin(int statusBarInset) {
        final ViewGroup.LayoutParams params = this.button.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).topMargin = titleRowTop(statusBarInset);
            this.button.setLayoutParams(params);
        }

    }

    private int titleRowTop(int statusBarInset) {
        return statusBarInset + dp(TITLE_ROW_CENTER_DP) - dp(BUTTON_DP) / 2;
    }

    private int statusBarHeight() {
        final Resources resources = this.activity.getResources();
        final int id = resources.getIdentifier("status_bar_height", "dimen", "android");
        return (id != 0) ? resources.getDimensionPixelSize(id) : dp(24);
    }

    private Drawable circleRipple() {
        final GradientDrawable fill = new GradientDrawable();
        fill.setShape(GradientDrawable.OVAL);
        fill.setColor(FILL);
        final GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(RIPPLE), fill, mask);
    }

    private GradientDrawable pill(int color, float radiusDp) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(Dimensions.dpExact(this.activity, radiusDp));
        return drawable;
    }

    private View icon() {
        final ImageView view = new ImageView(this.activity);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setImageDrawable(hostIcon());
        return view;
    }

    private Drawable hostIcon() {
        final Drawable icon = SettingsStyle.hostIcon(this.activity, HOST_ICON);
        return (icon != null) ? icon : new PhotoGlyph(Dimensions.dpExact(this.activity, 2));
    }

    private int dp(float value) {
        return Dimensions.dp(this.activity, value);
    }

    private static final class PhotoGlyph extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Path ridge = new Path();

        private final RectF frame = new RectF();

        private PhotoGlyph(float stroke) {
            this.paint.setColor(Color.WHITE);
            this.paint.setStrokeWidth(stroke);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(Canvas canvas) {
            final float left = getBounds().left;
            final float top = getBounds().top;
            final float width = getBounds().width();
            final float height = getBounds().height();
            final float inset = this.paint.getStrokeWidth() / 2;
            this.frame.set(left + inset, top + height * 0.15f, left + width - inset, top + height * 0.85f);
            this.paint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(this.frame, width * 0.14f, width * 0.14f, this.paint);
            this.ridge.reset();
            this.ridge.moveTo(this.frame.left + width * 0.08f, this.frame.bottom - height * 0.08f);
            this.ridge.lineTo(left + width * 0.38f, top + height * 0.52f);
            this.ridge.lineTo(left + width * 0.55f, top + height * 0.68f);
            this.ridge.lineTo(left + width * 0.68f, top + height * 0.58f);
            this.ridge.lineTo(this.frame.right - width * 0.08f, this.frame.bottom - height * 0.08f);
            canvas.drawPath(this.ridge, this.paint);
            this.paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(left + width * 0.7f, top + height * 0.36f, width * 0.07f, this.paint);
        }

        @Override
        public void setAlpha(int alpha) {
            this.paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            this.paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

    }

}
