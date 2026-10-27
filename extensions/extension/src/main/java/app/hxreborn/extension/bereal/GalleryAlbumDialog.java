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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
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

    private static final int LIVE_DOT = 0xFFFF3B30;

    private static final int DISABLED_ICON = 0xFF636366;

    private static final int CORNER_DP = 20;

    private static final int TILE_RADIUS_DP = 8;

    private static final int SIDE_DP = 12;

    private static final int GAP_DP = 4;

    private static final int HANDLE_ZONE_DP = 24;

    private static final int HEADER_DP = 48;

    private static final int DONE_DP = 36;

    private static final int SLOT_MAX_DP = 128;

    private static final int SLOT_GAP_DP = 18;

    private static final int SWAP_DP = 40;

    private static final int PILL_DP = 22;

    private static final int REMOVE_DP = 24;

    private static final int WIDE_SCREEN_DP = 600;

    private static final float MAX_HEIGHT_FRACTION = 0.85f;

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

    private Slot backSlot;

    private Slot frontSlot;

    private TextView swap;

    private TextView hint;

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
        final PickSelection.Side target = selection.isComplete() ? null : selection.target();
        styleSlot(this.backSlot, selection.back() != null, target == PickSelection.Side.BACK);
        styleSlot(this.frontSlot, selection.front() != null, target == PickSelection.Side.FRONT);
        final boolean canSwap = !selection.isEmpty() && !this.owner.isSaving();
        this.swap.setEnabled(canSwap);
        this.swap.setBackground(circle(canSwap ? Color.WHITE : TILE_FILL));
        this.swap.setTextColor(canSwap ? Color.BLACK : DISABLED_ICON);
        this.hint.setText(hintText(selection));
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
        this.sheet.addView(slots(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        this.hint = label(13, Typeface.NORMAL, GREY);
        this.hint.setGravity(Gravity.CENTER);
        this.hint.setPadding(dp(SIDE_DP), dp(8), dp(SIDE_DP), dp(12));
        this.sheet.addView(this.hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        this.body = new LinearLayout(this.activity);
        this.body.setOrientation(LinearLayout.VERTICAL);
        this.body.setPadding(dp(SIDE_DP), 0, dp(SIDE_DP), 0);
        final int screenHeight = this.activity.getResources().getDisplayMetrics().heightPixels;
        final int chrome = dp(HANDLE_ZONE_DP) + dp(HEADER_DP) + slotHeight() + dp(80);
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
        final TextView title = label(17, Typeface.BOLD, Color.WHITE);
        title.setText("Your BeReal");
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        final TextView done = label(15, Typeface.BOLD, Color.BLACK);
        done.setText("Done");
        done.setGravity(Gravity.CENTER);
        done.setPadding(dp(18), 0, dp(18), 0);
        done.setBackground(pill(Color.WHITE, DONE_DP / 2f));
        done.setOnClickListener((view) -> dismiss());
        header.addView(done, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(DONE_DP)));
        return header;
    }

    private View slots() {
        final LinearLayout row = new LinearLayout(this.activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        this.backSlot = slot(PickSelection.Side.BACK, "Back");
        this.frontSlot = slot(PickSelection.Side.FRONT, "Front");
        this.swap = label(20, Typeface.BOLD, Color.BLACK);
        this.swap.setText("⇄");
        this.swap.setGravity(Gravity.CENTER);
        this.swap.setContentDescription("Swap back and front");
        this.swap.setOnClickListener((view) -> this.owner.swap());
        final LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams(dp(SWAP_DP), dp(SWAP_DP));
        swapParams.setMargins(dp(SLOT_GAP_DP), 0, dp(SLOT_GAP_DP), dp(PILL_DP));
        row.addView(this.backSlot.column);
        row.addView(this.swap, swapParams);
        row.addView(this.frontSlot.column);
        return row;
    }

    private Slot slot(PickSelection.Side side, String name) {
        final Slot slot = new Slot(side, name);
        slot.column = new LinearLayout(this.activity);
        slot.column.setOrientation(LinearLayout.VERTICAL);
        slot.column.setGravity(Gravity.CENTER_HORIZONTAL);

        slot.frame = roundedTile(TILE_RADIUS_DP + 4);
        slot.frame.setOnClickListener((view) -> this.owner.target(side));
        slot.image = new ImageView(this.activity);
        slot.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        slot.frame.addView(slot.image,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        slot.cameraLabel = label(13, Typeface.BOLD, GREY);
        slot.cameraLabel.setText("Camera");
        slot.cameraLabel.setGravity(Gravity.CENTER);
        slot.frame.addView(slot.cameraLabel,
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        slot.sourceBadge = new LinearLayout(this.activity);
        slot.sourceBadge.setOrientation(LinearLayout.HORIZONTAL);
        slot.sourceBadge.setGravity(Gravity.CENTER_VERTICAL);
        slot.sourceBadge.setPadding(dp(8), 0, dp(8), 0);
        slot.liveDot = new View(this.activity);
        slot.liveDot.setBackground(circle(LIVE_DOT));
        final LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(7), dp(7));
        dotParams.setMarginEnd(dp(5));
        slot.sourceBadge.addView(slot.liveDot, dotParams);
        slot.sourceText = label(11, Typeface.BOLD, Color.WHITE);
        slot.sourceBadge.addView(slot.sourceText);
        final FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(PILL_DP), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        badgeParams.bottomMargin = dp(10);
        slot.frame.addView(slot.sourceBadge, badgeParams);

        slot.removeButton = label(13, Typeface.BOLD, Color.WHITE);
        slot.removeButton.setText("✕");
        slot.removeButton.setGravity(Gravity.CENTER);
        slot.removeButton.setBackground(circle(Color.BLACK));
        slot.removeButton.setContentDescription("Use the camera for the " + name.toLowerCase(Locale.ROOT));
        slot.removeButton.setOnClickListener((view) -> this.owner.useCamera(side));
        final FrameLayout.LayoutParams removeButtonParams = new FrameLayout.LayoutParams(dp(REMOVE_DP), dp(REMOVE_DP),
                Gravity.TOP | Gravity.END);
        removeButtonParams.setMargins(0, dp(6), dp(6), 0);
        slot.frame.addView(slot.removeButton, removeButtonParams);

        slot.column.addView(slot.frame, new LinearLayout.LayoutParams(slotWidth(), slotHeight()));
        final TextView caption = label(13, Typeface.BOLD, Color.WHITE);
        caption.setText(name);
        caption.setGravity(Gravity.CENTER);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        slot.column.addView(caption, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(PILL_DP)));
        return slot;
    }

    private void styleSlot(Slot slot, boolean filled, boolean targeted) {
        final Bitmap preview = filled ? this.owner.preview(slot.side) : null;
        slot.image.setImageBitmap(preview);
        slot.cameraLabel.setVisibility(filled ? View.GONE : View.VISIBLE);
        slot.removeButton.setVisibility(filled ? View.VISIBLE : View.GONE);
        slot.removeButton.setEnabled(!this.owner.isSaving());
        slot.liveDot.setVisibility(filled ? View.GONE : View.VISIBLE);
        slot.sourceText.setText(filled ? "GALLERY" : "LIVE");
        slot.sourceText.setTextColor(filled ? Color.BLACK : Color.WHITE);
        slot.sourceBadge.setBackground(pill(filled ? Color.WHITE : LABEL_SCRIM, PILL_DP / 2f));
        slot.frame.setForeground(slotBorder(filled, targeted));
        slot.frame.setSelected(targeted);
        slot.frame.setContentDescription(
                slot.name + " photo, " + (filled ? "from gallery" : "live camera") + (targeted ? ", selected" : ""));
    }

    private static String hintText(PickSelection<Uri> selection) {
        if (selection.isComplete()) {
            return "Tap Done, then the shutter.";
        }
        final String side = (selection.target() == PickSelection.Side.BACK) ? "back" : "front";
        return "Tap a photo for the " + side + ". LIVE sides shoot with the camera.";
    }

    int slotWidth() {
        final int screen = this.activity.getResources().getDisplayMetrics().widthPixels;
        final int fit = (screen - 2 * dp(SIDE_DP) - dp(SWAP_DP) - 2 * dp(SLOT_GAP_DP)) / 2;
        return Math.min(dp(SLOT_MAX_DP), fit);
    }

    private int slotHeight() {
        return slotWidth() * 4 / 3;
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
        return roundedTile(TILE_RADIUS_DP);
    }

    private FrameLayout roundedTile(int radiusDp) {
        final FrameLayout tile = new FrameLayout(this.activity);
        tile.setBackgroundColor(TILE_FILL);
        final float radius = Dimensions.dpExact(this.activity, radiusDp);
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

    private GradientDrawable slotBorder(boolean filled, boolean targeted) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(Dimensions.dpExact(this.activity, TILE_RADIUS_DP + 4));
        if (targeted) {
            drawable.setStroke(dp(3), Color.WHITE);
        } else if (!filled) {
            drawable.setStroke(dp(1.5f), GREY, Dimensions.dpExact(this.activity, 6),
                    Dimensions.dpExact(this.activity, 4));
        }
        return drawable;
    }

    private GradientDrawable circle(int color) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable pill(int color, float radiusDp) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(Dimensions.dpExact(this.activity, radiusDp));
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
                return GalleryPhotos.preview(new File(photo.getPath()), side * 2);
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

    private static final class Slot {

        private final PickSelection.Side side;

        private final String name;

        private LinearLayout column;

        private FrameLayout frame;

        private ImageView image;

        private TextView cameraLabel;

        private LinearLayout sourceBadge;

        private View liveDot;

        private TextView sourceText;

        private TextView removeButton;

        private Slot(PickSelection.Side side, String name) {
            this.side = side;
            this.name = name;
        }

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
