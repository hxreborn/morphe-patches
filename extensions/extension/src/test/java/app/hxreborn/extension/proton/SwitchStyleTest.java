/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SwitchStyleTest {

    private static final int BLACK = 0xFF000000;

    private static final int WHITE = 0xFFFFFFFF;

    private static int gray(int level) {
        return 0xFF000000 | (level << 16) | (level << 8) | level;
    }

    @Test
    public void whiteBackgroundGetsBlackContent() {
        assertEquals(BLACK, SwitchStyle.contentColorOn(WHITE));
    }

    @Test
    public void blackBackgroundGetsWhiteContent() {
        assertEquals(WHITE, SwitchStyle.contentColorOn(BLACK));
    }

    @Test
    public void protonStockAccentsGetTheirReadableContent() {
        assertEquals(BLACK, SwitchStyle.contentColorOn(AccentColor.STOCK_DARK_ACCENT));
        assertEquals(WHITE, SwitchStyle.contentColorOn(AccentColor.STOCK_LIGHT_ACCENT));
    }

    @Test
    public void contentFlipsFromWhiteToBlackAtGrayLevel118() {
        assertEquals(WHITE, SwitchStyle.contentColorOn(gray(116)));
        assertEquals(WHITE, SwitchStyle.contentColorOn(gray(117)));
        assertEquals(BLACK, SwitchStyle.contentColorOn(gray(118)));
        assertEquals(BLACK, SwitchStyle.contentColorOn(gray(119)));
    }

    @Test
    public void greenOutweighsTheOtherChannels() {
        assertEquals(BLACK, SwitchStyle.contentColorOn(0xFF00FF00));
        assertEquals(BLACK, SwitchStyle.contentColorOn(0xFFFF0000));
        assertEquals(WHITE, SwitchStyle.contentColorOn(0xFF0000FF));
    }

    @Test
    public void alphaDoesNotChangeTheContentColor() {
        assertEquals(BLACK, SwitchStyle.contentColorOn(0x00FFFFFF));
        assertEquals(WHITE, SwitchStyle.contentColorOn(0x00000000));
        assertEquals(BLACK, SwitchStyle.contentColorOn(0x80FFFFFF));
    }

    @Test
    public void contentColorIsOpaqueBlackOrWhiteAtEveryGrayLevel() {
        for (int level = 0; level <= 255; level++) {
            final int content = SwitchStyle.contentColorOn(gray(level));
            assertTrue("level " + level, content == BLACK || content == WHITE);
        }
    }

}
