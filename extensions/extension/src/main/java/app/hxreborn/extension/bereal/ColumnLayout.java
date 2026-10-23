/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import android.content.Context;
import android.widget.LinearLayout;

final class ColumnLayout extends LinearLayout {

    private final int maxWidth;

    ColumnLayout(Context context, int maxWidth) {
        super(context);
        this.maxWidth = maxWidth;
        setOrientation(VERTICAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        final boolean tooWide = mode != MeasureSpec.UNSPECIFIED
                && MeasureSpec.getSize(widthMeasureSpec) > this.maxWidth;
        super.onMeasure(tooWide ? MeasureSpec.makeMeasureSpec(this.maxWidth, mode) : widthMeasureSpec,
                heightMeasureSpec);
    }

}
