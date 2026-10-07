/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import app.hxreborn.extension.WebAssets;
import app.hxreborn.extension.proton.PatchedBuild;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class WebViewHooksTest {

    private Activity activity;

    @Before
    public void useApplicationContext() {
        PatchedBuild.useApplicationContext();
        PatchedBuild.setAccentPreset("");
        this.activity = Robolectric.buildActivity(Activity.class).setup().get();
    }

    private String switchScript(String thumb) {
        return WebAssets.MATERIAL_SWITCH_WEBVIEW.replace("__CHECKED_THUMB__", thumb);
    }

    @Test
    public void settingsThemeIgnoresAMissingViewBeforeStyling() {
        WebSettingsTheme.hideBeforeStyling(null);
    }

    @Test
    public void settingsThemeIgnoresAMissingViewWhenInjecting() {
        WebSettingsTheme.injectEnabledStyles(null);
    }

    @Test
    public void tapHighlightIgnoresAMissingView() {
        WebTapHighlight.remove(null);
    }

    @Test
    public void materialSwitchIgnoresAMissingView() {
        WebMaterialSwitch.apply(null);
    }

    @Test
    public void tapHighlightIsRemovedWithTheBundledScript() {
        // given
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebTapHighlight.remove(view);

        // then
        assertEquals(1, view.scripts.size());
        assertEquals(WebAssets.TAP_HIGHLIGHT_WEBVIEW, view.lastScript());
        assertNull(view.lastCallback());
    }

    @Test
    public void tapHighlightFailureDoesNotEscape() {
        WebTapHighlight.remove(new WebView(this.activity) {

            @Override
            public void evaluateJavascript(String script, ValueCallback<String> callback) {
                throw new IllegalStateException("no web view");
            }

        });
    }

    @Test
    public void materialSwitchTemplateCarriesTheThumbPlaceholder() {
        assertTrue(WebAssets.MATERIAL_SWITCH_WEBVIEW.contains("__CHECKED_THUMB__"));
    }

    @Test
    public void lightThemeStockAccentGetsAWhiteThumb() {
        // given
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebMaterialSwitch.apply(view);

        // then
        assertEquals(1, view.scripts.size());
        assertEquals(switchScript("#FFFFFF"), view.lastScript());
        assertNull(view.lastCallback());
    }

    @Test
    @Config(qualifiers = "night")
    public void darkThemeStockAccentGetsABlackThumb() {
        // given
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebMaterialSwitch.apply(view);

        // then
        assertEquals(1, view.scripts.size());
        assertEquals(switchScript("#000000"), view.lastScript());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void thumbContrastsWithTheLightThemeAccent() {
        PatchedBuild.setAccentPreset("#0000FF");
        final RecordingWebView blue = new RecordingWebView(this.activity, View.VISIBLE);
        WebMaterialSwitch.apply(blue);
        assertEquals(switchScript("#FFFFFF"), blue.lastScript());

        PatchedBuild.setAccentPreset("#FFFF00");
        final RecordingWebView yellow = new RecordingWebView(this.activity, View.VISIBLE);
        WebMaterialSwitch.apply(yellow);
        assertEquals(switchScript("#000000"), yellow.lastScript());
    }

    @Test
    @Config(qualifiers = "night", shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class,
            PatchedBuild.Upselling.class, PatchedBuild.Applied.class })
    public void thumbContrastsWithTheDarkThemeAccent() {
        PatchedBuild.setAccentPreset("#0000FF");
        final RecordingWebView blue = new RecordingWebView(this.activity, View.VISIBLE);
        WebMaterialSwitch.apply(blue);
        assertEquals(switchScript("#FFFFFF"), blue.lastScript());

        PatchedBuild.setAccentPreset("#FFFF00");
        final RecordingWebView yellow = new RecordingWebView(this.activity, View.VISIBLE);
        WebMaterialSwitch.apply(yellow);
        assertEquals(switchScript("#000000"), yellow.lastScript());
    }

    @Test
    public void materialSwitchFailureDoesNotEscape() {
        WebMaterialSwitch.apply(new WebView(this.activity) {

            @Override
            public void evaluateJavascript(String script, ValueCallback<String> callback) {
                throw new IllegalStateException("no web view");
            }

        });
    }

}
