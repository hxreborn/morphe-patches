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
package app.hxreborn.extension.aliexpress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class ViewerSaveButton {

    private static final String LABEL = "Save";

    private static final String SAVED_PREFIX = "Saved to ";

    private static final String SAVE_FAILED = "Save failed!";

    private static final int STORAGE_PERMISSION_REQUEST = 0x6878;

    private static final int[] SHARE_ANCHOR_RULES = { RelativeLayout.LEFT_OF, RelativeLayout.START_OF };

    private final View button;

    private final View viewerRoot;

    private ViewerSaveButton(View button, View viewerRoot) {
        this.button = button;
        this.viewerRoot = viewerRoot;
    }

    public static void attach(View share) {
        if (!PatchSettings.enableImageSaving() || !(share.getParent() instanceof RelativeLayout)) {
            return;
        }
        RelativeLayout bar = (RelativeLayout) share.getParent();
        View button = createButton(share);
        if (button == null) {
            return;
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(share.getLayoutParams());
        for (int rule : SHARE_ANCHOR_RULES) {
            params.addRule(rule, share.getId());
        }
        replaceAnchor(bar, share.getId(), button.getId());
        bar.addView(button, params);
        ViewerSaveButton saveButton = new ViewerSaveButton(button, bar.getRootView());
        button.setOnClickListener((view) -> saveButton.saveCurrentImage());
    }

    static int saveIconId() {
        return 0;
    }

    static String imageUrlOf(View view) {
        return null;
    }

    static File downloadImage(String url) throws IOException {
        return null;
    }

    private static View createButton(View share) {
        ImageView shareIcon = iconOf(share);
        if (shareIcon == null) {
            Log.w(PatchSettings.LOG_TAG, "Viewer share button has no icon");
            return null;
        }
        Context context = share.getContext();
        ImageView icon = new ImageView(context);
        icon.setImageResource(saveIconId());
        icon.setImageTintList(shareIcon.getImageTintList());
        icon.setScaleType(shareIcon.getScaleType());
        RelativeLayout button = new RelativeLayout(context);
        button.setId(View.generateViewId());
        button.setPaddingRelative(share.getPaddingStart(), share.getPaddingTop(), share.getPaddingEnd(),
                share.getPaddingBottom());
        button.setContentDescription(LABEL);
        button.addView(icon, new RelativeLayout.LayoutParams(shareIcon.getLayoutParams()));
        return button;
    }

    private static ImageView iconOf(View share) {
        if (!(share instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) share;
        for (int index = 0; index < group.getChildCount(); index++) {
            View child = group.getChildAt(index);
            if (child instanceof ImageView) {
                return (ImageView) child;
            }
        }
        return null;
    }

    private static void replaceAnchor(RelativeLayout bar, int oldAnchorId, int newAnchorId) {
        for (int index = 0; index < bar.getChildCount(); index++) {
            ViewGroup.LayoutParams layoutParams = bar.getChildAt(index).getLayoutParams();
            if (!(layoutParams instanceof RelativeLayout.LayoutParams)) {
                continue;
            }
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layoutParams;
            if (anchorsTo(params, oldAnchorId)) {
                for (int rule : SHARE_ANCHOR_RULES) {
                    params.addRule(rule, newAnchorId);
                }
            }
        }
    }

    private static boolean anchorsTo(RelativeLayout.LayoutParams params, int anchorId) {
        for (int rule : SHARE_ANCHOR_RULES) {
            if (params.getRule(rule) == anchorId) {
                return true;
            }
        }
        return false;
    }

    private void saveCurrentImage() {
        String url = visibleImageUrl(this.viewerRoot);
        if (url == null) {
            showResult(null);
            return;
        }
        if (requestMissingStoragePermission()) {
            return;
        }
        this.button.setEnabled(false);
        Handler mainThread = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            File saved = downloadOrNull(url);
            mainThread.post(() -> {
                this.button.setEnabled(true);
                showResult(saved);
            });
        }).start();
    }

    private boolean requestMissingStoragePermission() {
        Context context = this.button.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || context
            .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (context instanceof Activity) {
            ((Activity) context).requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_REQUEST);
        }
        return true;
    }

    private static File downloadOrNull(String url) {
        try {
            return downloadImage(url);
        } catch (IOException ex) {
            Log.w(PatchSettings.LOG_TAG, "Viewer image download failed", ex);
            return null;
        }
    }

    private void showResult(File saved) {
        String message = (saved != null) ? SAVED_PREFIX + saved.getParent() : SAVE_FAILED;
        Toast.makeText(this.button.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    static String visibleImageUrl(View root) {
        Rect visible = new Rect();
        String bestUrl = null;
        long bestArea = 0;
        ArrayDeque<View> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            View view = pending.poll();
            String url = imageUrlOf(view);
            if (url != null && view.getGlobalVisibleRect(visible)) {
                long area = (long) visible.width() * visible.height();
                if (area > bestArea) {
                    bestArea = area;
                    bestUrl = url;
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int index = 0; index < group.getChildCount(); index++) {
                    pending.add(group.getChildAt(index));
                }
            }
        }
        return bestUrl;
    }

}
