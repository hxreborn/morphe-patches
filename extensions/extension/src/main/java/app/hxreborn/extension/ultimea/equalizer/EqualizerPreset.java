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

import java.util.UUID;

final class EqualizerPreset {

    static final int MAX_NAME_LENGTH = 80;

    static final int MAX_MODE = 8;

    final String id;

    final String name;

    final EqualizerCurve curve;

    final SoundState state;

    final String baseName;

    final SoundState base;

    EqualizerPreset(String id, String name, EqualizerCurve curve) {
        this(id, name, new SoundState(curve.asCustom(), -1), "Custom", new SoundState(curve.asCustom(), -1));
    }

    EqualizerPreset(String id, String name, SoundState state, String baseName, SoundState base) {
        UUID.fromString(id);
        if (state.curve.mode() > MAX_MODE || base.curve.mode() > MAX_MODE) {
            throw new IllegalArgumentException("Unsupported equalizer preset mode");
        }
        this.id = id;
        this.name = validName(name);
        this.state = state;
        this.curve = this.state.curve;
        this.baseName = validName(baseName);
        this.base = base;
    }

    static EqualizerPreset create(String name, EqualizerCurve curve) {
        return new EqualizerPreset(UUID.randomUUID().toString(), name, curve);
    }

    static EqualizerPreset create(String name, SoundState state, String baseName, SoundState base) {
        return new EqualizerPreset(UUID.randomUUID().toString(), name, state, baseName, base);
    }

    static String validName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Use a name between 1 and 80 characters");
        }
        for (int index = 0; index < trimmed.length(); index++) {
            if (Character.isISOControl(trimmed.charAt(index))) {
                throw new IllegalArgumentException("Preset names cannot contain control characters");
            }
        }
        return trimmed;
    }

}
