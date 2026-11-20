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
package app.hxreborn.extension.ultimea.equalizer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ScrollView;
import android.graphics.Typeface;
import android.text.TextUtils;
import org.robolectric.shadows.ShadowPopupMenu;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "mdpi", shadows = { FineEqualizerTest.ConnectionShadow.class,
        FineEqualizerTest.AppliedFlagsShadow.class, FineEqualizerTest.BasesShadow.class })
@SuppressWarnings({ "deprecation", "DataFlowIssue" })
public class FineEqualizerTest {

    private ActivityController<Activity> controller;

    private Activity activity;

    @Before
    public void openHost() {
        this.controller = Robolectric.buildActivity(Activity.class).setup();
        this.activity = this.controller.get();
        this.activity.getSharedPreferences("hx_ultimea_equalizer_presets", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
        this.activity.setContentView(new FrameLayout(this.activity));
        ConnectionShadow.current = new SoundState(
                new EqualizerCurve(1, new int[] { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 }), 1);
        ConnectionShadow.applies = 0;
        ConnectionShadow.deferApply = false;
        ConnectionShadow.pendingApply = null;
        FineEqualizer.attach(this.activity);
    }

    @After
    public void closeHost() {
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        this.controller.close();
        this.activity.getSharedPreferences("hx_ultimea_equalizer_presets", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void openDefaultWithoutSendingSoundChange() {
        FineEqualizer.open(this.activity);
        ViewGroup root = root();
        Assert.assertNotNull(button(root, "Default"));
        Assert.assertNotNull(button(root, "Custom"));
        Assert.assertNotNull(button(root, "MOVIE"));
        Assert.assertNotNull(button(root, "MUSIC"));
        Assert.assertNull(button(root, "Apply"));
        Assert.assertNull(button(root, "Close"));
        Assert.assertEquals(0, ConnectionShadow.applies);
        Assert.assertEquals(1, ConnectionShadow.current.curve.mode());
        Assert.assertEquals(10, bands(root).size());
        for (SeekBar band : bands(root)) {
            Assert.assertFalse(band.isEnabled());
            Assert.assertTrue(band.getContentDescription().toString().contains("dB"));
        }
    }

    @Test
    public void showOnlyFineControlsInCustomTab() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        Assert.assertNull(button(root(), "Style"));
        Assert.assertNull(button(root(), "MOVIE"));
        Assert.assertNotNull(description(root(), "1000 Hz increase 0.1 dB"));
        Assert.assertNotNull(description(root(), "New preset from current curve"));
        for (SeekBar band : bands(root())) {
            Assert.assertFalse(band.isEnabled());
        }
    }

    @Test
    public void combineTenStepsAndSaveConfirmedCurve() throws Exception {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        ConnectionShadow.applies = 0;
        View plus = description(root(), "1000 Hz increase 0.1 dB");
        for (int step = 0; step < 10; step++) {
            plus.performClick();
        }
        Assert.assertEquals(0, ConnectionShadow.applies);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(299));
        Assert.assertEquals(0, ConnectionShadow.applies);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
        Assert.assertEquals(1, ConnectionShadow.applies);
        Assert.assertEquals(20, ConnectionShadow.current.curve.gain(5));
        List<EqualizerPreset> presets = new EqualizerPresetStore(this.activity).load();
        Assert.assertEquals(1, presets.size());
        Assert.assertEquals(ConnectionShadow.current, presets.get(0).state);
    }

    @Test
    public void reopenAfterPendingSaveCompletes() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        ConnectionShadow.applies = 0;
        ConnectionShadow.deferApply = true;
        description(root(), "1000 Hz increase 0.1 dB").performClick();
        ShadowDialog.getLatestDialog().dismiss();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertNotNull(ConnectionShadow.pendingApply);
        FineEqualizer.open(this.activity);
        Assert.assertFalse(ShadowDialog.getLatestDialog().isShowing());
        ConnectionShadow.finishApply();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertTrue(ShadowDialog.getLatestDialog().isShowing());
        Assert.assertTrue(description(root(), "1000 Hz increase 0.1 dB").isEnabled());
        Assert.assertEquals(11, ConnectionShadow.current.curve.gain(5));
        Assert.assertEquals(1, ConnectionShadow.applies);
    }

