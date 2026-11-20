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
package app.hxreborn.extension.bereal;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.O)
@SuppressWarnings("unused")
public final class LateFlag {

    private static final long LATE_GRACE_SECONDS = 60;

    private static final long ON_TIME_MIN_DELAY_SECONDS = 60;

    private static final long ON_TIME_MAX_DELAY_SECONDS = 105;

    private static volatile MomentWindow window;

    private LateFlag() {
    }

    public static boolean isPatchIncluded() {
        return false;
    }

    static Object creationDate(Object moment) {
        return null;
    }

    static Object expirationDate(Object moment) {
        return null;
    }

    static Instant instantValue(Object timestamp) {
        return null;
    }

    static Object timestampOf(Instant value) {
        return null;
    }

    public static void onMoment(Object moment) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (moment == null) {
            window = null;
            return;
        }
        final Instant start = instantOf(creationDate(moment));
        final Instant end = instantOf(expirationDate(moment));
        window = (start != null && end != null) ? new MomentWindow(start, end) : null;
    }

    public static Object takenAt(Object original, boolean isMain) {
        if (original == null || !isMain || Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || !PatchSettings.REMOVE_LATE_FLAG.get()) {
            return original;
        }
        final MomentWindow current = window;
        if (current == null) {
            return original;
        }
        final Instant takenAt = instantValue(original);
        if (!takenAt.isAfter(current.end.plusSeconds(LATE_GRACE_SECONDS))) {
            return original;
        }
        final long delay = ThreadLocalRandom.current()
            .nextLong(ON_TIME_MIN_DELAY_SECONDS, ON_TIME_MAX_DELAY_SECONDS + 1);
        Instant onTime = current.start.plusSeconds(delay);
        if (onTime.isAfter(current.end)) {
            onTime = current.end;
        }
        return timestampOf(onTime);
    }

    private static Instant instantOf(Object timestamp) {
        return (timestamp != null) ? instantValue(timestamp) : null;
    }

    public static String patchName() {
        return null;
    }

    public static String patchDescription() {
        return null;
    }

    private static final class MomentWindow {

        private final Instant start;

        private final Instant end;

        private MomentWindow(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

    }

}
