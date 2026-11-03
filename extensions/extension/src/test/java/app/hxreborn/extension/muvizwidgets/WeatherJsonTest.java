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
package app.hxreborn.extension.muvizwidgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class WeatherJsonTest {

    private static final String GEOCODING = "{\"results\":[{\"name\":\"Ankara\",\"latitude\":39.91987,"
            + "\"longitude\":32.85427,\"timezone\":\"Europe/Istanbul\",\"country\":\"Turkiye\",\"admin1\":\"Ankara\"},"
            + "{\"name\":\"Ankaran\",\"latitude\":45.57902,\"longitude\":13.73616,\"timezone\":\"Europe/Ljubljana\","
            + "\"country\":\"Slovenia\"}]}";

    private static final String FORECAST = "{\"timezone\":\"Europe/Istanbul\",\"current\":{\"temperature_2m\":14.0,"
            + "\"weather_code\":2,\"is_day\":1}}";

    @Test
    public void mapsGeocodingResultsToCityFields() throws Exception {
        final JSONArray cities = WeatherJson.cities(GEOCODING);

        assertEquals(2, cities.length());
        final JSONObject first = cities.getJSONObject(0);
        assertEquals("Ankara", first.getString("name"));
        assertEquals("Ankara", first.getString("region"));
        assertEquals("Turkiye", first.getString("country"));
        assertEquals("Europe/Istanbul", first.getString("tz_id"));
        assertEquals(39.91987, first.getDouble("lat"), 0.00001);
        assertEquals(32.85427, first.getDouble("lon"), 0.00001);
        assertEquals("", cities.getJSONObject(1).getString("region"));
    }

    @Test
    public void returnsNoCitiesWhenGeocodingFindsNothing() throws Exception {
        assertEquals(0, WeatherJson.cities("{\"generationtime_ms\":0.2}").length());
    }

    @Test
    public void mapsForecastToWeatherFields() throws Exception {
        final JSONObject weather = WeatherJson.current(FORECAST, "Ankara", 1762160000L);

        assertEquals("Ankara", weather.getString("cityName"));
        assertEquals("Partly cloudy", weather.getString("condition"));
        assertEquals(1003, weather.getInt("conditionCode"));
        assertEquals("https://cdn.weatherapi.com/weather/64x64/day/116.png", weather.getString("conditionIcon"));
        assertTrue(weather.getBoolean("isDay"));
        assertEquals(1762160000L, weather.getLong("lastUpdated"));
        assertEquals(14.0, weather.getDouble("tempCelsius"), 0.001);
        assertEquals(57.2, weather.getDouble("tempFahrenheit"), 0.001);
        assertEquals("Europe/Istanbul", weather.getString("timeZoneId"));
    }

    @Test
    public void usesNightIconAndClearTextAfterDark() throws Exception {
        final String forecast = FORECAST.replace("\"weather_code\":2", "\"weather_code\":0")
            .replace("\"is_day\":1", "\"is_day\":0");

        final JSONObject weather = WeatherJson.current(forecast, "Ankara", 0L);

        assertEquals("Clear", weather.getString("condition"));
        assertFalse(weather.getBoolean("isDay"));
        assertEquals("https://cdn.weatherapi.com/weather/64x64/night/113.png", weather.getString("conditionIcon"));
    }

    @Test
    public void mapsEveryWmoCodeToIconNumberAndText() {
        final int[] wmoCodes = { 0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82,
                85, 86, 95, 96, 99 };

        for (final int wmoCode : wmoCodes) {
            final int icon = WeatherJson.conditionCode(wmoCode) - 887;
            assertTrue("WMO " + wmoCode, icon >= 113 && icon <= 389);
            assertFalse("WMO " + wmoCode, WeatherJson.conditionText(wmoCode, true).isEmpty());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownWmoCode() {
        WeatherJson.conditionCode(42);
    }

    @Test
    public void readsReverseGeocodingName() throws Exception {
        assertEquals("Ankara", WeatherJson.reverseName("{\"city\":\"Ankara\",\"locality\":\"Kizilay\"}"));
        assertEquals("Kizilay", WeatherJson.reverseName("{\"city\":\"\",\"locality\":\"Kizilay\"}"));
        assertEquals("", WeatherJson.reverseName("{}"));
    }

    @Test
    public void recognisesCoordinatePairs() {
        assertTrue(WeatherJson.isCoordinatePair("39.919912,32.854"));
        assertTrue(WeatherJson.isCoordinatePair("-35.1,-117.99"));
        assertFalse(WeatherJson.isCoordinatePair("Ankara"));
        assertFalse(WeatherJson.isCoordinatePair("Paris,France"));
    }

    @Test
    public void keysCoordinatesLikeTheAppPrintsThem() {
        assertEquals("39.91987,32.85427", WeatherJson.coordinateKey(39.91987, 32.85427));
        assertEquals(WeatherJson.coordinateKey(39.91987, 32.85427),
                WeatherJson.coordinateKey((double) (float) 39.91987, (double) (float) 32.85427));
    }

}
