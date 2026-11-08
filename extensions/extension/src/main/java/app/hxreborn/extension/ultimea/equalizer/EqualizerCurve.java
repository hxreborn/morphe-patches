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
package app.hxreborn.extension.ultimea.equalizer;

import java.util.Arrays;

final class EqualizerCurve {

    static final int BAND_COUNT = 10;

    static final int CUSTOM_MODE = 7;

    static final int MIN_GAIN_TENTHS = -60;

    static final int MAX_GAIN_TENTHS = 60;

    private static final int[] FREQUENCIES = { 31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000 };

    private static final int BAND_BYTES = 4;

    private static final int PAYLOAD_BYTES = 1 + BAND_COUNT * BAND_BYTES;

    private final int mode;

    private final int[] gains;

    EqualizerCurve(int mode, int[] gains) {
        if (mode < 1 || mode > 9 || gains.length != BAND_COUNT) {
            throw new IllegalArgumentException("Invalid equalizer curve");
        }
        for (int gain : gains) {
            if (gain < MIN_GAIN_TENTHS || gain > MAX_GAIN_TENTHS) {
                throw new IllegalArgumentException("Equalizer gain exceeds slider range");
            }
        }
        this.mode = mode;
        this.gains = gains.clone();
    }

    static EqualizerCurve fromPayload(byte[] payload) {
        if (payload.length != PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Expected full equalizer curve");
        }
        int[] gains = new int[BAND_COUNT];
        for (int band = 0; band < BAND_COUNT; band++) {
            int offset = 1 + band * BAND_BYTES;
            int frequency = (payload[offset] & 255) | ((payload[offset + 1] & 255) << 8);
            if (frequency != FREQUENCIES[band]) {
                throw new IllegalArgumentException("Unexpected equalizer frequencies");
            }
            gains[band] = (short) ((payload[offset + 2] & 255) | ((payload[offset + 3] & 255) << 8));
        }
        return new EqualizerCurve(payload[0] & 255, gains);
    }

    byte[] payload() {
        byte[] payload = new byte[PAYLOAD_BYTES];
        payload[0] = (byte) this.mode;
        for (int band = 0; band < BAND_COUNT; band++) {
            int offset = 1 + band * BAND_BYTES;
            payload[offset] = (byte) FREQUENCIES[band];
            payload[offset + 1] = (byte) (FREQUENCIES[band] >> 8);
            payload[offset + 2] = (byte) this.gains[band];
            payload[offset + 3] = (byte) (this.gains[band] >> 8);
        }
        return payload;
    }

    EqualizerCurve withGain(int band, int gain) {
        int[] changed = this.gains.clone();
        changed[band] = gain;
        return new EqualizerCurve(CUSTOM_MODE, changed);
    }

    int mode() {
        return this.mode;
    }

    int gain(int band) {
        return this.gains[band];
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EqualizerCurve)) {
            return false;
        }
        EqualizerCurve curve = (EqualizerCurve) other;
        return this.mode == curve.mode && Arrays.equals(this.gains, curve.gains);
    }

    @Override
    public int hashCode() {
        return 31 * this.mode + Arrays.hashCode(this.gains);
    }

}
