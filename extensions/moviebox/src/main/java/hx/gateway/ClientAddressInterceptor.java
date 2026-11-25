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

    private static final long PEEKED_BYTES = 4L * 1024 * 1024;

    private final boolean premiumUnlocked;

    public ClientAddressInterceptor(boolean premiumUnlocked) {
        super("MovieBoxClientAddressInterceptor", "MovieBox client address override failed");
        this.premiumUnlocked = premiumUnlocked;
    }

    @Override
    protected Object intercept(Object chain) throws ReflectiveOperationException {
        Object request = request(chain);
        String path = (String) Reflect.call(Reflect.call(request, "url"), "encodedPath");
        if (!this.premiumUnlocked || !Endpoints.listsResourceLinks(path)) {
            return proceed(chain, request);
        }
        Object response = proceed(chain, withWithheldAddress(request));
        if (!((Boolean) Reflect.call(response, "isSuccessful"))) {
            Log.w(Logs.TAG, path + " answered HTTP " + Reflect.call(response, "code")
                    + " with the client address withheld, resending the app's request");
            Reflect.call(response, "close");
            return proceed(chain, request);
        }
        warnAboutRowsWithoutLinks(path, response);
        return response;
    }

    private static Object withWithheldAddress(Object request) throws ReflectiveOperationException {
        Object builder = Reflect.call(request, "newBuilder");
        Reflect.call(builder, "header", new Class<?>[] { String.class, String.class }, FORWARDED_FOR_HEADER,
                WITHHELD_ADDRESS);
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
