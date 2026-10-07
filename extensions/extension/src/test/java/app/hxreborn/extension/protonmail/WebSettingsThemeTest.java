/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import app.hxreborn.extension.WebAssets;
import app.hxreborn.extension.proton.AccentColor;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public final class WebSettingsThemeTest {

    private static final String ORANGE = "#E8710A";

    private static final Pattern TONE_MAP = Pattern.compile("colorMap = (\\{[^}]*\\}),");

    private static final Pattern HUE_SHIFT = Pattern.compile("hueShift = ([^,\\s]+),");

    private static final Pattern SATURATION_SCALE = Pattern.compile("saturationScale = ([^,\\s]+),");

    private static final Pattern TONE_MAP_LITERAL = Pattern
        .compile("\\{('\\d+,\\d+,\\d+':'\\d+,\\d+,\\d+'(,'\\d+,\\d+,\\d+':'\\d+,\\d+,\\d+')*)?\\}");

    private static final Pattern TONE = Pattern.compile("'(\\d+,\\d+,\\d+)':'(\\d+,\\d+,\\d+)'");

    private Context context;

    private Activity activity;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
        this.activity = Robolectric.buildActivity(Activity.class).setup().get();
        PatchedBuild.setAccentPreset("");
    }

    private RecordingWebView webView(int visibility) {
        return new RecordingWebView(this.activity, visibility);
    }

    private static void idleMainLooper(long millis) {
        ShadowLooper.idleMainLooper(millis, TimeUnit.MILLISECONDS);
    }

    private static float hueDegrees(int argb) {
        final float[] hsv = new float[3];
        Color.colorToHSV(argb, hsv);
        return hsv[0];
    }

    private static float hslSaturation(int argb) {
        final float red = Color.red(argb) / 255f;
        final float green = Color.green(argb) / 255f;
        final float blue = Color.blue(argb) / 255f;
        final float max = Math.max(red, Math.max(green, blue));
        final float min = Math.min(red, Math.min(green, blue));
        final float lightness = (max + min) / 2;
        return (max - min) / (1 - Math.abs(2 * lightness - 1));
    }

    private static String channels(int argb) {
        return Color.red(argb) + "," + Color.green(argb) + "," + Color.blue(argb);
    }

    private static String group(Pattern pattern, String script) {
        final Matcher matcher = pattern.matcher(script);
        assertTrue(script, matcher.find());
        return matcher.group(1);
    }

    private static Map<String, String> tones(String literal) {
        final Map<String, String> tones = new HashMap<>();
        final Matcher matcher = TONE.matcher(literal);
        while (matcher.find()) {
            tones.put(matcher.group(1), matcher.group(2));
        }
        return tones;
    }

    private static void assertRecolor(String script, String preset) {
        final int accent = AccentColor.transformedStockDarkAccent();
        final float expectedShift = hueDegrees(accent) - hueDegrees(AccentColor.STOCK_DARK_ACCENT);
        final float expectedScale = hslSaturation(accent) / hslSaturation(AccentColor.STOCK_DARK_ACCENT);

        assertEquals(preset, expectedShift, Float.parseFloat(group(HUE_SHIFT, script)), 0.01f);
        assertEquals(preset, expectedScale, Float.parseFloat(group(SATURATION_SCALE, script)), 0.001f);
    }

    @Test
    public void unpatchedBuildLeavesTheSettingsViewVisible() {
        // given
        PatchedBuild.setAmoledEnabled(true);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.hideBeforeStyling(view);

        // then
        idleMainLooper(5000);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    public void unpatchedBuildShowsTheViewWithoutInjecting() {
        // given
        PatchedBuild.setAmoledEnabled(true);
        final RecordingWebView view = webView(View.INVISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertTrue(view.scripts.isEmpty());
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    public void missingViewIsIgnored() {
        WebSettingsTheme.hideBeforeStyling(null);
        WebSettingsTheme.injectEnabledStyles(null);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void viewIsHiddenFor2500MillisecondsWhenAmoledStylesIt() {
        // given
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.hideBeforeStyling(view);

        // then
        assertEquals(View.INVISIBLE, view.getVisibility());
        idleMainLooper(2499);
        assertEquals(View.INVISIBLE, view.getVisibility());
        idleMainLooper(1);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void viewIsHiddenWhenOnlyTheAccentStylesIt() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.hideBeforeStyling(view);

        // then
        assertEquals(View.INVISIBLE, view.getVisibility());
        idleMainLooper(2500);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void viewIsNotHiddenWhenNothingStylesIt() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.hideBeforeStyling(view);

        // then
        assertEquals(View.VISIBLE, view.getVisibility());
        idleMainLooper(5000);
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void amoledStyleShowsTheViewAfterInjecting() {
        // given
        final RecordingWebView view = webView(View.INVISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertEquals(1, view.scripts.size());
        assertEquals(WebAssets.AMOLED_WEBVIEW, view.lastScript());
        assertNull(view.lastCallback());
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void nothingIsInjectedWhenNeitherStyleIsOn() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        final RecordingWebView view = webView(View.INVISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertTrue(view.scripts.isEmpty());
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void accentStyleWaitsForTheStyleToBeReadyBeforeShowingTheView() {
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final RecordingWebView view = webView(View.INVISIBLE);

        WebSettingsTheme.injectEnabledStyles(view);

        assertEquals(2, view.scripts.size());
        assertTrue(view.scripts.get(0).contains("colorMap"));
        assertEquals(WebAssets.ACCENT_STYLE_READY, view.lastScript());
        assertEquals(View.INVISIBLE, view.getVisibility());

        view.lastCallback().onReceiveValue("false");
        assertEquals(View.INVISIBLE, view.getVisibility());
        idleMainLooper(99);
        assertEquals(2, view.scripts.size());
        idleMainLooper(1);
        assertEquals(3, view.scripts.size());
        assertEquals(WebAssets.ACCENT_STYLE_READY, view.lastScript());

        view.lastCallback().onReceiveValue("null");
        idleMainLooper(100);
        assertEquals(4, view.scripts.size());
        assertEquals(View.INVISIBLE, view.getVisibility());

        view.lastCallback().onReceiveValue("true");
        assertEquals(View.VISIBLE, view.getVisibility());
        idleMainLooper(1000);
        assertEquals(4, view.scripts.size());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void visibleViewIsNotCheckedForTheAccentStyle() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertEquals(1, view.scripts.size());
        assertTrue(view.lastScript().contains("colorMap"));
        assertEquals(View.VISIBLE, view.getVisibility());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void amoledStyleIsInjectedBeforeTheAccentStyle() {
        // given
        PatchedBuild.setAccentPreset(ORANGE);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertEquals(2, view.scripts.size());
        assertEquals(WebAssets.AMOLED_WEBVIEW, view.scripts.get(0));
        assertTrue(view.scripts.get(1).contains("colorMap"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void accentScriptFillsEveryPlaceholder() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        AccentColor.transformBrandColor(0xFF9292F9L);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        final String script = view.lastScript();
        assertFalse(script.contains("__TONES__"));
        assertFalse(script.contains("__SHIFT__"));
        assertFalse(script.contains("__SATURATION__"));
        assertEquals(WebAssets.ACCENT_RECOLOR.replace("__TONES__", group(TONE_MAP, script))
            .replace("__SHIFT__", group(HUE_SHIFT, script))
            .replace("__SATURATION__", group(SATURATION_SCALE, script)), script);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void accentScriptRecolorsTheStockAccentForEachPreset() {
        PatchedBuild.setAmoledEnabled(false);
        for (String preset : new String[] { ORANGE, "#D93025", "#34A853", "#4C8BF5", "#7B1FA2", "#FF0080", "#00FF00",
                "#FFFF00", "#0000FF", "#FFE0E0", "#00005F", "#FF00FF" }) {
            PatchedBuild.setAccentPreset(preset);
            final RecordingWebView view = webView(View.VISIBLE);

            WebSettingsTheme.injectEnabledStyles(view);

            assertRecolor(view.lastScript(), preset);
        }
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void pinkAccentShiftsTheStockHueByNinetyDegrees() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset("#FF0080");
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertEquals(329.88f - 240f, Float.parseFloat(group(HUE_SHIFT, view.lastScript())), 0.5f);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void emptyColorMapIsAnEmptyObjectLiteral() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertEquals("{}", group(TONE_MAP, view.lastScript()));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void colorMapMapsBrandChannelsToAccentChannels() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final int accent = (int) AccentColor.transformBrandColor(0xFF9292F9L);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        final String literal = group(TONE_MAP, view.lastScript());
        assertTrue(literal, TONE_MAP_LITERAL.matcher(literal).matches());
        assertEquals("{'146,146,249':'" + channels(accent) + "'}", literal);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void colorMapSeparatesEntriesWithCommas() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        final Map<String, String> expected = new HashMap<>();
        for (long brand : new long[] { 0xFF9292F9L, 0xFF6D4AFFL, 0xFF000001L, 0xFFFFFFFFL }) {
            expected.put(channels((int) brand), channels((int) AccentColor.transformBrandColor(brand)));
        }
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        final String literal = group(TONE_MAP, view.lastScript());
        assertTrue(literal, TONE_MAP_LITERAL.matcher(literal).matches());
        assertEquals(4, expected.size());
        assertEquals(expected, tones(literal));
        assertEquals(3, (literal.length() - literal.replace("','", "").length()) / 3);
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void colorMapIgnoresAlphaOfTheBrandColors() {
        // given
        PatchedBuild.setAmoledEnabled(false);
        PatchedBuild.setAccentPreset(ORANGE);
        AccentColor.transformBrandColor(0x80010203L);
        final RecordingWebView view = webView(View.VISIBLE);

        // when
        WebSettingsTheme.injectEnabledStyles(view);

        // then
        assertNotNull(tones(group(TONE_MAP, view.lastScript())).get("1,2,3"));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void scriptFailureDoesNotEscape() {
        // given
        final WebView failing = new WebView(this.activity) {

            @Override
            public void evaluateJavascript(String script, ValueCallback<String> callback) {
                throw new IllegalStateException("no web view");
            }

        };

        // when
        WebSettingsTheme.injectEnabledStyles(failing);
    }

}
