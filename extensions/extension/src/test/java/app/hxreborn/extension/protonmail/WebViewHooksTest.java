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
import app.hxreborn.extension.proton.WebMaterialSwitch;
import app.hxreborn.extension.shared.WebTapHighlight;

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
        WebMaterialSwitch.restyleIfEnabled(null);
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
    @Config(shadows = PatchedBuild.Switches.class)
    public void materialSwitchRunsTheBundledScript() {
        // given
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebMaterialSwitch.restyleIfEnabled(view);

        // then
        assertEquals(1, view.scripts.size());
        assertEquals(WebAssets.MATERIAL_SWITCH_WEBVIEW, view.lastScript());
        assertNull(view.lastCallback());
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void materialSwitchSkipsTheScriptWhenTurnedOff() {
        // given
        PatchedBuild.setMaterialSwitchesEnabled(false);
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebMaterialSwitch.restyleIfEnabled(view);

        // then
        assertTrue(view.scripts.isEmpty());
    }

    @Test
    @Config(shadows = PatchedBuild.Switches.class)
    public void materialSwitchRestyleIgnoresTheSetting() {
        // given
        PatchedBuild.setMaterialSwitchesEnabled(false);
        final RecordingWebView view = new RecordingWebView(this.activity, View.VISIBLE);

        // when
        WebMaterialSwitch.restyle(view);

        // then
        assertEquals(WebAssets.MATERIAL_SWITCH_WEBVIEW, view.lastScript());
    }

    @Test
    public void materialSwitchFailureDoesNotEscape() {
        WebMaterialSwitch.restyleIfEnabled(new WebView(this.activity) {

            @Override
            public void evaluateJavascript(String script, ValueCallback<String> callback) {
                throw new IllegalStateException("no web view");
            }

        });
    }

}
