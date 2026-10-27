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

import hx.mp4.Mp4Boxes;
import hx.mp4.Segment;

final class Piece {

    private static final int TRACK_ID_BYTES = Mp4Boxes.U32_BYTES;

    final long offset;

    final long length;

    final String url;

    final int prefixBytes;

    private final int trackIdOffset;

    private final int trackId;

    Piece(long offset, Segment segment, int trackId) {
        this.offset = offset;
        this.length = segment.payloadBytes();
        this.url = segment.url;
        this.prefixBytes = segment.prefixBytes;
        this.trackIdOffset = segment.trackIdOffset - segment.prefixBytes;
        this.trackId = trackId;
    }

    void patchTrackId(byte[] bytes, long from) {
        for (int k = 0; k < TRACK_ID_BYTES; k++) {
            long index = this.trackIdOffset + k - from;
            if (index >= 0 && index < bytes.length) {
                bytes[(int) index] = (byte) (this.trackId >>> (Byte.SIZE * (TRACK_ID_BYTES - 1 - k)));
            }
        }
    }

}
