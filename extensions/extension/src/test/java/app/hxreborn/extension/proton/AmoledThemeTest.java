/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class AmoledThemeTest {

    private static final long PACKED_BLACK = 0xFF000000L << 32;

    private static final long PACKED_SURFACE = 0xFF2B2B38L << 32;

    private static final long PACKED_DIMMED_SURFACE = 0xFF15151CL << 32;

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
    }

    private static long packed(int argb) {
        return (argb & 0xFFFFFFFFL) << 32;
    }

    private static void assertDark(int argb) {
        assertEquals(Integer.toHexString(argb), PACKED_BLACK, AmoledTheme.transformPackedBackground(packed(argb)));
        assertEquals(Integer.toHexString(argb), PACKED_SURFACE, AmoledTheme.transformPackedSurface(packed(argb)));
    }

    private static void assertNotDark(int argb) {
        assertEquals(Integer.toHexString(argb), packed(argb), AmoledTheme.transformPackedBackground(packed(argb)));
        assertEquals(Integer.toHexString(argb), packed(argb), AmoledTheme.transformPackedSurface(packed(argb)));
    }

    @Test
    public void unpatchedBuildLeavesColorsAlone() {
        PatchedBuild.setAmoledEnabled(true);

        assertFalse(AmoledTheme.isPatched());
        assertFalse(AmoledTheme.isEnabled());
        for (long color : new long[] { 0L, 0xFF191927L, 0xFFFFFFFFL, -1L, Long.MIN_VALUE }) {
            assertEquals(color, AmoledTheme.transformBackground(color));
        }
        for (long color : new long[] { 0L, packed(0xFF191927), packed(0xFF2B2B38), packed(0xFFFFFFFF), -1L }) {
            assertEquals(color, AmoledTheme.transformPackedBackground(color));
            assertEquals(color, AmoledTheme.transformPackedSurface(color));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildIsEnabledUntilTurnedOff() {
        assertTrue(AmoledTheme.isPatched());
        assertTrue(AmoledTheme.isEnabled());

        PatchedBuild.setAmoledEnabled(false);
        assertFalse(AmoledTheme.isEnabled());

        PatchedBuild.setAmoledEnabled(true);
        assertTrue(AmoledTheme.isEnabled());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void settingIsStoredInThePatchPreferences() {
        // when
        PatchedBuild.setAmoledEnabled(false);

        // then
        assertFalse(this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean("amoled_dark_theme", true));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void enabledThemeTurnsBackgroundsBlack() {
        for (long color : new long[] { 0L, 0xFF191927L, 0xFFFFFFFFL, -1L, Long.MIN_VALUE }) {
            assertEquals(0xFF000000L, AmoledTheme.transformBackground(color));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void disabledThemeLeavesColorsAlone() {
        PatchedBuild.setAmoledEnabled(false);

        for (long color : new long[] { 0L, 0xFF191927L, 0xFFFFFFFFL, -1L, Long.MIN_VALUE }) {
            assertEquals(color, AmoledTheme.transformBackground(color));
        }
        for (long color : new long[] { 0L, packed(0xFF191927), packed(0xFF2B2B38), packed(0xFFFFFFFF), -1L }) {
            assertEquals(color, AmoledTheme.transformPackedBackground(color));
            assertEquals(color, AmoledTheme.transformPackedSurface(color));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void channelSumBelow384IsDark() {
        assertDark(0xFF000000);
        assertDark(0xFF191927);
        assertDark(0xFF2B2B38);
        assertDark(0xFF7F8080);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void channelSumOf384OrMoreIsNotDark() {
        assertNotDark(0xFF808080);
        assertNotDark(0xFF9292F9);
        assertNotDark(0xFFFFFFFF);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void oneBrightChannelCanStayBelowTheThreshold() {
        assertDark(0xFF0000FF);
        assertDark(0xFFFF8000);
        assertNotDark(0xFFFF8100);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void everyChannelCountsEqually() {
        assertDark(0xFF7F8080);
        assertDark(0xFF807F80);
        assertDark(0xFF80807F);
        assertNotDark(0xFF808080);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void alphaDoesNotAffectDarkness() {
        assertDark(0x00000000);
        assertDark(0x80191927);
        assertNotDark(0x00FFFFFF);
        assertNotDark(0x00808080);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void colorSpaceBitsBelowTheArgbWordAreIgnored() {
        assertEquals(PACKED_BLACK, AmoledTheme.transformPackedBackground(packed(0xFF191927) | 0xFFFFFFFFL));
        assertEquals(PACKED_SURFACE, AmoledTheme.transformPackedSurface(packed(0xFF191927) | 0xFFFFFFFFL));
        assertEquals(packed(0xFFFFFFFF) | 0x7FL, AmoledTheme.transformPackedBackground(packed(0xFFFFFFFF) | 0x7FL));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void dimRaisedSurfacesIsOffByDefault() {
        assertTrue(AmoledTheme.isEnabled());
        assertFalse(AmoledTheme.isDimRaisedSurfacesEnabled());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void undimmedRaisedSurfaceKeepsItsColor() {
        assertEquals(packed(0xFF292733), AmoledTheme.transformPackedRaisedSurface(packed(0xFF292733)));
        assertEquals(packed(0xFF2B2B38), AmoledTheme.transformPackedRaisedSurface(packed(0xFF2B2B38)));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void dimmedSurfaceUsesTheDarkerColor() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);

        assertEquals(PACKED_DIMMED_SURFACE, AmoledTheme.transformPackedSurface(packed(0xFF191927)));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void dimSettingHasNoEffectWhileTheThemeIsOff() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);
        PatchedBuild.setAmoledEnabled(false);

        assertFalse(AmoledTheme.isDimRaisedSurfacesEnabled());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void dimSettingIsStoredInThePatchPreferences() {
        // when
        PatchedBuild.setDimRaisedSurfacesEnabled(true);

        // then
        assertTrue(this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean("amoled_dim_raised_surfaces", false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void raisedSurfaceHalvesEachChannel() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);

        assertEquals(packed(0xFF141319), AmoledTheme.transformPackedRaisedSurface(packed(0xFF292733)));
        assertEquals(packed(0xFF15151C), AmoledTheme.transformPackedRaisedSurface(packed(0xFF2B2B38)));
        assertEquals(packed(0xFF0F0F18), AmoledTheme.transformPackedRaisedSurface(packed(0xFF1F1F31)));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void raisedSurfaceKeepsAlphaAndColorSpaceBits() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);

        assertEquals(packed(0x80141319) | 0x7FL, AmoledTheme.transformPackedRaisedSurface(packed(0x80292733) | 0x7FL));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void raisedSurfaceLeavesLightColorsAlone() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);

        assertEquals(packed(0xFFF5F4F2), AmoledTheme.transformPackedRaisedSurface(packed(0xFFF5F4F2)));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void disabledThemeLeavesRaisedSurfacesAlone() {
        PatchedBuild.setDimRaisedSurfacesEnabled(true);
        PatchedBuild.setAmoledEnabled(false);

        assertEquals(packed(0xFF292733), AmoledTheme.transformPackedRaisedSurface(packed(0xFF292733)));
    }

}
