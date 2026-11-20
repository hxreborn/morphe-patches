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
package hx.dash;

import java.util.Locale;

import android.util.Log;
import hx.util.Logs;

final class TransferLog {

    static final TransferLog SHARED = new TransferLog();

    private static final long REPORT_INTERVAL_MS = 5000L;

    private static final long NANOS_PER_MS = 1000000L;

    private static final long MS_PER_SECOND = 1000L;

    private static final int BYTES_PER_KIB = 1024;

    private static final double BYTES_PER_MB = 1e6;

    private long windowStart = System.nanoTime();

    private long served;

    private long reads;

    private long readBytes;

    private long headerNanos;

    private long bodyNanos;

    private long retries;

    private int activeRanges;

    synchronized void rangeStarted() {
        if (this.activeRanges++ == 0) {
            restart(System.nanoTime());
        }
    }

    synchronized int rangeEnded() {
        return --this.activeRanges;
    }

    synchronized void read(long bytes, long headerNanos, long bodyNanos) {
        this.reads++;
        this.readBytes += bytes;
        this.headerNanos += headerNanos;
        this.bodyNanos += bodyNanos;
    }

    synchronized void retry() {
        this.retries++;
    }

    synchronized void served(long bytes) {
        this.served += bytes;
        long now = System.nanoTime();
        if (now - this.windowStart >= REPORT_INTERVAL_MS * NANOS_PER_MS) {
            report(now);
            restart(now);
        }
    }

    private void report(long now) {
        double seconds = (now - this.windowStart) / (double) (MS_PER_SECOND * NANOS_PER_MS);
        Log.i(Logs.TAG,
                String.format(Locale.ROOT,
                        "DASH served %.1f MB in %.1f s = %.1f MB/s over %d ranges, %d CDN reads of %d KiB avg, "
                                + "headers %d ms avg, body %d ms avg, %d retries",
                        this.served / BYTES_PER_MB, seconds, this.served / BYTES_PER_MB / seconds, this.activeRanges,
                        this.reads, average(this.readBytes) / BYTES_PER_KIB, average(this.headerNanos) / NANOS_PER_MS,
                        average(this.bodyNanos) / NANOS_PER_MS, this.retries));
    }

    private long average(long total) {
        return (this.reads == 0) ? 0 : total / this.reads;
    }

    private void restart(long now) {
        this.windowStart = now;
        this.served = 0;
        this.reads = 0;
        this.readBytes = 0;
        this.headerNanos = 0;
        this.bodyNanos = 0;
        this.retries = 0;
    }

}
