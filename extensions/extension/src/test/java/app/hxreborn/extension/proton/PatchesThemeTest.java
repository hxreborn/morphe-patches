/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.util.TypedValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchesThemeTest {

    private static final ContextWrapper NO_THEME = new ContextWrapper(null);

    private static Context application() {
        return RuntimeEnvironment.getApplication();
    }

    private static int themeColor(Context context, int attribute) {
        final TypedArray attributes = context.obtainStyledAttributes(new int[] { attribute });
        try {
            return attributes.getColor(0, 0);
        } finally {
            attributes.recycle();
        }
    }

    @Test
    public void grayBelowHalfBrightnessIsDark() {
        assertTrue(PatchesTheme.isDark(0xFF000000));
        assertTrue(PatchesTheme.isDark(0xFF7F7F7F));
        assertFalse(PatchesTheme.isDark(0xFF808080));
        assertFalse(PatchesTheme.isDark(0xFFFFFFFF));
    }

    @Test
    public void greenWeighsMoreThanTheOtherChannels() {
        assertTrue(PatchesTheme.isDark(0xFFFF0000));
        assertTrue(PatchesTheme.isDark(0xFF0000FF));
        assertFalse(PatchesTheme.isDark(0xFF00FF00));
    }

    @Test
    public void brightnessTruncatesBeforeComparing() {
        assertTrue(PatchesTheme.isDark(0xFF00DA00));
        assertFalse(PatchesTheme.isDark(0xFF00DB00));
    }

    @Test
    public void alphaDoesNotAffectDarkness() {
        assertTrue(PatchesTheme.isDark(0xFF101010));
        assertTrue(PatchesTheme.isDark(0x00101010));
        assertFalse(PatchesTheme.isDark(0x00FFFFFF));
    }

    @Test
    public void protonDarkSurfacesAreDark() {
        assertTrue(PatchesTheme.isDark(0xFF191927));
        assertTrue(PatchesTheme.isDark(0xFF2B2B38));
        assertFalse(PatchesTheme.isDark(0xFFEDEDEE));
    }

    @Test
    public void halfAlphaReplacesTheAlphaChannel() {
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0xFF123456));
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0x00123456));
        assertEquals(0x80123456, PatchesTheme.withHalfAlpha(0x80123456));
        assertEquals(0x80000000, PatchesTheme.withHalfAlpha(0));
        assertEquals(0x80FFFFFF, PatchesTheme.withHalfAlpha(0xFFFFFFFF));
    }

    @Test
    public void halfAlphaIsIdempotent() {
        // given
        final int once = PatchesTheme.withHalfAlpha(0xFF9292F9);

        // when
        final int twice = PatchesTheme.withHalfAlpha(once);

        // then
        assertEquals(once, twice);
    }

    @Test
    public void unresolvableAttributesFallBackToProtonDarkColors() {
        for (Context context : new Context[] { NO_THEME, application() }) {
            assertEquals(0xFF1C1B24, PatchesTheme.resolveColorAttribute(context, PatchesTheme.BACKGROUND_NORM));
            assertEquals(0xFF292733, PatchesTheme.resolveColorAttribute(context, PatchesTheme.BACKGROUND_SECONDARY));
            assertEquals(0xFFEDEDEE, PatchesTheme.resolveColorAttribute(context, PatchesTheme.TEXT_NORM));
            assertEquals(0xFFA9A9AF, PatchesTheme.resolveColorAttribute(context, PatchesTheme.TEXT_WEAK));
            assertEquals(0xFF5B5966, PatchesTheme.resolveColorAttribute(context, PatchesTheme.ICON_DISABLED));
        }
    }

    @Test
    public void unknownAttributeFallsBackToSecondaryBackground() {
        for (Context context : new Context[] { NO_THEME, application() }) {
            assertEquals(PatchesTheme.resolveColorAttribute(context, PatchesTheme.BACKGROUND_SECONDARY),
                    PatchesTheme.resolveColorAttribute(context, "proton_something_else"));
        }
    }

    @Test
    public void fallbackColorsAreOpaque() {
        for (String attribute : new String[] { PatchesTheme.BACKGROUND_NORM, PatchesTheme.BACKGROUND_SECONDARY,
                PatchesTheme.TEXT_NORM, PatchesTheme.TEXT_WEAK, PatchesTheme.ICON_DISABLED }) {
            assertEquals(attribute, 0xFF, PatchesTheme.resolveColorAttribute(NO_THEME, attribute) >>> 24);
        }
    }

    @Test
    public void colorAttributeOfTheThemeIsResolved() {
        // given
        final Context context = application();
        final int expected = themeColor(context, android.R.attr.colorAccent);

        // when
        final int resolved = PatchesTheme.resolveColorAttribute(context, "android:attr/colorAccent");

        // then
        assertNotEquals(0xFF292733, expected);
        assertEquals(expected, resolved);
    }

    @Test
    public void colorResourceAttributeOfTheThemeIsResolved() {
        // given
        final Context context = application();
        final TypedValue value = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, value, true));
        assertTrue(value.resourceId != 0);
        assertNotEquals(0xFF292733, context.getColor(value.resourceId));

        // when
        final int resolved = PatchesTheme.resolveColorAttribute(context, "android:attr/textColorPrimary");

        // then
        assertEquals(context.getColor(value.resourceId), resolved);
    }

}
