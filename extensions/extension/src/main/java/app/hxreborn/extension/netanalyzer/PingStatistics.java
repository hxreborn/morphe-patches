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
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PingStatistics {

    private static final int NONE = -1;

    private static final double HALF = 2.0;

    final List<Double> bars;

    final int minIndex;

    final int maxIndex;

    final double min;

    final double max;

    final double median;

    final int received;

    final int lost;

    final double lostFraction;

    private PingStatistics(List<Double> bars, int minIndex, int maxIndex, double min, double max, double median,
            int received, int lost, double lostFraction) {
        this.bars = Collections.unmodifiableList(bars);
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.min = min;
        this.max = max;
        this.median = median;
        this.received = received;
        this.lost = lost;
        this.lostFraction = lostFraction;
    }

    static PingStatistics of(List<Probe> probes, boolean active) {
        final List<Double> bars = new ArrayList<>();
        final List<Double> samples = new ArrayList<>();
        int minIndex = NONE;
        int maxIndex = NONE;
        int lost = 0;
        int counted = 0;
        for (int i = 0; i < probes.size(); i++) {
            final Probe probe = probes.get(i);
            final boolean pendingLast = !probe.pingable && active && i == probes.size() - 1;
            if (!pendingLast) {
                bars.add(probe.delay);
            }
            if (probe.inFlight || pendingLast) {
                continue;
            }
            counted++;
            if (!probe.pingable) {
                lost++;
                continue;
            }
            samples.add(probe.delay);
            final int barIndex = bars.size() - 1;
            if (minIndex == NONE || probe.delay < bars.get(minIndex)) {
                minIndex = barIndex;
            }
            if (maxIndex == NONE || probe.delay > bars.get(maxIndex)) {
                maxIndex = barIndex;
            }
        }
        final double min = (minIndex == NONE) ? 0.0 : bars.get(minIndex);
        final double max = (maxIndex == NONE) ? 0.0 : bars.get(maxIndex);
        return new PingStatistics(bars, minIndex, maxIndex, min, max, median(samples), samples.size(), lost,
                (counted == 0) ? 0.0 : lost / (double) counted);
    }

    private static double median(List<Double> samples) {
        if (samples.isEmpty()) {
            return 0.0;
        }
        final List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        final int middle = sorted.size() / 2;
        return (sorted.size() % 2 == 1) ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / HALF;
    }

}
