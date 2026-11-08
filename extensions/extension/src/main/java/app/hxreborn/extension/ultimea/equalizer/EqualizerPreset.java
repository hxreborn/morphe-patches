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

    final String id;

    final String name;

    final EqualizerCurve curve;

    EqualizerPreset(String id, String name, EqualizerCurve curve) {
        UUID.fromString(id);
        this.id = id;
        this.name = validName(name);
        this.curve = curve.asCustom();
    }

    static EqualizerPreset create(String name, EqualizerCurve curve) {
        return new EqualizerPreset(UUID.randomUUID().toString(), name, curve);
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
