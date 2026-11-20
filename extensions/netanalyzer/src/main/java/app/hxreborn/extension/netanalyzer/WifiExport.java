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
package app.hxreborn.extension.netanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

@SuppressWarnings("unused")
public final class WifiExport extends TableExport {

    private static final String TAG = "hxNetAnalyzer";

    private static final String[] HEADER = { "SSID", "BSSID", "Vendor", "Band", "Primary Channel",
            "Primary Channel Frequency", "Center Channel", "Center Channel Frequency", "Channel Width",
            "Signal Strength", "Encryption", "Standard", "WPS", "Connected" };

    private static final String[] BANDS = { "2.4 GHz", "5 GHz", "6 GHz" };

    private static final String REGISTRATION_SEPARATOR = ";";

    private static final String CLASS_SEPARATOR = ",";

    private static final String MEGAHERTZ = " MHz";

    private static final String DECIBEL_MILLIWATTS = " dBm";

    private static final String YES = "Yes";

    private static final String NO = "No";

    private static final Comparator<Hotspot> STRONGEST_FIRST = (left, right) -> Integer.compare(right.level,
            left.level);

    private static Class<?> hotspotClass;

    private static final List<Class<?>> HOLDERS = new ArrayList<>();

    public static void register(String registration) {
        final String[] parts = registration.split(REGISTRATION_SEPARATOR);
        final ClassLoader loader = WifiExport.class.getClassLoader();
        try {
            hotspotClass = Class.forName(parts[0], false, loader);
            HOLDERS.clear();
            for (String holder : parts[1].split(CLASS_SEPARATOR)) {
                HOLDERS.add(Class.forName(holder, false, loader));
            }
        } catch (ClassNotFoundException ex) {
            Log.e(TAG, "Cannot register the Wi-Fi scan holders", ex);
        }
    }

    @Override
    public String subject(Context context) {
        return "Wi-Fi Signal";
    }

    @Override
    String[] header() {
        return HEADER;
    }

    @Override
    void writeRows(Context context, ExportTable table) {
        if (context
            .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final List<Hotspot> hotspots = hotspots();
        for (String band : BANDS) {
            final List<Hotspot> inBand = new ArrayList<>();
            for (Hotspot hotspot : hotspots) {
                if (band.equals(hotspot.band())) {
                    inBand.add(hotspot);
                }
            }
            Collections.sort(inBand, STRONGEST_FIRST);
            for (Hotspot hotspot : inBand) {
                table.add(hotspot.ssid)
                    .add(hotspot.bssid)
                    .add((hotspot.bssid != null) ? OuiDatabase.vendor(context, hotspot.bssid) : null)
                    .add(band)
                    .addLong(hotspot.primaryChannel)
                    .add(hotspot.frequency + MEGAHERTZ)
                    .addLong(hotspot.centerChannel)
                    .add(hotspot.frequencyOfChannel(hotspot.centerChannel) + MEGAHERTZ)
                    .add(hotspot.width + MEGAHERTZ)
                    .add(hotspot.level + DECIBEL_MILLIWATTS)
                    .add(hotspot.encryption())
                    .add(hotspot.standard())
                    .addRaw(hotspot.wps ? YES : NO)
                    .addRaw(hotspot.connected ? YES : NO)
                    .endRow();
            }
        }
    }

    private static List<Hotspot> hotspots() {
        final List<Hotspot> hotspots = new ArrayList<>();
        if (hotspotClass == null) {
            return hotspots;
        }
        try {
            for (Class<?> holder : HOLDERS) {
                final List<?> models = StaticStates.listOf(holder, hotspotClass);
                if (models.isEmpty()) {
                    continue;
                }
                for (Object model : models) {
                    hotspots.add(Hotspot.read(model));
                }
                return hotspots;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Log.e(TAG, "Cannot read the Wi-Fi scan results", ex);
        }
        return hotspots;
    }

}