    @Test
    public void undoConfirmedStepsWithoutApply() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        SoundState original = ConnectionShadow.current;
        description(root(), "1000 Hz increase 0.1 dB").performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
        Assert.assertTrue(description(root(), "Undo").isEnabled());
        description(root(), "Undo").performClick();
        Assert.assertEquals(original, ConnectionShadow.current);
        Assert.assertFalse(description(root(), "Undo").isEnabled());
    }

    @Test
    public void retainOneRemoteCardAndOriginalHost() {
        ViewGroup content = this.activity.findViewById(android.R.id.content);
        ViewGroup wrapper = (ViewGroup) content.getChildAt(0);
        Assert.assertEquals(2, wrapper.getChildCount());
        ViewGroup inset = (ViewGroup) wrapper.getChildAt(1);
        Assert.assertEquals(1, inset.getChildCount());
        View remote = inset.getChildAt(0);
        Assert.assertEquals("Remote controls", remote.getContentDescription());
        Assert.assertEquals(View.GONE, remote.getVisibility());
        Assert.assertEquals(View.GONE, inset.getVisibility());
        FineEqualizer.navigation("flutter/navigation",
                ByteBuffer.wrap("{\"method\":\"routeInformationUpdated\",\"args\":{\"uri\":\"/device-detail\"}}"
                    .getBytes(StandardCharsets.UTF_8)));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(View.VISIBLE, remote.getVisibility());
        Assert.assertEquals(View.VISIBLE, inset.getVisibility());
        FineEqualizer.navigation("flutter/navigation",
                ByteBuffer.wrap("{\"args\":{\"uri\":\"/profile\"}}".getBytes(StandardCharsets.UTF_8)));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(View.GONE, inset.getVisibility());
        FineEqualizer.attach(this.activity);
        Assert.assertEquals(1, content.getChildCount());
        Assert.assertEquals(0, ConnectionShadow.applies);
    }

    @Test
    public void showReadOnlyCurveAndThreeRemoteRows() {
        ViewGroup content = this.activity.findViewById(android.R.id.content);
        ViewGroup wrapper = (ViewGroup) content.getChildAt(0);
        ViewGroup inset = (ViewGroup) wrapper.getChildAt(1);
        inset.getChildAt(0).performClick();
        Assert.assertNotNull(description(root(), "Bass decrease 0.1 dB"));
        Assert.assertNotNull(description(root(), "Midrange increase 0.1 dB"));
        Assert.assertNotNull(description(root(), "Treble increase 0.1 dB"));
        for (SeekBar band : bands(root())) {
            Assert.assertFalse(band.isEnabled());
        }
        description(root(), "Bass increase 0.1 dB").performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
        Assert.assertEquals(31, ConnectionShadow.current.curve.gain(0));
        Assert.assertEquals(41, ConnectionShadow.current.curve.gain(1));
        Assert.assertEquals(0, ConnectionShadow.current.curve.gain(4));
    }

    @Test
    @Config(qualifiers = "land-mdpi")
    public void keepActionsReachableInLandscapeWithLargeFont() {
        android.content.res.Configuration configuration = new android.content.res.Configuration(
                this.activity.getResources().getConfiguration());
        configuration.fontScale = 2f;
        this.activity.getResources()
            .updateConfiguration(configuration, this.activity.getResources().getDisplayMetrics());
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        ViewGroup root = root();
        root.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 640, 360);
        for (String label : new String[] { "Back", "Undo", "More options", "Previous band", "Next band",
                "1000 Hz increase 0.1 dB", "1000 Hz decrease 0.1 dB" }) {
            View control = description(root, label);
            Assert.assertNotNull(label, control);
            Assert.assertTrue(label, control.getMeasuredHeight() >= 48);
            Assert.assertTrue(label, control.getMeasuredWidth() >= 48);
        }
        Assert.assertEquals(0, ConnectionShadow.applies);
    }

    @Test
    public void centerHeaderIconsAlongsideTitle() {
        FineEqualizer.open(this.activity);
        measure(450, 900);
        Button back = (Button) description(root(), "Back");
        TextView title = text(root(), "Fine equalizer");
        Assert.assertSame(back.getParent(), title.getParent());
        Assert.assertEquals(back.getTop(), title.getTop());
        Assert.assertEquals(back.getHeight(), title.getHeight());
        Assert.assertNull(back.getCompoundDrawables()[1]);
        Assert.assertEquals(24f, title.getTextSize(), 0f);
        Assert.assertEquals(Typeface.NORMAL, title.getTypeface().getStyle());
    }

    @Test
    public void orderModeCardsAndSendGameSixAndNightFive() {
        FineEqualizer.open(this.activity);
        List<String> names = new ArrayList<>();
        collectModes(root(), names);
        Assert.assertEquals(java.util.Arrays.asList("MOVIE", "MUSIC", "VOICE", "SPORT", "GAME", "NIGHT"), names);
        button(root(), "GAME").performClick();
        Assert.assertEquals(6, ConnectionShadow.current.curve.mode());
        button(root(), "NIGHT").performClick();
        Assert.assertEquals(5, ConnectionShadow.current.curve.mode());
        Assert.assertNull(button(root(), "NIGHT").getCompoundDrawables()[1]);
    }

    @Test
    public void showModeCurveWithoutNameOrSaveRow() {
        FineEqualizer.open(this.activity);
        Assert.assertEquals(View.GONE, text(root(), "Saved").getVisibility());
        Assert.assertNull(text(root(), "Untitled"));
        int[] expected = { 30, 40, 20, 10, 0, 10, 10, 0, -20, -20 };
        List<SeekBar> sliders = bands(root());
        for (int band = 0; band < sliders.size(); band++) {
            Assert.assertEquals(expected[band], sliders.get(band).getProgress() + EqualizerCurve.MIN_GAIN_TENTHS);
            Assert.assertFalse(sliders.get(band).isEnabled());
        }
    }

    @Test
    public void leaveModesUnselectedAndGraphAbsentForCustomSound() {
        ConnectionShadow.current = new SoundState(new EqualizerCurve(EqualizerCurve.CUSTOM_MODE, new int[10]), 1);
        FineEqualizer.open(this.activity);
        button(root(), "Default").performClick();
        for (String label : new String[] { "MOVIE", "MUSIC", "VOICE", "SPORT", "GAME", "NIGHT" }) {
            Assert.assertFalse(button(root(), label).isSelected());
        }
        Assert.assertEquals(View.GONE, ((View) bands(root()).get(0).getParent()).getVisibility());
        Assert.assertFalse(button(root(), "Customize current mode").isEnabled());
    }

    @Test
    public void scrollToCustomizeAboveBottomPadding() {
        FineEqualizer.open(this.activity);
        measure(450, 600);
        Button customize = button(root(), "Customize current mode");
        ScrollView scroll = scroll(root());
        scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
        android.graphics.Rect bounds = new android.graphics.Rect();
        customize.getDrawingRect(bounds);
        scroll.offsetDescendantRectToMyCoords(customize, bounds);
        Assert.assertTrue(scroll.getScrollY() > 0);
        Assert.assertTrue(bounds.bottom - scroll.getScrollY() <= scroll.getHeight() - scroll.getPaddingBottom());
        Assert.assertTrue(bounds.top - scroll.getScrollY() >= 0);
    }

    @Test
    @Config(sdk = 35)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void showSaveCaptionWithoutDuplicatedNameAndPadPresetNames() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        description(root(), "New preset from current curve").performClick();
        measure(450, 900);
        Button preset = button(root(), "Untitled 2");
        Assert.assertTrue(preset.getPaddingLeft() >= 12);
        Assert.assertTrue(preset.getPaddingRight() >= 12);
        Assert.assertEquals(TextUtils.TruncateAt.END, preset.getEllipsize());
        Assert.assertNull(text(root(), "Untitled 2"));
        TextView saved = text(root(), "Saved");
        Assert.assertEquals(new app.hxreborn.extension.ultimea.HostStyle(this.activity).secondaryText,
                saved.getCurrentTextColor());
        Assert.assertEquals(14f, saved.getTextSize(), 0f);
        Assert.assertTrue("Caption height " + saved.getHeight(), saved.getHeight() < 48);
        ViewGroup body = (ViewGroup) saved.getParent();
        Assert.assertEquals(body.getWidth() - body.getPaddingRight(), saved.getRight());
    }

    @Test
    @Config(sdk = 35)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void putXupmixChevronOnRightAndSeparatePills() {
        FineEqualizer.open(this.activity);
        Button xupmix = button(root(), "Xupmix ON");
        Assert.assertTrue("Bold typeface positive control", Typeface.create("sans-serif", Typeface.BOLD).isBold());
        Assert.assertTrue("Xupmix typeface", xupmix.getTypeface().isBold() || xupmix.getPaint().isFakeBoldText());
        Assert.assertNotNull(xupmix.getCompoundDrawablesRelative()[2]);
        button(root(), "Custom").performClick();
        measure(450, 900);
        View minus = description(root(), "1000 Hz decrease 0.1 dB");
        View plus = description(root(), "1000 Hz increase 0.1 dB");
        Assert.assertTrue("Pill gap " + (plus.getLeft() - minus.getRight()), plus.getLeft() - minus.getRight() >= 12);
    }

    @Test
    public void omitBrokenStockRouteFromMoreMenu() {
        FineEqualizer.open(this.activity);
        description(root(), "More options").performClick();
        android.widget.PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        for (int item = 0; item < menu.getMenu().size(); item++) {
            Assert.assertNotEquals("Equalizer (stock)", menu.getMenu().getItem(item).getTitle());
        }
    }

    @Test
    @Config(qualifiers = "w450dp-h900dp-mdpi")
    public void separateFooterFromNavigationAndMatchHostScale() {
        FineEqualizer.navigation("flutter/navigation",
                ByteBuffer.wrap("{\"args\":{\"uri\":\"/device-detail\"}}".getBytes(StandardCharsets.UTF_8)));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        ViewGroup wrapper = (ViewGroup) ((ViewGroup) this.activity.findViewById(android.R.id.content)).getChildAt(0);
        ViewGroup footer = (ViewGroup) wrapper.getChildAt(1);
        wrapper.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder()
            .setInsets(android.view.WindowInsets.Type.systemBars(), android.graphics.Insets.of(0, 24, 0, 30))
            .build());
        wrapper.measure(View.MeasureSpec.makeMeasureSpec(450, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
        wrapper.layout(0, 0, 450, 900);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        wrapper.measure(View.MeasureSpec.makeMeasureSpec(450, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY));
        wrapper.layout(0, 0, 450, 900);
        Assert.assertEquals(42, footer.getPaddingBottom());
        Assert.assertEquals(96, footer.getChildAt(0).getHeight());
    }

    @Test
    public void retainRetryAfterInitialReadFailsOnDefault() {
        SoundState original = ConnectionShadow.current;
        ConnectionShadow.current = null;
        FineEqualizer.open(this.activity);
        TextView retry = text(root(), "Not saved. Tap to retry");
        Assert.assertNotNull(retry);
        Assert.assertEquals(View.VISIBLE, retry.getVisibility());
        Assert.assertTrue(retry.isEnabled());
        ConnectionShadow.current = original;
        retry.performClick();
        Assert.assertEquals(View.GONE, text(root(), "Saved").getVisibility());
        Assert.assertTrue(button(root(), "MOVIE").isEnabled());
    }

    @Test
    @Config(qualifiers = "notnight")
    public void keepSelectedLabelsReadableOnLightSurface() {
        app.hxreborn.extension.ultimea.HostStyle style = new app.hxreborn.extension.ultimea.HostStyle(this.activity);
        Assert.assertFalse(style.dark);
        FineEqualizer.open(this.activity);
        Assert.assertEquals(style.accent, button(root(), "MOVIE").getCurrentTextColor());
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        Assert.assertEquals(style.accent, button(root(), "Untitled").getCurrentTextColor());
        double foreground = android.graphics.Color.luminance(style.accent);
        double background = android.graphics.Color.luminance(style.selectedSurface);
        Assert.assertTrue((Math.max(foreground, background) + .05) / (Math.min(foreground, background) + .05) >= 4.5);
    }

    @Test
    @Config(qualifiers = "night")
    public void retainWhiteSelectedLabelsOnDarkSurface() {
        Assert.assertTrue(new app.hxreborn.extension.ultimea.HostStyle(this.activity).dark);
        FineEqualizer.open(this.activity);
        Assert.assertEquals(android.graphics.Color.WHITE, button(root(), "MOVIE").getCurrentTextColor());
    }

    @Test
    public void openPresetActionsOnLongPressWithAccessibleLabel() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        measure(450, 900);
        Button preset = button(root(), "Untitled");
        Assert.assertEquals("Untitled", preset.getContentDescription());
        android.view.accessibility.AccessibilityNodeInfo info = preset.createAccessibilityNodeInfo();
        boolean editAction = false;
        for (android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction action : info.getActionList()) {
            editAction |= action.getId() == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK
                    && "Edit preset".contentEquals(action.getLabel());
        }
        Assert.assertTrue(editAction);
        Assert.assertTrue(preset.performLongClick());
        android.widget.PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertSame(preset, org.robolectric.util.ReflectionHelpers.getField(menu, "mAnchor"));
        Assert.assertEquals(java.util.Arrays.asList("Rename", "Share", "Reset to Movie", "Delete"), menuLabels(menu));
        menu.getMenu().performIdentifierAction(menu.getMenu().getItem(0).getItemId(), 0);
        Assert.assertEquals("Rename",
                Shadows.shadowOf((android.app.AlertDialog) ShadowDialog.getLatestDialog()).getTitle());
    }

    @Test
    public void selectUnselectedCardBeforeOpeningItsActions() throws Exception {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        description(root(), "New preset from current curve").performClick();
        ConnectionShadow.deferApply = true;
        measure(450, 900);
        button(root(), "Untitled").performLongClick();
        Assert.assertNull(ShadowPopupMenu.getLatestPopupMenu());
        ConnectionShadow.finishApply();
        measure(450, 900);
        android.widget.PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(menu);
        EqualizerPresetStore store = new EqualizerPresetStore(this.activity);
        Assert.assertEquals(store.load().get(0).id, store.current());
        Button anchor = org.robolectric.util.ReflectionHelpers.getField(menu, "mAnchor");
        Assert.assertEquals(store.current(), anchor.getTag());
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(menu.getMenu().getItem(3), "invoke");
        android.app.AlertDialog confirm = (android.app.AlertDialog) ShadowDialog.getLatestDialog();
        confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(1, store.load().size());
        Assert.assertEquals("Untitled 2", store.load().get(0).name);
    }

    @Test
    public void confirmSelectedCardTapAndKeepOnlyImportInScreenMenu() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        int before = ConnectionShadow.applies;
        button(root(), "Untitled").performClick();
        Assert.assertEquals(before + 1, ConnectionShadow.applies);
        Assert.assertEquals("Selected Untitled", org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
        description(root(), "More options").performClick();
        Assert.assertEquals(java.util.Collections.singletonList("Import"),
                menuLabels(ShadowPopupMenu.getLatestPopupMenu()));
    }

    @Test
    public void explainCardTapWhenItsSessionHasClosed() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        java.lang.ref.WeakReference<?> current = org.robolectric.util.ReflectionHelpers
            .getStaticField(FineEqualizer.class, "current");
        EqualizerSession session = org.robolectric.util.ReflectionHelpers.getField(current.get(), "session");
        session.close();
        button(root(), "Untitled").performClick();
        Assert.assertEquals("Equalizer is closed. Open it again",
                org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void customizeModeWithoutCreatingPresetUntilPlusIsTapped() throws Exception {
        FineEqualizer.open(this.activity);
        button(root(), "Customize current mode").performClick();
        Assert.assertTrue(button(root(), "Custom").isSelected());
        EqualizerPresetStore store = new EqualizerPresetStore(this.activity);
        Assert.assertTrue(store.load().isEmpty());
        Assert.assertNull(text(root(), "Untitled"));
        description(root(), "New preset from current curve").performClick();
        Assert.assertEquals(1, store.load().size());
        Assert.assertNotNull(button(root(), "Untitled"));
    }

    @Test
    public void moveSelectedPresetBassWithoutCreatingPresetsOnRemote() throws Exception {
        EqualizerPresetStore store = new EqualizerPresetStore(this.activity);
        SoundState saved = new SoundState(ConnectionShadow.current.curve.asCustom(), 1);
        EqualizerPreset preset = store.add(EqualizerPresetFile.parse(EqualizerPresetFile
            .json(EqualizerPreset.create("Saved movie", saved, "Movie", ConnectionShadow.current))
            .toString()));
        store.setCurrent(preset.id);
        ConnectionShadow.current = saved;
        ViewGroup content = this.activity.findViewById(android.R.id.content);
        ViewGroup wrapper = (ViewGroup) content.getChildAt(0);
        ((ViewGroup) wrapper.getChildAt(1)).getChildAt(0).performClick();
        Assert.assertEquals(1, store.load().size());
        Assert.assertNotNull(
                text(root(), "Bass, Midrange and Treble each move their whole band group by 0.1 dB per press."));
        description(root(), "Bass increase 0.1 dB").performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
        Assert.assertEquals(1, store.load().size());
        EqualizerPreset updated = store.load().get(0);
        Assert.assertEquals(preset.id, updated.id);
        Assert.assertEquals(preset.id, store.current());
        for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
            Assert.assertEquals(saved.curve.gain(band) + ((band <= 3) ? 1 : 0), updated.curve.gain(band));
        }
    }

    @Test
    @Config(sdk = 35, qualifiers = "w411dp-h868dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void keepBandValueOnOneLineAtPhoneWidth() {
        assertBandReadoutFits(1f);
    }

    @Test
    @Config(sdk = 35, qualifiers = "w411dp-h868dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void keepBandValueOnOneLineWithDoubleFont() {
        assertBandReadoutFits(2f);
    }

    private void assertBandReadoutFits(float fontScale) {
        android.content.res.Configuration configuration = new android.content.res.Configuration(
                this.activity.getResources().getConfiguration());
        configuration.fontScale = fontScale;
        this.activity.getResources()
            .updateConfiguration(configuration, this.activity.getResources().getDisplayMetrics());
        int[] gains = new int[10];
        java.util.Arrays.fill(gains, -60);
        ConnectionShadow.current = new SoundState(new EqualizerCurve(EqualizerCurve.CUSTOM_MODE, gains), 1);
        FineEqualizer.open(this.activity);
        measure(411, 868);
        TextView value = text(root(), "1 kHz\n−6.0 dB");
        Assert.assertNotNull(value);
        Assert.assertEquals(2, value.getLineCount());
        Assert.assertEquals("−6.0 dB",
                value.getText()
                    .subSequence(value.getLayout().getLineStart(1), value.getLayout().getLineEnd(1))
                    .toString());
        for (String label : new String[] { "1000 Hz increase 0.1 dB", "1000 Hz decrease 0.1 dB" }) {
            View control = description(root(), label);
            Assert.assertTrue(label, control.getWidth() >= 48);
            Assert.assertTrue(label, control.getHeight() >= 48);
            Assert.assertTrue(label, control.getRight() <= ((View) control.getParent()).getWidth());
        }
    }

    @Test
    @Config(sdk = 35, qualifiers = "w411dp-h868dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void scrollFullCustomizeButtonIntoPhoneViewport() {
        FineEqualizer.open(this.activity);
        root().setPadding(0, 28, 0, 24);
        measure(411, 868);
        ScrollView viewport = scroll(root());
        Assert.assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, viewport.getChildAt(0).getLayoutParams().height);
        Assert.assertTrue(viewport.getChildAt(0).getHeight() > viewport.getHeight());
        viewport.scrollTo(0, viewport.getChildAt(0).getHeight());
        Button customize = button(root(), "Customize current mode");
        android.graphics.Rect visible = new android.graphics.Rect();
        customize.getDrawingRect(visible);
        viewport.offsetDescendantRectToMyCoords(customize, visible);
        Assert.assertTrue(viewport.getScrollY() > 0);
        Assert.assertEquals(customize.getHeight(), visible.height());
        Assert.assertTrue(visible.top >= viewport.getScrollY());
        Assert.assertTrue(visible.bottom <= viewport.getScrollY() + viewport.getHeight());
        Assert.assertTrue(customize.getHeight() >= 48);
    }

    @Test
    public void insetRenameFieldWithinDialogContent() {
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        description(root(), "New preset from current curve").performClick();
        measure(450, 900);
        button(root(), "Untitled").performLongClick();
        android.widget.PopupMenu menu = ShadowPopupMenu.getLatestPopupMenu();
        menu.getMenu().performIdentifierAction(menu.getMenu().getItem(0).getItemId(), 0);
        android.app.AlertDialog dialog = (android.app.AlertDialog) ShadowDialog.getLatestDialog();
        TextView input = text(dialog.getWindow().getDecorView(), "Untitled");
        View content = (View) input.getParent();
        Assert.assertEquals(24, content.getPaddingLeft());
        Assert.assertEquals(24, content.getPaddingRight());
        Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, input.getLayoutParams().width);
    }

    @Test
    public void explainUnselectedMovieCurveOnCustomTab() throws Exception {
        EqualizerPresetStore store = new EqualizerPresetStore(this.activity);
        SoundState custom = ConnectionShadow.current.withCurve(ConnectionShadow.current.curve.asCustom());
        EqualizerPreset preset = store.add(EqualizerPresetFile.parse(
                EqualizerPresetFile.json(EqualizerPreset.create("Untitled", custom, "Movie", ConnectionShadow.current))
                    .toString()));
        store.setCurrent(preset.id);
        FineEqualizer.open(this.activity);
        button(root(), "Custom").performClick();
        Assert.assertFalse(button(root(), "Untitled").isSelected());
        Assert.assertNotNull(text(root(), "No preset selected. Select a preset or tap +."));
        Assert.assertFalse(description(root(), "1000 Hz increase 0.1 dB").isEnabled());
        button(root(), "Untitled").performClick();
        Assert.assertEquals(EqualizerCurve.CUSTOM_MODE, ConnectionShadow.current.curve.mode());
        Assert.assertTrue(button(root(), "Untitled").isSelected());
        Assert.assertTrue(description(root(), "1000 Hz increase 0.1 dB").isEnabled());
    }

    private static List<String> menuLabels(android.widget.PopupMenu menu) {
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < menu.getMenu().size(); index++) {
            labels.add(menu.getMenu().getItem(index).getTitle().toString());
        }
        return labels;
    }

    private void measure(int width, int height) {
        ViewGroup view = root();
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
    }

    private static TextView text(View view, String label) {
        if (view instanceof TextView && !(view instanceof Button) && label.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup parent) {
            for (int child = 0; child < parent.getChildCount(); child++) {
                TextView match = text(parent.getChildAt(child), label);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static ScrollView scroll(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        if (view instanceof ViewGroup parent) {
            for (int child = 0; child < parent.getChildCount(); child++) {
                ScrollView match = scroll(parent.getChildAt(child));
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void collectModes(View view, List<String> names) {
        if (view instanceof Button) {
            String label = ((Button) view).getText().toString();
            if (java.util.Arrays.asList("MOVIE", "MUSIC", "VOICE", "SPORT", "GAME", "NIGHT").contains(label)) {
                names.add(label);
            }
        }
        if (view instanceof ViewGroup parent) {
            for (int child = 0; child < parent.getChildCount(); child++) {
                collectModes(parent.getChildAt(child), names);
            }
        }
    }

    private ViewGroup root() {
        return ShadowDialog.getLatestDialog().findViewById(android.R.id.content);
    }

    private static Button button(View view, String title) {
        if (view instanceof Button && title.equals(((Button) view).getText().toString())) {
            return (Button) view;
        }
        if (view instanceof ViewGroup parent) {
            for (int index = 0; index < parent.getChildCount(); index++) {
                Button match = button(parent.getChildAt(index), title);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static View description(View view, String label) {
        if (label.contentEquals(view.getContentDescription() != null ? view.getContentDescription() : "")) {
            return view;
        }
        if (view instanceof ViewGroup parent) {
            for (int index = 0; index < parent.getChildCount(); index++) {
                View match = description(parent.getChildAt(index), label);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static List<SeekBar> bands(View view) {
        List<SeekBar> bands = new ArrayList<>();
        if (view instanceof SeekBar) {
            bands.add((SeekBar) view);
        }
        if (view instanceof ViewGroup parent) {
            for (int index = 0; index < parent.getChildCount(); index++) {
                bands.addAll(bands(parent.getChildAt(index)));
            }
        }
        return bands;
    }

    @Implements(value = EqualizerBases.class, isInAndroidSdk = false)
    public static class BasesShadow {

        @Implementation
        protected static String definitions() {
            try {
                org.json.JSONArray modes = new org.json.JSONArray();
                String[] names = { "movie", "music", "voice", "sport", "night", "game" };
                for (String name : names) {
                    org.json.JSONArray gains = new org.json.JSONArray();
                    int[] movie = { 3, 4, 2, 1, 0, 1, 1, 0, -2, -2 };
                    for (int band = 0; band < EqualizerCurve.BAND_COUNT; band++) {
                        gains.put("movie".equals(name) ? movie[band] : 0);
                    }
                    modes.put(new org.json.JSONObject().put("name", name).put("db", gains));
                }
                return new org.json.JSONObject().put("default_mode", modes).toString();
            } catch (org.json.JSONException ex) {
                throw new IllegalStateException(ex);
            }
        }

    }

    @Implements(value = EqualizerConnection.class, isInAndroidSdk = false)
    @SuppressWarnings({ "unused", "ClassEscapesDefinedScope" })
    public static class ConnectionShadow {

        private static SoundState current;

        private static int applies;

        private static boolean deferApply;

        private static SoundCallback pendingApply;

        private static SoundState pendingTarget;

        static void finishApply() {
            current = pendingTarget;
            SoundCallback callback = pendingApply;
            pendingApply = null;
            callback.completed(current);
        }

        @Implementation
        protected static Object connection() {
            return ConnectionShadow.class;
        }

        @Implementation
        protected static void readState(SoundCallback callback) {
            callback.completed(current);
        }

        @Implementation
        protected static void applyState(SoundState before, SoundState target, SoundCallback callback) {
            applies++;
            if (deferApply) {
                pendingTarget = target;
                pendingApply = callback;
                return;
            }
            current = target;
            callback.completed(target);
        }

    }

    @Implements(value = app.hxreborn.extension.ultimea.AppliedPatches.class, isInAndroidSdk = false)
    @SuppressWarnings("unused")
    public static class AppliedFlagsShadow {

        @Implementation
        protected static boolean localEqualizerPresets() {
            return true;
        }

    }

}
