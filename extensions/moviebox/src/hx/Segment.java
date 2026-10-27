/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

final class Segment {

    final String url;

    final long startTime;

    final long duration;

    long byteLength;

    int prefixBytes;

    int trackIdOffset;

    Segment(String url, long startTime, long duration) {
        this.url = url;
        this.startTime = startTime;
        this.duration = duration;
    }

    long payloadBytes() {
        return this.byteLength - this.prefixBytes;
    }

}
