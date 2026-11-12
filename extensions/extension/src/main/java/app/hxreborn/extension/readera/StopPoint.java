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

final class StopPoint {

    private final int firstGeneration;

    private final int lastGeneration;

    private final Object boundary;

    private final Object utterance;

    private final int page;

    StopPoint(int generation, Object boundary, Object utterance, int page) {
        this(generation, generation, boundary, utterance, page);
    }

    private StopPoint(int firstGeneration, int lastGeneration, Object boundary, Object utterance, int page) {
        this.firstGeneration = firstGeneration;
        this.lastGeneration = lastGeneration;
        this.boundary = boundary;
        this.utterance = utterance;
        this.page = page;
    }

    int firstGeneration() {
        return this.firstGeneration;
    }

    int lastGeneration() {
        return this.lastGeneration;
    }

    boolean coversGeneration(int generation) {
        return generation >= this.firstGeneration && generation <= this.lastGeneration;
    }

    Object boundary() {
        return this.boundary;
    }

    StopPoint withRestartGeneration(int generation) {
        return new StopPoint(this.firstGeneration, generation, this.boundary, this.utterance, this.page);
    }

    StopPoint withUtterance(Object utterance) {
        return new StopPoint(this.firstGeneration, this.lastGeneration, this.boundary, utterance, this.page);
    }

    boolean isReachedBy(Object position) {
        if (!(position instanceof ReadingPosition)) {
            return false;
        }
        ReadingPosition reading = (ReadingPosition) position;
        if (this.utterance instanceof ReadingPosition) {
            ReadingPosition end = (ReadingPosition) this.utterance;
            return this.utterance.equals(position) || (reading.readingLayoutHash() == end.readingLayoutHash()
                    && reading.readingRatio() > end.readingRatio());
        }
        return reading.readingPage() >= this.page;
    }

}
