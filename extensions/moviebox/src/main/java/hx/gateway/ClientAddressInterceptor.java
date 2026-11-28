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
package hx.gateway;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import android.util.Log;
import hx.okhttp.ChainInterceptor;
import hx.source.Endpoints;
import hx.util.Logs;
import hx.util.Reflect;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ClientAddressInterceptor extends ChainInterceptor {

    static final String FORWARDED_FOR_HEADER = "x-forwarded-for";

    static final String WITHHELD_ADDRESS = "unknown";

    static final String REGION_BLOCK_MESSAGE = "Service not available in current region";

    private static final int FORBIDDEN = 403;

    private static final int TOO_MANY_REQUESTS = 429;

    private static final long PEEKED_BYTES = 4L * 1024 * 1024;

    private static final long REGION_BLOCK_PEEKED_BYTES = 1024;

    private static final String DEVICE_COUNTRY = "";

    private final boolean premiumUnlocked;

    private final ChosenCountry chosenCountry;

    private final Set<String> regionBlockedCountries = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final AtomicReference<String> loggedCountry = new AtomicReference<>(DEVICE_COUNTRY);

    public ClientAddressInterceptor(boolean premiumUnlocked, ChosenCountry chosenCountry) {
        super("MovieBoxClientAddressInterceptor", "MovieBox client address override failed");
        this.premiumUnlocked = premiumUnlocked;
        this.chosenCountry = chosenCountry;
    }

    @Override
    protected Object intercept(Object chain) throws ReflectiveOperationException {
        Object request = request(chain);
        String path = (String) Reflect.call(Reflect.call(request, "url"), "encodedPath");
        String country = chosenCountryWithAddress();
        if (!this.premiumUnlocked || !Endpoints.listsResourceLinks(path)) {
            return proceedWithCountryAddress(chain, request, country, path);
        }
        Object response = proceed(chain, withForwardedFor(request, WITHHELD_ADDRESS));
        if (!((Boolean) Reflect.call(response, "isSuccessful"))) {
            Log.w(Logs.TAG, path + " answered HTTP " + Reflect.call(response, "code")
                    + " with the client address withheld, resending the app's request");
            Reflect.call(response, "close");
            return proceedWithCountryAddress(chain, request, country, path);
        }
        warnAboutRowsWithoutLinks(path, response);
        return response;
    }

    private String chosenCountryWithAddress() {
        String country = this.chosenCountry.isoCode().toUpperCase(Locale.ROOT);
        if (CountryAddresses.addressIn(country) == null) {
            country = DEVICE_COUNTRY;
        }
        if (!country.equals(this.loggedCountry.getAndSet(country))) {
            Log.i(Logs.TAG, country.equals(DEVICE_COUNTRY) ? "API requests use the device address"
                    : "API requests use an address in " + country + ", chosen in the Laboratory");
        }
        return country;
    }

    private Object proceedWithCountryAddress(Object chain, Object request, String country, String path)
            throws ReflectiveOperationException {
        if (country.equals(DEVICE_COUNTRY) || !path.startsWith(Endpoints.API_PATH_PREFIX)) {
            return proceedWithinRegion(chain, request, DEVICE_COUNTRY, path);
        }
        Object response = proceedWithinRegion(chain, withForwardedFor(request, CountryAddresses.addressIn(country)),
                country, path);
        int code = (Integer) Reflect.call(response, "code");
        if (code != FORBIDDEN && code != TOO_MANY_REQUESTS) {
            return response;
        }
        Log.w(Logs.TAG, path + " answered HTTP " + code + " to the address in " + country
                + ", resending with the device address");
        Reflect.call(response, "close");
        return proceedWithinRegion(chain, request, DEVICE_COUNTRY, path);
    }

    private Object proceedWithinRegion(Object chain, Object request, String country, String path)
            throws ReflectiveOperationException {
        if (this.regionBlockedCountries.contains(country)) {
            return proceed(chain, withForwardedFor(request, WITHHELD_ADDRESS));
        }
        Object response = proceed(chain, request);
        if (!isRegionBlock(response)) {
            return response;
        }
        Log.w(Logs.TAG, path + " is blocked in " + (country.equals(DEVICE_COUNTRY) ? "this region" : country)
                + ", withholding the client address from now on");
        this.regionBlockedCountries.add(country);
        Reflect.call(response, "close");
        return proceed(chain, withForwardedFor(request, WITHHELD_ADDRESS));
    }

    private static boolean isRegionBlock(Object response) throws ReflectiveOperationException {
        if ((Integer) Reflect.call(response, "code") != FORBIDDEN || Reflect.call(response, "body") == null) {
            return false;
        }
        Object peeked = Reflect.call(response, "peekBody", new Class<?>[] { long.class }, REGION_BLOCK_PEEKED_BYTES);
        return ((String) Reflect.call(peeked, "string")).contains(REGION_BLOCK_MESSAGE);
    }

    private static Object withForwardedFor(Object request, String address) throws ReflectiveOperationException {
        Object builder = Reflect.call(request, "newBuilder");
        Reflect.call(builder, "header", new Class<?>[] { String.class, String.class }, FORWARDED_FOR_HEADER, address);
        return Reflect.call(builder, "build");
    }

    private static void warnAboutRowsWithoutLinks(String path, Object response) throws ReflectiveOperationException {
        if (Reflect.call(response, "body") == null) {
            return;
        }
        Object peeked = Reflect.call(response, "peekBody", new Class<?>[] { long.class }, PEEKED_BYTES);
        JSONArray rows = rows((String) Reflect.call(peeked, "string"));
        if (rows == null) {
            return;
        }
        int withoutLink = 0;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && row.has("resourceLink") && row.optString("resourceLink").isEmpty()) {
                withoutLink++;
            }
        }
        if (withoutLink > 0) {
            Log.w(Logs.TAG, path + ": " + withoutLink + " of " + rows.length()
                    + " rows have no link with the client address withheld");
        }
    }

    private static JSONArray rows(String body) {
        try {
            JSONObject data = new JSONObject(body).optJSONObject("data");
            return (data != null) ? data.optJSONArray("list") : null;
        } catch (JSONException ex) {
            Log.w(Logs.TAG, "resource rows not read");
            return null;
        }
    }

}
