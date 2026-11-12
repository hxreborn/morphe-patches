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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.media.session.PlaybackState;
import android.os.Build;

@SuppressWarnings("unused")
public final class SpeechNotification {

    private static final String STOP_ACTION = "app.hxreborn.readera.STOP";

    private static final int PREVIOUS_ACTION = 0;

    private static final int PLAY_PAUSE_ACTION = 1;

    private static final int NEXT_ACTION = 2;

    private static final int SECONDS_PER_MINUTE = 60;

    private SpeechNotification() {
    }

    public static int[] compactActionIndices() {
        return new int[] { PREVIOUS_ACTION, PLAY_PAUSE_ACTION, NEXT_ACTION };
    }

    public static boolean showsCountdown() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT < Build.VERSION_CODES.R;
    }

    public static Notification addSleepTimerCountdown(Notification notification, Context context) {
        if (!showsCountdown()) {
            return notification;
        }
        return addSleepTimerCountdown(notification, context, sleepTimerRemainingMillis());
    }

    @TargetApi(Build.VERSION_CODES.N)
    static Notification addSleepTimerCountdown(Notification notification, Context context, long remainingMillis) {
        if (remainingMillis <= 0) {
            return notification;
        }
        boolean playing = notification.actions != null && notification.actions.length > PLAY_PAUSE_ACTION
                && notification.actions[PLAY_PAUSE_ACTION].icon == android.R.drawable.ic_media_pause;
        Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification);
        builder.setWhen(System.currentTimeMillis() + remainingMillis)
            .setShowWhen(playing)
            .setUsesChronometer(playing)
            .setChronometerCountDown(true);
        if (!playing) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis);
            builder.setSubText(String.format(Locale.getDefault(), "Sleep timer %d:%02d", seconds / SECONDS_PER_MINUTE,
                    seconds % SECONDS_PER_MINUTE));
        }
        return builder.build();
    }

    public static PlaybackState addStopAction(PlaybackState state) {
        if (state == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return state;
        }
        return new PlaybackState.Builder(state)
            .addCustomAction(STOP_ACTION, "Stop", android.R.drawable.ic_menu_close_clear_cancel)
            .build();
    }

    public static boolean isStopAction(String action) {
        return STOP_ACTION.equals(action);
    }

    static long sleepTimerRemainingMillis() {
        return 0;
    }

    public static long remainingMillisAfterElapsed(int minutes, long elapsedMillis) {
        return Math.max(0, TimeUnit.MINUTES.toMillis(minutes) - elapsedMillis);
    }

}
