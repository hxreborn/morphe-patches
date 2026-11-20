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

final class EqualizerHistory {

    private SoundState confirmed;

    private SoundState undo;

    private SoundState requested;

    EqualizerHistory(SoundState initial) {
        this.confirmed = initial;
    }

    SoundState current() {
        return this.confirmed;
    }

    boolean canUndo() {
        return this.undo != null && this.requested == null;
    }

    void request(SoundState curve) {
        if (this.requested != null) {
            throw new IllegalStateException("Equalizer change is still pending");
        }
        this.requested = curve;
    }

    SoundState undoTarget() {
        if (!canUndo()) {
            throw new IllegalStateException("No confirmed equalizer change to undo");
        }
        return this.undo;
    }

    boolean confirm(SoundState readback) {
        if (this.requested == null || !this.requested.equals(readback)) {
            return false;
        }
        if (!this.confirmed.equals(readback)) {
            this.undo = this.confirmed;
        }
        this.confirmed = readback;
        this.requested = null;
        return true;
    }

    void failed() {
        this.requested = null;
    }

}
