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

final class SoundState {

    final EqualizerCurve curve;

    final int xupmix;

    SoundState(EqualizerCurve curve, int xupmix) {
        if (curve == null || xupmix < -1 || xupmix > 1) {
            throw new IllegalArgumentException("Invalid sound state");
        }
        this.curve = curve;
        this.xupmix = xupmix;
    }

    SoundState withCurve(EqualizerCurve curve) {
        return new SoundState(curve, this.xupmix);
    }

    SoundState withXupmix(int xupmix) {
        return new SoundState(this.curve, xupmix);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SoundState)) {
            return false;
        }
        SoundState state = (SoundState) other;
        return this.curve.equals(state.curve) && this.xupmix == state.xupmix;
    }

    @Override
    public int hashCode() {
        return this.curve.hashCode() * 31 + this.xupmix;
    }

}
