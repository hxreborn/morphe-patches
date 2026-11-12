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
package app.hxreborn.extension.readera;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.Voice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.Locale;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public final class SpeechPitchSliderTest {

    private static final String FIRST_VOICE = "first-voice";

    private static final String SECOND_VOICE = "second-voice";

    private SharedPreferences preferences;

    private LinearLayout root;

    private SeekBar seekBar;

    private TextView label;

    @Before
    public void prepareSlider() {
        Context context = RuntimeEnvironment.getApplication();
        this.preferences = context.getSharedPreferences("hx_readera_speech", Context.MODE_PRIVATE);
        this.preferences.edit().clear().commit();
        this.root = new LinearLayout(context);
        this.root.setTag("hx_readera_pitch");
        this.seekBar = new SeekBar(context);
        this.seekBar.setId(android.R.id.progress);
        this.seekBar.setMax(150);
        this.label = new TextView(context);
        this.label.setId(android.R.id.text2);
        this.root.addView(this.seekBar);
        this.root.addView(this.label);
    }

    @After
    public void clearPreferences() {
        this.preferences.edit().clear().commit();
    }

    @Test
    public void useNormalPitchWithoutSavedValue() {
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(50, this.seekBar.getProgress());
        Assert.assertEquals("1.0x", this.label.getText().toString());
    }

    @Test
    public void savePitchForSelectedVoice() {
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        changeProgress(83);
        Assert.assertEquals(1.3f, this.preferences.getFloat("pitch:" + FIRST_VOICE, 0), 0);
        Assert.assertEquals("1.3x", this.label.getText().toString());
        SpeechPitchSlider.attach(this.root, () -> voice(SECOND_VOICE));
        Assert.assertEquals(50, this.seekBar.getProgress());
        changeProgress(150);
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(80, this.seekBar.getProgress());
        Assert.assertEquals(2.0f, this.preferences.getFloat("pitch:" + SECOND_VOICE, 0), 0);
    }

    @Test
    public void saveToCurrentVoiceAfterSelectionChangesWithoutAttachment() {
        Voice[] selected = { voice(FIRST_VOICE) };
        this.preferences.edit().putFloat("pitch:" + FIRST_VOICE, 1.3f).putFloat("pitch:" + SECOND_VOICE, 0.7f).commit();
        SpeechPitchSlider.attach(this.root, () -> selected[0]);
        selected[0] = voice(SECOND_VOICE);
        changeProgress(80);
        Assert.assertEquals(1.3f, this.preferences.getFloat("pitch:" + FIRST_VOICE, 0), 0);
        Assert.assertEquals(1.3f, this.preferences.getFloat("pitch:" + SECOND_VOICE, 0), 0);
        Assert.assertEquals("1.3x", this.label.getText().toString());
    }

    @Test
    public void useDefaultPitchForUnconfiguredVoice() {
        SpeechPitchSlider.attach(this.root, () -> null);
        changeProgress(0);
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(0, this.seekBar.getProgress());
        Assert.assertEquals("0.5x", this.label.getText().toString());
    }

    @Test
    public void replaceInvalidSavedPitchWithNormalPitch() {
        this.preferences.edit().putFloat("pitch:" + FIRST_VOICE, Float.NaN).commit();
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(50, this.seekBar.getProgress());
    }

    @Test
    public void clampSavedPitchToSliderRange() {
        this.preferences.edit().putFloat("pitch:" + FIRST_VOICE, 4.0f).commit();
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(150, this.seekBar.getProgress());
    }

    @Test
    public void refreshExistingRowWithoutSavingProgrammaticProgress() {
        this.preferences.edit().putFloat("pitch:" + FIRST_VOICE, 1.5f).commit();
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        this.seekBar.setProgress(0);
        SpeechPitchSlider.attach(this.root, () -> voice(FIRST_VOICE));
        Assert.assertEquals(100, this.seekBar.getProgress());
        Assert.assertEquals(2, this.root.getChildCount());
        Assert.assertEquals(1.5f, this.preferences.getFloat("pitch:" + FIRST_VOICE, 0), 0);
    }

    @Test
    public void placePitchImmediatelyAfterRateAndPreserveLayout() {
        LinearLayout parent = new LinearLayout(this.root.getContext());
        View rate = new View(this.root.getContext());
        rate.setId(0);
        View timer = new View(this.root.getContext());
        parent.addView(rate);
        parent.addView(timer);
        parent.addView(this.root);
        ViewGroup.LayoutParams parameters = this.root.getLayoutParams();
        SpeechPitchSlider.attach(parent, () -> voice(FIRST_VOICE));
        Assert.assertSame(rate, parent.getChildAt(0));
        Assert.assertSame(this.root, parent.getChildAt(1));
        Assert.assertSame(timer, parent.getChildAt(2));
        Assert.assertSame(parameters, this.root.getLayoutParams());
        SpeechPitchSlider.attach(parent, () -> voice(FIRST_VOICE));
        Assert.assertEquals(3, parent.getChildCount());
        Assert.assertSame(this.root, parent.getChildAt(1));
    }

    private void changeProgress(int progress) {
        SeekBar.OnSeekBarChangeListener listener = ReflectionHelpers.getField(this.seekBar, "mOnSeekBarChangeListener");
        listener.onProgressChanged(this.seekBar, progress, true);
    }

    private static Voice voice(String name) {
        return new Voice(name, Locale.ENGLISH, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL, false,
                Collections.emptySet());
    }

}
