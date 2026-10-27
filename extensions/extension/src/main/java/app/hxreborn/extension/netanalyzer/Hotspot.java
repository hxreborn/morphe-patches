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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class Hotspot {

    static final String[] MODES = { "a", "b", "g", "n", "ac", "ax", "be" };

    private static final Class<?>[] LAYOUT = { List.class, List.class, boolean.class, int.class, int.class, int.class,
            int.class, int.class, int.class, String.class, String.class, boolean.class, int.class, int.class,
            String.class, List.class, List.class, boolean.class, boolean.class, String.class, Map.class, Map.class,
            Map.class, Map.class };

    private static final String MODE_A = "a";

    private static final String MODE_AC = "ac";

    private static final String MODE_AX = "ax";

    private static final String LIST_SEPARATOR = "/";

    private static final int CHANNEL_SPACING_MHZ = 5;

    private static final int BAND_2GHZ_BASE_MHZ = 2407;

    private static final int JAPAN_CHANNEL = 14;

    private static final int JAPAN_CHANNEL_MHZ = 2484;

    private static final int BAND_5GHZ_START_MHZ = 3000;

    private static final int BAND_5GHZ_BASE_MHZ = 5000;

    private static final int BAND_6GHZ_START_MHZ = 5925;

    private static final int BAND_6GHZ_BASE_MHZ = 5950;

    private static final int BAND_6GHZ_LOW_CHANNEL = 2;

    private static final int BAND_6GHZ_LOW_CHANNEL_MHZ = 5935;

    final List<?> protocols;

    final List<?> ciphers;

    final boolean wps;

    final int frequency;

    final int primaryChannel;

    final int level;

    final int centerChannel;

    final int secondCenterChannel;

    final int width;

    final String ssid;

    final String bssid;

    final boolean connected;

    final int utilization;

    final int stations;

    final String country;

    final List<?> elementIds;

    final List<?> extensionIds;

    final boolean singleUserBeamforming;

    final boolean multiUserBeamforming;

    final String capabilities;

    final Map<?, ?> maxSpeed;

    final Map<?, ?> streams;

    final Map<?, ?> maxMcs;

    final Map<?, ?> modulation;

    private Hotspot(Object[] values) {
        this.protocols = (List<?>) values[0];
        this.ciphers = (List<?>) values[1];
        this.wps = (Boolean) values[2];
        this.frequency = (Integer) values[3];
        this.primaryChannel = (Integer) values[4];
        this.level = (Integer) values[5];
        this.centerChannel = (Integer) values[6];
        this.secondCenterChannel = (Integer) values[7];
        this.width = (Integer) values[8];
        this.ssid = (String) values[9];
        this.bssid = (String) values[10];
        this.connected = (Boolean) values[11];
        this.utilization = (Integer) values[12];
        this.stations = (Integer) values[13];
        this.country = (String) values[14];
        this.elementIds = (List<?>) values[15];
        this.extensionIds = (List<?>) values[16];
        this.singleUserBeamforming = (Boolean) values[17];
        this.multiUserBeamforming = (Boolean) values[18];
        this.capabilities = (String) values[19];
        this.maxSpeed = (Map<?, ?>) values[20];
        this.streams = (Map<?, ?>) values[21];
        this.maxMcs = (Map<?, ?>) values[22];
        this.modulation = (Map<?, ?>) values[23];
    }

    static Hotspot read(Object model) throws ReflectiveOperationException {
        final List<Field> fields = new ArrayList<>();
        for (Field field : model.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                fields.add(field);
            }
        }
        Collections.sort(fields, (left, right) -> left.getName().compareTo(right.getName()));
        if (fields.size() != LAYOUT.length) {
            throw new NoSuchFieldException("Expected " + LAYOUT.length + " hotspot fields, found " + fields.size());
        }
        final Object[] values = new Object[LAYOUT.length];
        for (int i = 0; i < LAYOUT.length; i++) {
            final Field field = fields.get(i);
            if (!LAYOUT[i].isAssignableFrom(field.getType())) {
                throw new NoSuchFieldException("Unexpected type " + field.getType() + " at hotspot field " + i);
            }
            field.setAccessible(true);
            values[i] = field.get(model);
        }
        return new Hotspot(values);
    }

    static Object modelOf(Object row) throws ReflectiveOperationException {
        for (Field field : row.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                field.setAccessible(true);
                return field.get(row);
            }
        }
        throw new NoSuchFieldException("No hotspot in " + row.getClass());
    }

    String band() {
        if (this.frequency < BAND_5GHZ_START_MHZ) {
            return "2.4 GHz";
        }
        return (this.frequency < BAND_6GHZ_START_MHZ) ? "5 GHz" : "6 GHz";
    }

    String encryption() {
        final String protocolList = join(this.protocols);
        return this.ciphers.isEmpty() ? protocolList : protocolList + " (" + join(this.ciphers) + ")";
    }

    String standard() {
        final StringBuilder standard = new StringBuilder();
        final boolean skipLegacyA = this.maxSpeed.get(MODE_AC) != null && this.maxSpeed.get(MODE_AX) != null;
        for (String mode : MODES) {
            final Object speed = this.maxSpeed.get(mode);
            if (speed == null || (skipLegacyA && MODE_A.equals(mode))) {
                continue;
            }
            if (standard.length() > 0) {
                standard.append(LIST_SEPARATOR);
            }
            standard.append(mode).append(Math.round(((Number) speed).doubleValue()));
            final Object streamCount = this.streams.get(mode);
            if (streamCount != null && ((Number) streamCount).intValue() > 1) {
                standard.append('(').append(streamCount).append(')');
            }
        }
        return standard.toString();
    }

    private static String join(List<?> values) {
        final StringBuilder joined = new StringBuilder();
        for (Object value : values) {
            if (joined.length() > 0) {
                joined.append(LIST_SEPARATOR);
            }
            joined.append(value);
        }
        return joined.toString();
    }

    int frequencyOfChannel(int channel) {
        if (this.frequency < BAND_5GHZ_START_MHZ) {
            return (channel == JAPAN_CHANNEL) ? JAPAN_CHANNEL_MHZ : BAND_2GHZ_BASE_MHZ + CHANNEL_SPACING_MHZ * channel;
        }
        if (this.frequency < BAND_6GHZ_START_MHZ) {
            return BAND_5GHZ_BASE_MHZ + CHANNEL_SPACING_MHZ * channel;
        }
        return (channel == BAND_6GHZ_LOW_CHANNEL) ? BAND_6GHZ_LOW_CHANNEL_MHZ
                : BAND_6GHZ_BASE_MHZ + CHANNEL_SPACING_MHZ * channel;
    }

}
