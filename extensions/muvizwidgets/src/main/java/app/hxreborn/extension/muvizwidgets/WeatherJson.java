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

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherJson {

    private static final String ICON_URL = "https://cdn.weatherapi.com/weather/64x64/%s/%d.png";

    private static final int ICON_OFFSET = 887;

    private static final int FAHRENHEIT_SCALE_NUMERATOR = 9;

    private static final int FAHRENHEIT_SCALE_DENOMINATOR = 5;

    private static final int FAHRENHEIT_AT_ZERO_CELSIUS = 32;

    private static final int[] WMO_CODES = { 0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
            80, 81, 82, 85, 86, 95, 96, 99 };

    private static final int[] CONDITION_CODES = { 1000, 1000, 1003, 1009, 1135, 1147, 1150, 1153, 1153, 1168, 1171,
            1183, 1189, 1195, 1198, 1201, 1213, 1219, 1225, 1210, 1240, 1243, 1246, 1255, 1258, 1273, 1276, 1276 };

    private static final String[] CONDITION_TEXTS = { "Sunny", "Sunny", "Partly cloudy", "Overcast", "Fog",
            "Freezing fog", "Patchy light drizzle", "Light drizzle", "Light drizzle", "Freezing drizzle",
            "Heavy freezing drizzle", "Light rain", "Moderate rain", "Heavy rain", "Light freezing rain",
            "Moderate or heavy freezing rain", "Light snow", "Moderate snow", "Heavy snow", "Patchy light snow",
            "Light rain shower", "Moderate or heavy rain shower", "Torrential rain shower", "Light snow showers",
            "Moderate or heavy snow showers", "Patchy light rain with thunder", "Moderate or heavy rain with thunder",
            "Moderate or heavy rain with thunder" };

    private WeatherJson() {
    }

    public static String coordinateKey(double latitude, double longitude) {
        return Float.toString((float) latitude) + "," + Float.toString((float) longitude);
    }

    public static boolean isCoordinatePair(String query) {
        return query.matches("-?\\d+(\\.\\d+)?,-?\\d+(\\.\\d+)?");
    }

    private static JSONObject city(String name, String region, String country, String timeZoneId, double latitude,
            double longitude) throws JSONException {
        return new JSONObject().put("name", name)
            .put("region", region)
            .put("country", country)
            .put("tz_id", timeZoneId)
            .put("lat", latitude)
            .put("lon", longitude);
    }

    public static JSONArray cities(String geocodingResponse) throws JSONException {
        final JSONObject root = new JSONObject(geocodingResponse);
        final JSONArray results = root.optJSONArray("results");
        final JSONArray cities = new JSONArray();
        if (results == null) {
            return cities;
        }
        for (int i = 0; i < results.length(); i++) {
            final JSONObject result = results.getJSONObject(i);
            cities.put(city(result.getString("name"), result.optString("admin1", ""), result.optString("country", ""),
                    result.optString("timezone", TimeZone.getDefault().getID()), result.getDouble("latitude"),
                    result.getDouble("longitude")));
        }
        return cities;
    }

    public static String reverseName(String reverseGeocodingResponse) throws JSONException {
        return placeName(new JSONObject(reverseGeocodingResponse));
    }

    public static JSONObject reverseCity(String reverseGeocodingResponse, double latitude, double longitude)
            throws JSONException {
        final JSONObject root = new JSONObject(reverseGeocodingResponse);
        return city(placeName(root), root.optString("principalSubdivision", ""), root.optString("countryName", ""),
                TimeZone.getDefault().getID(), latitude, longitude);
    }

    public static JSONObject current(String forecastResponse, String cityName, long nowSeconds) throws JSONException {
        final JSONObject root = new JSONObject(forecastResponse);
        final JSONObject observation = root.getJSONObject("current");
        final double celsius = observation.getDouble("temperature_2m");
        final boolean isDay = observation.getInt("is_day") == 1;
        final int wmoCode = observation.getInt("weather_code");
        final int conditionCode = conditionCode(wmoCode);
        return new JSONObject().put("cityName", cityName)
            .put("condition", conditionText(wmoCode, isDay))
            .put("conditionCode", conditionCode)
            .put("conditionIcon",
                    String.format(Locale.ROOT, ICON_URL, isDay ? "day" : "night", conditionCode - ICON_OFFSET))
            .put("isDay", isDay)
            .put("lastUpdated", nowSeconds)
            .put("tempCelsius", celsius)
            .put("tempFahrenheit",
                    celsius * FAHRENHEIT_SCALE_NUMERATOR / FAHRENHEIT_SCALE_DENOMINATOR + FAHRENHEIT_AT_ZERO_CELSIUS)
            .put("timeZoneId", root.getString("timezone"));
    }

    public static int conditionCode(int wmoCode) {
        return CONDITION_CODES[indexOfWmoCode(wmoCode)];
    }

    public static String conditionText(int wmoCode, boolean isDay) {
        final int index = indexOfWmoCode(wmoCode);
        if (WMO_CODES[index] == 0 || WMO_CODES[index] == 1) {
            return isDay ? "Sunny" : "Clear";
        }
        return CONDITION_TEXTS[index];
    }

    private static String placeName(JSONObject reverseGeocoding) {
        for (final String key : new String[] { "city", "locality", "principalSubdivision" }) {
            final String value = reverseGeocoding.optString(key, "");
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static int indexOfWmoCode(int wmoCode) {
        for (int i = 0; i < WMO_CODES.length; i++) {
            if (WMO_CODES[i] == wmoCode) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown WMO weather code " + wmoCode);
    }

}
