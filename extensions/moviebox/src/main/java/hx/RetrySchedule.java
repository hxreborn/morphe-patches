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
package hx;

final class RetrySchedule {

    static final long STOP = -1L;

    private final int fastAttempts;

    private final long fastDelayMs;

    private final int slowAttempts;

    private final long slowDelayMs;

    RetrySchedule(int fastAttempts, long fastDelayMs, int slowAttempts, long slowDelayMs) {
        this.fastAttempts = fastAttempts;
        this.fastDelayMs = fastDelayMs;
        this.slowAttempts = slowAttempts;
        this.slowDelayMs = slowDelayMs;
    }

    long delayAfter(int failedAttempts) {
        if (failedAttempts < this.fastAttempts) {
            return this.fastDelayMs;
        }
        return (failedAttempts < this.fastAttempts + this.slowAttempts) ? this.slowDelayMs : STOP;
    }

    boolean reportsAfter(int failedAttempts) {
        return failedAttempts == this.fastAttempts;
    }

}
