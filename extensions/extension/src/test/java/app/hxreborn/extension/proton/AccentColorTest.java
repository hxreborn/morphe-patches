/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorSpace;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class AccentColorTest {

    private static final String ORANGE = "#E8710A";

    private static final long ARGB_MASK = 0xFFFFFFFFL;

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
        PatchedBuild.setAccentPreset(AccentColor.STOCK);
    }

    private static float[] lab(int argb) {
        return Color.valueOf(argb).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB)).getComponents();
    }

    private static double chroma(int argb) {
        final float[] lab = lab(argb);
        return Math.hypot(lab[1], lab[2]);
    }

    private static double hueDegrees(int argb) {
        final float[] lab = lab(argb);
        return Math.toDegrees(Math.atan2(lab[2], lab[1]));
    }

    private static double hueDistance(int first, int second) {
        final double distance = Math.abs(hueDegrees(first) - hueDegrees(second)) % 360;
        return Math.min(distance, 360 - distance);
    }

    private static double lightnessRatio(int picked) {
        return Math.max(0.6, Math.min(1.4, lab(picked)[0] / lab(AccentColor.STOCK_DARK_ACCENT)[0]));
    }

    private static void assertChannelsNear(int expected, int actual, int tolerance) {
        for (int shift = 0; shift <= 24; shift += 8) {
            final int difference = Math.abs(((expected >>> shift) & 0xFF) - ((actual >>> shift) & 0xFF));
            assertTrue(Integer.toHexString(expected) + " vs " + Integer.toHexString(actual) + " at " + shift,
                    difference <= tolerance);
        }
    }

    private static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    private static int argb(String preset) {
        return AccentColor.resolvePresetColor(preset);
    }

    @Test
    public void unpatchedBuildKeepsTheStockAccents() {
        assertFalse(AccentColor.isPatched());
        assertEquals(0xFF9292F9, AccentColor.getAccentColor(true));
        assertEquals(0xFF6D4AFF, AccentColor.getAccentColor(false));
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.transformedStockDarkAccent());
        assertFalse(AccentColor.hasCustomAccent());
    }

    @Test
    public void unpatchedBuildIgnoresAStoredPreset() {
        // when
        AccentColor.setPreset(ORANGE);

        // then
        assertEquals(AccentColor.STOCK, AccentColor.getPreset());
        assertFalse(AccentColor.hasCustomAccent());
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.getAccentColor(true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.getAccentColor(false));
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.transformedStockDarkAccent());
    }

    @Test
    public void brandColorsPassThroughWithoutCustomAccent() {
        AccentColor.setPreset(ORANGE);

        for (long color : new long[] { 0L, 0xFF9292F9L, 0xFF6D4AFFL, 0xFFFFFFFFL, 0x1_FF9292F9L, -1L }) {
            assertEquals(color, AccentColor.transformBrandColor(color));
        }
        assertTrue(AccentColor.transformedBrandColors().isEmpty());
    }

    @Test
    public void packedBrandColorsPassThroughWithoutCustomAccent() {
        AccentColor.setPreset(ORANGE);

        for (long packed : new long[] { 0L, 0xFF9292F9L << 32, (0xFF6D4AFFL << 32) | 0x7FL, -1L, Long.MIN_VALUE }) {
            assertEquals(packed, AccentColor.transformPackedBrandColor(packed));
        }
    }

    @Test
    public void transformedBrandColorsIsAnEmptyDefensiveCopy() {
        final Map<Integer, Integer> first = AccentColor.transformedBrandColors();
        assertTrue(first.isEmpty());
        first.put(1, 2);
        assertTrue(AccentColor.transformedBrandColors().isEmpty());
        assertNotSame(first, AccentColor.transformedBrandColors());
    }

    @Test
    public void emptyOrMissingPresetsResolveToStock() {
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(AccentColor.STOCK, true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(AccentColor.STOCK, false));
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(null, true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(null, false));
    }

    @Test
    public void unparsablePresetsResolveToStock() {
        for (String preset : new String[] { "not a color", "#12", "#GGGGGG", "#00000000", "system2" }) {
            assertEquals(preset, 0, argb(preset));
            assertEquals(preset, AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(preset, true));
            assertEquals(preset, AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(preset, false));
        }
    }

    @Test
    public void hexPresetsAreParsedAsArgb() {
        assertEquals(0xFFFF0000, argb("#FF0000"));
        assertEquals(0x80123456, argb("#80123456"));
    }

    @Test
    public void grayPresetsProduceNoAdjustment() {
        for (String preset : new String[] { "#000000", "#303030", "#808080", "#FFFFFF", "#808081" }) {
            assertEquals(preset, AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(preset, true));
            assertEquals(preset, AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(preset, false));
        }
    }

    @Test
    public void presetsAreAcceptedFromOneLabChromaUnit() {
        int accepted = 0;
        int rejected = 0;
        for (int blue = 0x80; blue <= 0x90; blue++) {
            final String preset = String.format("#8080%02X", blue);
            final boolean custom = AccentColor.resolveAccentColor(preset, true) != AccentColor.STOCK_DARK_ACCENT;
            assertEquals(preset + " chroma " + chroma(argb(preset)), chroma(argb(preset)) >= 1, custom);
            if (custom) {
                accepted++;
            } else {
                rejected++;
            }
        }
        assertTrue(accepted > 0);
        assertTrue(rejected > 0);
    }

    @Test
    public void darkAccentReproducesPresetsInsideTheLightnessRange() {
        for (String preset : new String[] { "#4C8BF5", ORANGE, "#34A853", "#D93025", "#FF0000", "#FF0080", "#2E7D32",
                "#FF00FF" }) {
            final double ratio = lab(argb(preset))[0] / lab(AccentColor.STOCK_DARK_ACCENT)[0];
            assertTrue(preset, ratio >= 0.6 && ratio <= 1.4);

            assertChannelsNear(argb(preset), AccentColor.resolveAccentColor(preset, true), 2);
        }
    }

    @Test
    public void accentsKeepTheHueOfThePreset() {
        for (String preset : new String[] { "#4C8BF5", ORANGE, "#34A853", "#D93025", "#FF0080", "#7B1FA2", "#0000FF",
                "#FFFF00", "#00005F", "#FFE0E0" }) {
            assertTrue(preset, hueDistance(argb(preset), AccentColor.resolveAccentColor(preset, true)) <= 1);
        }
        for (String preset : new String[] { ORANGE, "#34A853", "#D93025", "#FF0080", "#7B1FA2", "#FFFF00",
                "#FFE0E0" }) {
            assertTrue(preset, hueDistance(argb(preset), AccentColor.resolveAccentColor(preset, false)) <= 6);
        }
    }

    @Test
    public void lightnessFollowsThePresetWithinSixtyToOneHundredFortyPercentOfStock() {
        final float stockLightness = lab(AccentColor.STOCK_DARK_ACCENT)[0];
        for (String preset : new String[] { ORANGE, "#2E7D32", "#D93025" }) {
            assertEquals(preset, lab(argb(preset))[0], lab(AccentColor.resolveAccentColor(preset, true))[0], 0.5);
        }
        assertEquals(0.6 * stockLightness, lab(AccentColor.resolveAccentColor("#00005F", true))[0], 1.5);
        assertEquals(0.6 * stockLightness, lab(AccentColor.resolveAccentColor("#000033", true))[0], 1.5);
        assertEquals(1.4 * stockLightness, lab(AccentColor.resolveAccentColor("#FFFF00", true))[0], 2);
        assertEquals(1.4 * stockLightness, lab(AccentColor.resolveAccentColor("#FFE0E0", true))[0], 2);
    }

    @Test
    public void lightAccentScalesItsOwnLightnessByTheSameRatio() {
        final float lightStockLightness = lab(AccentColor.STOCK_LIGHT_ACCENT)[0];
        for (String preset : new String[] { ORANGE, "#34A853", "#D93025", "#2E7D32", "#00005F", "#FFFF00",
                "#FFE0E0" }) {
            assertEquals(preset, lightStockLightness * lightnessRatio(argb(preset)),
                    lab(AccentColor.resolveAccentColor(preset, false))[0], 2);
        }
    }

    @Test
    public void darkAccentGetsThePresetChroma() {
        for (String preset : new String[] { "#80808A", "#808082", "#4C8BF5", ORANGE, "#34A853" }) {
            assertEquals(preset, chroma(argb(preset)), chroma(AccentColor.resolveAccentColor(preset, true)), 1);
        }
    }

    @Test
    public void lightAccentChromaScalesWithThePresetChroma() {
        final double stockChroma = chroma(AccentColor.STOCK_DARK_ACCENT);
        final double lightStockChroma = chroma(AccentColor.STOCK_LIGHT_ACCENT);
        for (String preset : new String[] { "#808082", "#80808A", "#FFE0E0" }) {
            assertEquals(preset, lightStockChroma * chroma(argb(preset)) / stockChroma,
                    chroma(AccentColor.resolveAccentColor(preset, false)), 1);
        }
    }

    @Test
    public void darkAccentChromaStaysWithinThePresetChroma() {
        for (String preset : new String[] { "#0000FF", "#FFFF00", "#00FF00", "#FF00FF", "#00005F" }) {
            assertTrue(preset, chroma(AccentColor.resolveAccentColor(preset, true)) <= chroma(argb(preset)) + 1);
        }
    }

    @Test
    public void systemPresetUsesTheSystemAccentColor() {
        assertTrue(AccentColor.isSystemAccentAvailable());
        final int system = this.context.getColor(android.R.color.system_accent1_500);

        assertNotEquals(0, system);
        assertEquals(system, AccentColor.resolvePresetColor(AccentColor.SYSTEM));
        assertEquals(AccentColor.resolveAccentColor(hex(system), true),
                AccentColor.resolveAccentColor(AccentColor.SYSTEM, true));
        assertEquals(AccentColor.resolveAccentColor(hex(system), false),
                AccentColor.resolveAccentColor(AccentColor.SYSTEM, false));
        assertNotEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(AccentColor.SYSTEM, true));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildKeepsStockWithoutAPreset() {
        assertTrue(AccentColor.isPatched());
        assertEquals(AccentColor.STOCK, AccentColor.getPreset());
        assertFalse(AccentColor.hasCustomAccent());
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.getAccentColor(true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.getAccentColor(false));
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.transformedStockDarkAccent());
        assertEquals(0xFF9292F9L, AccentColor.transformBrandColor(0xFF9292F9L));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildAppliesTheStoredPreset() {
        // when
        AccentColor.setPreset(ORANGE);

        // then
        assertEquals(ORANGE, AccentColor.getPreset());
        assertTrue(AccentColor.hasCustomAccent());
        assertEquals(AccentColor.resolveAccentColor(ORANGE, true), AccentColor.getAccentColor(true));
        assertEquals(AccentColor.resolveAccentColor(ORANGE, false), AccentColor.getAccentColor(false));
        assertEquals(AccentColor.getAccentColor(true), AccentColor.transformedStockDarkAccent());
        assertChannelsNear(0xFFE8710A, AccentColor.getAccentColor(true), 2);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void presetIsStoredInThePatchPreferences() {
        // when
        AccentColor.setPreset(ORANGE);

        // then
        assertEquals(ORANGE, this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString("accent_color", null));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void grayOrStockPresetsLeaveNoCustomAccent() {
        for (String preset : new String[] { AccentColor.STOCK, "#808080", "#FFFFFF", "not a color" }) {
            AccentColor.setPreset(preset);

            assertFalse(preset, AccentColor.hasCustomAccent());
            assertEquals(preset, AccentColor.STOCK_DARK_ACCENT, AccentColor.getAccentColor(true));
            assertEquals(preset, 0xFF9292F9L, AccentColor.transformBrandColor(0xFF9292F9L));
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void systemPresetIsAppliedWhenStored() {
        // when
        AccentColor.setPreset(AccentColor.SYSTEM);

        // then
        assertTrue(AccentColor.hasCustomAccent());
        assertEquals(AccentColor.resolveAccentColor(AccentColor.SYSTEM, true), AccentColor.getAccentColor(true));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void brandColorMapsToTheAccentAsAnUnsignedArgb() {
        // given
        AccentColor.setPreset(ORANGE);

        // when
        final long transformed = AccentColor.transformBrandColor(0xFF9292F9L);

        // then
        assertEquals(0L, transformed & ~ARGB_MASK);
        assertEquals(AccentColor.transformedStockDarkAccent() & ARGB_MASK, transformed);
        assertChannelsNear(0xFFE8710A, (int) transformed, 2);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void brandColorIgnoresBitsAboveTheArgbWord() {
        // given
        AccentColor.setPreset(ORANGE);
        final long plain = AccentColor.transformBrandColor(0xFF6D4AFFL);

        // when
        final long withHighBits = AccentColor.transformBrandColor(0x1_FF6D4AFFL);

        // then
        assertEquals(plain, withHighBits);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void brandColorKeepsItsAlpha() {
        // given
        AccentColor.setPreset(ORANGE);

        // when
        final int transformed = (int) AccentColor.transformBrandColor(0x809292F9L);

        // then
        assertTrue(Math.abs((transformed >>> 24) - 0x80) <= 1);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void keepsAnArgbBrandColorWithoutAPreset() {
        assertEquals(0xFF6D4AFF, AccentColor.transformBrandColorArgb(0xFF6D4AFF));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void transformsAnArgbBrandColorAsTheLongOverloadDoes() {
        // given
        AccentColor.setPreset(ORANGE);

        // when
        final int transformed = AccentColor.transformBrandColorArgb(0xFF6D4AFF);

        // then
        assertEquals((int) AccentColor.transformBrandColor(0xFF6D4AFFL), transformed);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void transformedBrandColorsAreRememberedUntilThePresetChanges() {
        AccentColor.setPreset(ORANGE);
        final long stock = AccentColor.transformBrandColor(0xFF9292F9L);
        final long light = AccentColor.transformBrandColor(0xFF6D4AFFL);

        final Map<Integer, Integer> remembered = AccentColor.transformedBrandColors();
        assertEquals(2, remembered.size());
        assertEquals((int) stock, (int) remembered.get(0xFF9292F9));
        assertEquals((int) light, (int) remembered.get(0xFF6D4AFF));

        remembered.clear();
        assertEquals(2, AccentColor.transformedBrandColors().size());

        AccentColor.setPreset("#34A853");
        assertTrue(AccentColor.transformedBrandColors().isEmpty());
        assertNotEquals(stock, AccentColor.transformBrandColor(0xFF9292F9L));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void packedBrandColorTransformsOnlyTheArgbHalf() {
        // given
        AccentColor.setPreset(ORANGE);

        // when
        final long transformed = AccentColor.transformPackedBrandColor((0xFF9292F9L << 32) | 0x7FL);

        // then
        assertEquals(AccentColor.transformBrandColor(0xFF9292F9L), transformed >>> 32);
        assertEquals(0x7FL, transformed & ARGB_MASK);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void packedBrandColorIsReturnedUnchangedWhenNothingIsTransformed() {
        for (long packed : new long[] { 0L, 0xFF9292F9L << 32, (0xFF6D4AFFL << 32) | 0x7FL, -1L, Long.MIN_VALUE }) {
            assertEquals(packed, AccentColor.transformPackedBrandColor(packed));
        }
    }

    @Test
    @Config(sdk = 28)
    public void systemPresetIsUnavailableBelowAndroid12() {
        assertFalse(AccentColor.isSystemAccentAvailable());
        assertEquals(0, AccentColor.resolvePresetColor(AccentColor.SYSTEM));
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor(AccentColor.SYSTEM, true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(AccentColor.SYSTEM, false));
        assertChannelsNear(0xFFE8710A, AccentColor.resolveAccentColor(ORANGE, true), 2);
    }

    @Test
    @Config(sdk = 25)
    public void customPresetIsIgnoredBelowOreo() {
        assertEquals(AccentColor.STOCK_DARK_ACCENT, AccentColor.resolveAccentColor("#FF0000", true));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor("#FF0000", false));
        assertEquals(AccentColor.STOCK_LIGHT_ACCENT, AccentColor.resolveAccentColor(AccentColor.SYSTEM, false));
    }

}
