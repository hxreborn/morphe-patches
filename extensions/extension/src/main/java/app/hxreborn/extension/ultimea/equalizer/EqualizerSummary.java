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

import android.content.Context;
import org.json.JSONException;

public final class EqualizerSummary {

    private static EqualizerCurve curve;

    private static int xupmix = -1;

    private EqualizerSummary() {
    }

    static synchronized void curve(EqualizerCurve value) {
        curve = value;
    }

    static synchronized void xupmix(int value) {
        xupmix = value;
    }

    static synchronized void clear() {
        curve = null;
        xupmix = -1;
    }

    public static synchronized String current(Context context) {
        if (curve == null) {
            return null;
        }
        EqualizerBases.Entry entry = EqualizerBases.mode(curve.mode());
        String mode;
        if (entry != null) {
            mode = entry.name;
        } else if (curve.mode() == EqualizerCurve.CUSTOM_MODE) {
            mode = "Custom";
        } else if (curve.mode() == EqualizerPreset.MAX_MODE) {
            mode = "Style";
        } else {
            return null;
        }
        EqualizerPresetStore store = new EqualizerPresetStore(context);
        String current = store.current();
        try {
            for (EqualizerPreset preset : store.load()) {
                if (preset.id.equals(current) && preset.state.curve.equals(curve)
                        && (preset.state.xupmix < 0 || preset.state.xupmix == xupmix)) {
                    return mode + ": " + preset.name;
                }
            }
        } catch (JSONException ex) {
            return mode;
        }
        return mode;
    }

}
