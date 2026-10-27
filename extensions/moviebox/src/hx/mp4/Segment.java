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
package hx.mp4;

public final class Segment {

    public final String url;

    public final long startTime;

    public final long duration;

    public long byteLength;

    public int prefixBytes;

    public int trackIdOffset;

    public Segment(String url, long startTime, long duration) {
        this.url = url;
        this.startTime = startTime;
        this.duration = duration;
    }

    public long payloadBytes() {
        return this.byteLength - this.prefixBytes;
    }

}
