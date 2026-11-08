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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, shadows = EqualizerStylesTest.BasesShadow.class)
public class EqualizerStylesTest {

    private static String definitions;

    @Before
    public void loadStockDefinition() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/ultimea/equalizer/styles.json")) {
            Assert.assertNotNull(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            definitions = new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
        ReflectionHelpers.setStaticField(EqualizerStyles.class, "points", null);
    }

    @Test
    public void loadsEveryStockCurveInRowMajorOrder() {
        List<EqualizerStyles.Point> points = EqualizerStyles.points();
        Assert.assertEquals(121, points.size());
        Assert.assertSame(points, EqualizerStyles.points());
        for (int index = 0; index < points.size(); index++) {
            EqualizerStyles.Point point = points.get(index);
            Assert.assertEquals(index / EqualizerStyles.GRID_SIZE, point.row);
            Assert.assertEquals(index % EqualizerStyles.GRID_SIZE, point.column);
            Assert.assertSame(point, EqualizerStyles.at(point.row, point.column));
            Assert.assertEquals(EqualizerStyles.STYLE_MODE, point.curve.mode());
            Assert.assertSame(point, EqualizerStyles.match(point.curve));
        }
        Assert.assertEquals("B（-2，2）", points.get(0).id);
        Assert.assertEquals(55, points.get(0).curve.gain(0));
        Assert.assertEquals("C（2，-2）", points.get(points.size() - 1).id);
        Assert.assertEquals(-25, points.get(points.size() - 1).curve.gain(9));
    }

    @Test
    public void namesOnlyFourStockAnchorsAndKeepsCenterIdentifier() {
        Assert.assertEquals("Bass", EqualizerStyles.at(2, 2).name);
        Assert.assertEquals("Rock", EqualizerStyles.at(2, 8).name);
        Assert.assertEquals("Pop", EqualizerStyles.at(8, 2).name);
        Assert.assertEquals("Classical", EqualizerStyles.at(8, 8).name);
        Assert.assertEquals("O", EqualizerStyles.at(5, 5).id);
        Assert.assertNull(EqualizerStyles.at(5, 5).name);
        Assert.assertEquals(new EqualizerCurve(8, new int[10]), EqualizerStyles.at(5, 5).curve);
        int named = 0;
        for (EqualizerStyles.Point point : EqualizerStyles.points()) {
            if (point.name != null) {
                named++;
            }
        }
        Assert.assertEquals(4, named);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void rejectsListMutation() {
        EqualizerStyles.points().clear();
    }

    @Test
    public void matchesOnlyExactStyleModeAndGains() {
        EqualizerCurve curve = EqualizerStyles.at(0, 0).curve;
        Assert.assertNull(EqualizerStyles.match(null));
        Assert.assertNull(EqualizerStyles.match(curve.asCustom()));
        EqualizerCurve changed = curve.withGain(0, 54);
        int[] gains = new int[EqualizerCurve.BAND_COUNT];
        for (int band = 0; band < gains.length; band++) {
            gains[band] = changed.gain(band);
        }
        Assert.assertNull(EqualizerStyles.match(new EqualizerCurve(EqualizerStyles.STYLE_MODE, gains)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeRow() {
        EqualizerStyles.at(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRowBeyondGrid() {
        EqualizerStyles.at(EqualizerStyles.GRID_SIZE, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeColumn() {
        EqualizerStyles.at(0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsColumnBeyondGrid() {
        EqualizerStyles.at(0, EqualizerStyles.GRID_SIZE);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsMalformedRowCount() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        json.put("style", new JSONArray());
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsMalformedColumnCount() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        json.getJSONArray("style").put(4, new JSONArray());
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsMalformedBandCount() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).put("db", new JSONArray());
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test
    public void acceptsTenthDbGainPrecision() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).getJSONArray("db").put(0, 5.1);
        point(json, 0, 0).getJSONArray("db").put(1, -5.1);
        definitions = json.toString();
        Assert.assertEquals(51, EqualizerStyles.at(0, 0).curve.gain(0));
        Assert.assertEquals(-51, EqualizerStyles.at(0, 0).curve.gain(1));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsMissingPointIdentifier() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).remove("point");
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsBlankPointIdentifier() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).put("point", " ");
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsNonNumericGain() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).getJSONArray("db").put(0, "5.5");
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsGainBeyondTenthDbPrecision() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).getJSONArray("db").put(0, 5.55);
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsGainAboveRange() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).getJSONArray("db").put(0, 6.1);
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsGainBelowRange() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 0).getJSONArray("db").put(0, -6.1);
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsDuplicatePointIdentifiers() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 1).put("point", point(json, 0, 0).get("point"));
        definitions = json.toString();
        EqualizerStyles.points();
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsDuplicateCurves() throws JSONException {
        JSONObject json = new JSONObject(definitions);
        point(json, 0, 1).put("db", point(json, 0, 0).getJSONArray("db"));
        definitions = json.toString();
        EqualizerStyles.points();
    }

    private static JSONObject point(JSONObject json, int row, int column) throws JSONException {
        return json.getJSONArray("style").getJSONArray(row).getJSONObject(column);
    }

    @Implements(EqualizerBases.class)
    public static class BasesShadow {

        @Implementation
        protected static String definitions() {
            return EqualizerStylesTest.definitions;
        }

    }

}
