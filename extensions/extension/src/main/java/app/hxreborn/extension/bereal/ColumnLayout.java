/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
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
