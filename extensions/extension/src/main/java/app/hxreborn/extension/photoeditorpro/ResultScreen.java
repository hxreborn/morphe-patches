/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressWarnings("unused")
public final class ResultScreen {

    public static void hideShareRowAndCenterPreview(View shareRow) {
        if (!PatchSettings.HIDE_SHARE.get()
                || !(shareRow.getParent() instanceof ViewGroup screen)) {
            return;
        }
        shareRow.setVisibility(View.GONE);
        int labelIndex = screen.indexOfChild(shareRow) - 1;
        View label = screen.getChildAt(labelIndex);
        if (!(label instanceof TextView)) {
            return;
        }
        label.setVisibility(View.GONE);
        View previewGroup = screen.getChildAt(labelIndex - 1);
        if (previewGroup != null
                && previewGroup.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params) {
            centerVertically(screen, previewGroup, params.topMargin);
        }
    }

    private static void centerVertically(ViewGroup screen, View previewGroup, int minTopMargin) {
        screen.addOnLayoutChangeListener((view, left, top, right, bottom,
                                          oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) previewGroup.getLayoutParams();
            int headerBottom = previewGroup.getTop() - params.topMargin;
            int centeredTopMargin =
                    (screen.getHeight() - previewGroup.getHeight()) / 2 - headerBottom;
            int topMargin = Math.max(minTopMargin, centeredTopMargin);
            if (topMargin != params.topMargin) {
                screen.post(() -> {
                    params.topMargin = topMargin;
                    previewGroup.setLayoutParams(params);
                });
            }
        });
    }

    private ResultScreen() {
    }
}
