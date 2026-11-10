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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class EqualizerStyles {

    static final int GRID_SIZE = 11;

    static final int STYLE_MODE = 8;

    private static final int FIRST_ANCHOR = 2;

    private static final int LAST_ANCHOR = 8;

    private static final int TENTHS_PER_DB = 10;

    private static List<Point> points;

    private EqualizerStyles() {
    }

    static synchronized List<Point> points() {
        if (points != null) {
            return points;
        }
        try {
            JSONArray rows = new JSONObject(EqualizerBases.definitions()).getJSONArray("style");
            if (rows.length() != GRID_SIZE) {
                throw new JSONException("Expected eleven style rows");
            }
            List<Point> parsed = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            Set<EqualizerCurve> curves = new HashSet<>();
            for (int row = 0; row < GRID_SIZE; row++) {
                JSONArray columns = rows.getJSONArray(row);
                if (columns.length() != GRID_SIZE) {
                    throw new JSONException("Expected eleven style columns");
                }
                for (int column = 0; column < GRID_SIZE; column++) {
                    JSONObject item = columns.getJSONObject(column);
                    Object id = item.get("point");
                    if (!(id instanceof String) || ((String) id).trim().isEmpty() || !ids.add((String) id)) {
                        throw new JSONException("Expected unique style point identifiers");
                    }
                    JSONArray values = item.getJSONArray("db");
                    if (values.length() != EqualizerCurve.BAND_COUNT) {
                        throw new JSONException("Expected ten style bands");
                    }
                    int[] gains = new int[EqualizerCurve.BAND_COUNT];
                    for (int band = 0; band < gains.length; band++) {
                        Object value = values.get(band);
                        if (!(value instanceof Number)) {
                            throw new JSONException("Expected numeric style gains");
                        }
                        double tenths = ((Number) value).doubleValue() * TENTHS_PER_DB;
                        if (tenths != Math.rint(tenths) || tenths < EqualizerCurve.MIN_GAIN_TENTHS
                                || tenths > EqualizerCurve.MAX_GAIN_TENTHS) {
                            throw new JSONException("Style gain exceeds supported range or precision");
                        }
                        gains[band] = (int) tenths;
                    }
                    EqualizerCurve curve = new EqualizerCurve(STYLE_MODE, gains);
                    if (!curves.add(curve)) {
                        throw new JSONException("Expected unique style curves");
                    }
                    parsed.add(new Point(row, column, (String) id, name(row, column), curve));
                }
            }
            points = Collections.unmodifiableList(parsed);
            return points;
        } catch (JSONException ex) {
            throw new IllegalStateException("Equalizer styles could not be read", ex);
        }
    }

    static Point at(int row, int column) {
        if (row < 0 || row >= GRID_SIZE || column < 0 || column >= GRID_SIZE) {
            throw new IllegalArgumentException("Style coordinates exceed grid bounds");
        }
        return points().get(row * GRID_SIZE + column);
    }

    static Point match(EqualizerCurve curve) {
        if (curve == null || curve.mode() != STYLE_MODE) {
            return null;
        }
        for (Point point : points()) {
            if (point.curve.equals(curve)) {
                return point;
            }
        }
        return null;
    }

    private static String name(int row, int column) {
        if (row == FIRST_ANCHOR) {
            if (column == FIRST_ANCHOR) {
                return "Bass";
            }
            if (column == LAST_ANCHOR) {
                return "Rock";
            }
        }
        if (row == LAST_ANCHOR) {
            if (column == FIRST_ANCHOR) {
                return "Pop";
            }
            if (column == LAST_ANCHOR) {
                return "Classical";
            }
        }
        return null;
    }

    static final class Point {

        final int row;

        final int column;

        final String id;

        final String name;

        final EqualizerCurve curve;

        Point(int row, int column, String id, String name, EqualizerCurve curve) {
            this.row = row;
            this.column = column;
            this.id = id;
            this.name = name;
            this.curve = curve;
        }

    }

}
