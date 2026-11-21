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

import java.time.Duration;

import android.content.Context;
import android.os.Looper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = FineEqualizerTest.BasesShadow.class)
@SuppressWarnings("DataFlowIssue")
public class EqualizerWritesTest {

    private EqualizerSession session;

    private EqualizerWrites writes;

    private SoundState sound;

    private int applies;

    @Before
    public void readSound() {
        Context context = RuntimeEnvironment.getApplication();
        this.sound = new SoundState(new EqualizerCurve(7, new int[10]), 1);
        this.session = new EqualizerSession(context, false, (state) -> {
        }, new EqualizerSessionConnection() {
            @Override
            public void read(SoundCallback callback) {
                callback.completed(EqualizerWritesTest.this.sound);
            }

            @Override
            public void apply(SoundState before, SoundState target, SoundCallback callback) {
                EqualizerWritesTest.this.applies++;
                EqualizerWritesTest.this.sound = target;
                callback.completed(target);
            }
        });
        this.writes = new EqualizerWrites(this.session, () -> {
        });
        this.session.read();
    }

    @After
    public void closeWrites() {
        this.writes.close();
        this.session.close();
    }

    @Test
    public void sendOneCurveWhenDragEnds() {
        this.writes.begin();
        this.writes.setGain(5, 1);
        this.writes.setGain(5, 2);
        this.writes.setGain(5, 3);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Assert.assertEquals(0, this.applies);
        Assert.assertEquals(3, this.writes.curve().gain(5));
        this.writes.release();
        Assert.assertEquals(1, this.applies);
        Assert.assertEquals(3, this.sound.curve.gain(5));
    }

    @Test
    public void preservePreviewWhenDisconnectPausesWrites() {
        this.writes.step(5, 1);
        this.writes.pause();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        Assert.assertEquals(0, this.applies);
        Assert.assertEquals(1, this.writes.curve().gain(5));
        this.writes.flush();
        Assert.assertEquals(1, this.applies);
        Assert.assertEquals(1, this.sound.curve.gain(5));
    }

    @Test
    public void retainDisconnectedPreviewUntilRetryAcceptsIt() {
        this.writes.step(5, 1);
        this.writes.pause();
        this.session.disconnected();
        this.writes.flush();
        Assert.assertTrue(this.writes.pending());
        Assert.assertEquals(1, this.writes.curve().gain(5));
        Assert.assertEquals(0, this.applies);
        this.session.retry();
        this.writes.flush();
        Assert.assertFalse(this.writes.pending());
        Assert.assertEquals(1, this.sound.curve.gain(5));
        Assert.assertEquals(1, this.applies);
    }

    @Test
    public void undoConfirmedCurvesInOrder() {
        for (int step = 0; step < 3; step++) {
            this.writes.step(5, 1);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
        }
        for (int gain = 2; gain >= 0; gain--) {
            this.session.undo();
            Assert.assertEquals(gain, this.sound.curve.gain(5));
        }
        Assert.assertFalse(this.session.state().canUndo);
    }

    @Test
    public void formatSignedDecimalValues() {
        Assert.assertEquals("0.0", EqualizerGraph.gainLabel(0));
        Assert.assertEquals("+0.1", EqualizerGraph.gainLabel(1));
        Assert.assertEquals("−1.2", EqualizerGraph.gainLabel(-12));
    }

}
