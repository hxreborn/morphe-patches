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

import android.app.Notification;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class SpeechNotificationTest {

    private static final int TIMER_MINUTES = 15;

    private static final long ELAPSED_MS = 120000;

    private static final long REMAINING_MS = 780000;

    @Test
    public void includePreviousPlayNextInCompactView() {
        Assert.assertArrayEquals(new int[] { 0, 1, 2 }, SpeechNotification.compactActionIndices());
    }

    @Test
    public void subtractElapsedTime() {
        Assert.assertEquals(REMAINING_MS, SpeechNotification.remainingMillisAfterElapsed(TIMER_MINUTES, ELAPSED_MS));
    }

    @Test
    public void stopCountdownAfterDeadline() {
        Assert.assertEquals(0, SpeechNotification.remainingMillisAfterElapsed(1, ELAPSED_MS));
    }

    @Test
    public void omitCountdownWhenTimerIsOff() {
        Assert.assertEquals(0, SpeechNotification.remainingMillisAfterElapsed(0, ELAPSED_MS));
    }

    @Test
    public void preserveMediaSessionTokenWhileCountingDown() {
        MediaSession session = new MediaSession(RuntimeEnvironment.getApplication(), "session fixture");
        Notification original = new Notification.Builder(RuntimeEnvironment.getApplication())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_previous, "Previous", null)
            .addAction(android.R.drawable.ic_media_pause, "Pause", null)
            .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()))
            .build();
        Notification configured = SpeechNotification.addSleepTimerCountdown(original,
                RuntimeEnvironment.getApplication(), REMAINING_MS);
        Assert.assertTrue(configured.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        Assert.assertEquals(session.getSessionToken(),
                configured.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION));
        session.release();
    }

    @Test
    public void countDownWhilePlaying() {
        Notification original = new Notification.Builder(RuntimeEnvironment.getApplication())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_previous, "Previous", null)
            .addAction(android.R.drawable.ic_media_pause, "Pause", null)
            .build();
        Notification configured = SpeechNotification.addSleepTimerCountdown(original,
                RuntimeEnvironment.getApplication(), REMAINING_MS);
        Assert.assertTrue(configured.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        Assert.assertTrue(configured.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN));
        Assert.assertTrue(configured.extras.getBoolean(Notification.EXTRA_SHOW_WHEN));
    }

    @Test
    public void freezeTimerWhilePaused() {
        Notification original = new Notification.Builder(RuntimeEnvironment.getApplication())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_previous, "Previous", null)
            .addAction(android.R.drawable.ic_media_play, "Play", null)
            .build();
        Notification configured = SpeechNotification.addSleepTimerCountdown(original,
                RuntimeEnvironment.getApplication(), REMAINING_MS);
        Assert.assertFalse(configured.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
        Assert.assertFalse(configured.extras.getBoolean(Notification.EXTRA_SHOW_WHEN));
        Assert.assertEquals("Sleep timer 13:00", configured.extras.getString(Notification.EXTRA_SUB_TEXT));
    }

    @Test
    @Config(sdk = 33)
    public void addStopToPlayer() {
        PlaybackState original = new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, ELAPSED_MS, 1)
            .setActions(PlaybackState.ACTION_PAUSE)
            .addCustomAction("original", "Original", android.R.drawable.ic_media_play)
            .build();
        PlaybackState configured = SpeechNotification.addStopAction(original);
        Assert.assertEquals(original.getActions(), configured.getActions());
        Assert.assertEquals(original.getState(), configured.getState());
        Assert.assertEquals(original.getPosition(), configured.getPosition());
        Assert.assertEquals("original", configured.getCustomActions().get(0).getAction());
        PlaybackState.CustomAction stop = configured.getCustomActions().get(1);
        Assert.assertEquals("Stop", stop.getName());
        Assert.assertTrue(SpeechNotification.isStopAction(stop.getAction()));
    }

    @Test
    public void preservePlaybackStateBeforeAndroid13() {
        PlaybackState original = new PlaybackState.Builder().build();
        Assert.assertSame(original, SpeechNotification.addStopAction(original));
    }

    @Test
    public void preserveAbsentStateAndOtherCustomActions() {
        Assert.assertNull(SpeechNotification.addStopAction(null));
        Assert.assertFalse(SpeechNotification.isStopAction(null));
        Assert.assertFalse(SpeechNotification.isStopAction("original"));
    }

    @Test
    @Config(sdk = 30)
    public void preserveNotificationInSystemPlayer() {
        Notification notification = new Notification();
        notification.extras = new Bundle();
        notification.extras.putString(Notification.EXTRA_MEDIA_SESSION, "session fixture");
        Assert.assertSame(notification,
                SpeechNotification.addSleepTimerCountdown(notification, RuntimeEnvironment.getApplication()));
        Assert.assertEquals("session fixture", notification.extras.getString(Notification.EXTRA_MEDIA_SESSION));
        Assert.assertFalse(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER));
    }

    @Test
    @Config(sdk = 29)
    public void showCountdownInNotificationTemplate() {
        Assert.assertTrue(SpeechNotification.showsCountdown());
    }

    @Test
    @Config(sdk = 30)
    public void omitCountdownInSystemPlayer() {
        Assert.assertFalse(SpeechNotification.showsCountdown());
    }

    @Test
    @Config(sdk = 23)
    public void preserveNotificationBeforeCountdownApi() {
        Notification notification = new Notification();
        Assert.assertSame(notification,
                SpeechNotification.addSleepTimerCountdown(notification, RuntimeEnvironment.getApplication()));
    }

}
