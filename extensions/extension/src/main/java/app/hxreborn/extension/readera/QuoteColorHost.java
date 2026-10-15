/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.readera;

import android.graphics.ColorFilter;

public interface QuoteColorHost {

    int getQuoteColor();

    void setQuoteColor(int color);

    ColorFilter getQuoteColorFilter(int color);

}
