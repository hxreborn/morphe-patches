/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.readera;

import android.graphics.ColorFilter;

public interface HighlightColorHost {

    int getHighlightColor();

    void setHighlightColor(int color);

    ColorFilter getHighlightColorFilter(int color);

}
