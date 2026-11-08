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
package app.hxreborn.extension.ultimea;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorSpace;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 31)
public class AccentPaletteTest {

    private static final String PREFERENCE_KEY = "accent_color";

    private static final int LIGHT_PRIMARY = 0xff541597;

    private static final int DARK_PRIMARY = 0xffc499ff;

    private static final int LIGHT_SURFACE = 0xffeee5f7;

    private static final int DARK_SURFACE = 0xff332343;

    private static final String CUSTOM = "#c060f0";

    private static final int FIRST_SYSTEM_COLOR = 0xffaa2233;

    private static final int SECOND_SYSTEM_COLOR = 0xff2277bb;

    private Context context;

    private SharedPreferences preferences;

    @Before
    public void clearPreferences() {
        this.context = RuntimeEnvironment.getApplication();
        this.preferences = this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.preferences.edit().clear().commit();
    }

    @After
    public void clearSavedSelection() {
        this.preferences.edit().clear().commit();
    }

    @Test
    public void restoresSelectionWhenCommitReportsFailure() {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.BLUE.preset));
        int[] commits = { 0 };
        SharedPreferences failing = (SharedPreferences) java.lang.reflect.Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[] { SharedPreferences.class },
                (proxy, method, arguments) -> {
                    if (!"edit".equals(method.getName())) {
                        return method.invoke(this.preferences, arguments);
                    }
                    SharedPreferences.Editor editor = this.preferences.edit();
                    return java.lang.reflect.Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                            new Class<?>[] { SharedPreferences.Editor.class }, (editorProxy, operation, values) -> {
                                Object result = operation.invoke(editor, values);
                                if ("commit".equals(operation.getName())) {
                                    return ++commits[0] != 1;
                                }
                                return (result == editor) ? editorProxy : result;
                            });
                });
        Context wrapped = new ContextWrapper(this.context) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return failing;
            }
        };
        Assert.assertFalse(AccentColor.setPreset(wrapped, AccentPalette.Fixed.RED.preset));
        Assert.assertEquals(AccentPalette.Fixed.BLUE.preset, AccentPalette.getPreset(this.context));
        Assert.assertEquals(2, commits[0]);
    }

    @Test
    @Config(sdk = 26)
    public void preservesSwatchWhenAccentDialogOpens() {
        try (org.robolectric.android.controller.ActivityController<android.app.Activity> controller = org.robolectric.Robolectric
            .buildActivity(android.app.Activity.class)
            .setup()) {
            AccentColorDialog.show(controller.get());
            android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            Assert.assertTrue(dialog.isShowing());
            android.view.View focused = dialog.getCurrentFocus();
            Assert.assertFalse(focused instanceof android.widget.EditText);
            dialog.dismiss();
        }
    }

    @Test
    public void preservesExactStockRoles() {
        Assert.assertEquals(AccentPalette.STOCK, AccentPalette.getPreset(this.context));
        Assert.assertEquals("ULTIMEA purple", AccentPalette.label(this.context));
        Assert.assertEquals(LIGHT_PRIMARY, AccentPalette.primary(this.context, false));
        Assert.assertEquals(DARK_PRIMARY, AccentPalette.primary(this.context, true));
        Assert.assertEquals(LIGHT_SURFACE, AccentPalette.surface(this.context, false));
        Assert.assertEquals(DARK_SURFACE, AccentPalette.surface(this.context, true));
        Assert.assertEquals(0x33541597, AccentPalette.transform(this.context, 0x33541597));
    }

    @Test
    public void rejectsMalformedSelectionsWithoutReplacingSavedChoice() {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.BLUE.preset));
        for (String invalid : new String[] { null, "blue", "SYSTEM", "#fff", "#00112233", "#gg1122", " #001122" }) {
            Assert.assertFalse(AccentPalette.setPreset(this.context, invalid));
            Assert.assertEquals(AccentPalette.Fixed.BLUE.preset, AccentPalette.getPreset(this.context));
        }
    }

    @Test
    public void normalizesValidHexAndPreservesUnknownColorLabel() {
        Assert.assertTrue(AccentPalette.setPreset(this.context, CUSTOM));
        Assert.assertEquals("#C060F0", AccentPalette.getPreset(this.context));
        Assert.assertEquals("Custom #C060F0", AccentPalette.label(this.context));
    }

    @Test
    public void exposesRequestedFixedChoices() {
        String[] names = { "Red", "Orange", "Gold", "Green", "Teal", "Cyan", "Blue", "Pink" };
        int[] hues = { 0, 28, 48, 105, 168, 195, 220, 325 };
        AccentPalette.Fixed[] choices = AccentPalette.Fixed.values();
        Assert.assertEquals(names.length, choices.length);
        for (int index = 0; index < choices.length; index++) {
            Assert.assertEquals(names[index], choices[index].label);
            float[] hsv = new float[3];
            Color.colorToHSV(Color.parseColor(choices[index].preset), hsv);
            Assert.assertEquals(hues[index], hsv[0], 0.2);
            Assert.assertTrue(AccentPalette.setPreset(this.context, choices[index].preset));
            Assert.assertEquals(names[index], AccentPalette.label(this.context));
        }
    }

    @Test
    public void preservesAlphaAndLabLightnessAcrossFixedChoices() {
        int[] sources = { LIGHT_PRIMARY, DARK_PRIMARY, LIGHT_SURFACE, DARK_SURFACE, 0x33541597 };
        for (AccentPalette.Fixed fixed : AccentPalette.Fixed.values()) {
            Assert.assertTrue(AccentPalette.setPreset(this.context, fixed.preset));
            for (int source : sources) {
                int actual = AccentPalette.transform(this.context, source);
                Assert.assertEquals(Color.alpha(source), Color.alpha(actual));
                Assert.assertEquals(fixed.label, lightness(source), lightness(actual), 0.8);
            }
        }
    }

    @Test
    public void rotatesPrimaryHueTowardEveryFixedChoice() {
        for (AccentPalette.Fixed fixed : AccentPalette.Fixed.values()) {
            Assert.assertTrue(AccentPalette.setPreset(this.context, fixed.preset));
            int expected = Color.parseColor(fixed.preset);
            Assert.assertTrue(fixed.label, hueDistance(expected, AccentPalette.primary(this.context, false)) < 8);
            Assert.assertTrue(fixed.label, hueDistance(expected, AccentPalette.primary(this.context, true)) < 8);
        }
    }

    @Test
    public void preservesNeutralColors() {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
        for (int neutral : new int[] { Color.BLACK, Color.WHITE, Color.GRAY, Color.TRANSPARENT }) {
            Assert.assertEquals(neutral, AccentPalette.transform(this.context, neutral));
        }
    }

    @Test
    public void resetsOnlyAccentSelection() {
        this.preferences.edit().putBoolean(PatchSettings.REMOVE_POPUPS, false).commit();
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.STOCK));
        Assert.assertFalse(this.preferences.contains(PREFERENCE_KEY));
        Assert.assertFalse(this.preferences.getBoolean(PatchSettings.REMOVE_POPUPS, true));
        Assert.assertEquals(DARK_PRIMARY, AccentPalette.primary(this.context, true));
    }

    @Test
    public void fallsBackForMalformedStoredPreference() {
        this.preferences.edit().putString(PREFERENCE_KEY, "invalid").commit();
        Assert.assertEquals(AccentPalette.STOCK, AccentPalette.getPreset(this.context));
        this.preferences.edit().putInt(PREFERENCE_KEY, 1).commit();
        Assert.assertEquals(AccentPalette.STOCK, AccentPalette.getPreset(this.context));
    }

    @Test
    public void readsCurrentSystemColorWithoutCaching() {
        PaletteContext system = new PaletteContext(this.context);
        Assert.assertTrue(AccentPalette.isSystemAvailable());
        Assert.assertTrue(AccentPalette.setPreset(system, AccentPalette.SYSTEM));
        Assert.assertEquals("Material You", AccentPalette.label(system));
        system.palette.color = FIRST_SYSTEM_COLOR;
        int first = AccentPalette.primary(system, true);
        system.palette.color = SECOND_SYSTEM_COLOR;
        int second = AccentPalette.primary(system, true);
        Assert.assertNotEquals(first, second);
        Assert.assertEquals(AccentPalette.SYSTEM, AccentPalette.getPreset(system));
    }

    @Test
    public void appliesNeutralSystemPaletteWithoutRestoringPurple() {
        PaletteContext system = new PaletteContext(this.context);
        system.palette.color = Color.GRAY;
        Assert.assertTrue(AccentPalette.setPreset(system, AccentPalette.SYSTEM));
        int source = 0x33541597;
        int actual = AccentPalette.transform(system, source);
        float[] lab = Color.valueOf(actual).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB)).getComponents();
        Assert.assertNotEquals(source, actual);
        Assert.assertEquals(Color.alpha(source), Color.alpha(actual));
        Assert.assertEquals(lightness(source), lab[0], 0.8);
        Assert.assertTrue(Math.hypot(lab[1], lab[2]) < 1);
        Assert.assertEquals(Color.GRAY, AccentPalette.transform(system, Color.GRAY));
        Assert.assertEquals(Color.WHITE, AccentPalette.transform(system, Color.WHITE));
        Assert.assertEquals(Color.BLACK, AccentPalette.transform(system, Color.BLACK));
    }

    @Test
    @Config(sdk = 28)
    public void fallsBackForSystemChoiceBeforeAndroidTwelve() {
        Assert.assertFalse(AccentPalette.isSystemAvailable());
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.SYSTEM));
        Assert.assertEquals(LIGHT_PRIMARY, AccentPalette.primary(this.context, false));
        Assert.assertEquals(DARK_PRIMARY, AccentPalette.primary(this.context, true));
    }

    @Test
    @Config(sdk = 23)
    public void fallsBackBeforeLabConversionIsAvailable() {
        Assert.assertTrue(AccentPalette.setPreset(this.context, AccentPalette.Fixed.RED.preset));
        Assert.assertEquals(LIGHT_PRIMARY, AccentPalette.primary(this.context, false));
        Assert.assertEquals(DARK_PRIMARY, AccentPalette.primary(this.context, true));
        Assert.assertEquals(LIGHT_SURFACE, AccentPalette.surface(this.context, false));
    }

    @Test
    public void handlesMissingContext() {
        Assert.assertEquals(AccentPalette.STOCK, AccentPalette.getPreset(null));
        Assert.assertFalse(AccentPalette.setPreset(null, AccentPalette.Fixed.RED.preset));
        Assert.assertEquals(DARK_PRIMARY, AccentPalette.primary(null, true));
    }

    private static float lightness(int color) {
        return Color.valueOf(color).convert(ColorSpace.get(ColorSpace.Named.CIE_LAB)).getComponents()[0];
    }

    private static double hueDistance(int first, int second) {
        ColorSpace lab = ColorSpace.get(ColorSpace.Named.CIE_LAB);
        float[] firstLab = Color.valueOf(first).convert(lab).getComponents();
        float[] secondLab = Color.valueOf(second).convert(lab).getComponents();
        double difference = Math
            .abs(Math.toDegrees(Math.atan2(firstLab[2], firstLab[1]) - Math.atan2(secondLab[2], secondLab[1]))) % 360;
        return Math.min(difference, 360 - difference);
    }

    private static final class PaletteContext extends ContextWrapper {

        private final PaletteResources palette;

        private PaletteContext(Context base) {
            super(base);
            this.palette = new PaletteResources(base.getResources());
        }

        @Override
        public Resources getResources() {
            return this.palette;
        }

    }

    private static final class PaletteResources extends Resources {

        private int color;

        private PaletteResources(Resources original) {
            super(original.getAssets(), original.getDisplayMetrics(), original.getConfiguration());
        }

        @Override
        public int getColor(int id, Theme theme) throws NotFoundException {
            return id == android.R.color.system_accent1_500 ? this.color : super.getColor(id, theme);
        }

    }

}
