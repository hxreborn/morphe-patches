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
package app.hxreborn.extension.liftoff;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@SuppressWarnings("unused")
public final class Customizations {

    private static final String PROFILE_PROCEDURE = "user.findUserProfileByAccessToken";

    private static final String API_DOMAIN = "api.getgymbros.com";

    private static final ThreadLocal<String[]> RESPONSE_PROCEDURES = new ThreadLocal<>();

    private static final String[] THEMES = { "theme_amberDark", "theme_amberLight", "theme_aquaDark", "theme_aquaLight",
            "theme_autumnVibes", "theme_azureScale", "theme_blueberryDark", "theme_blueberryLight", "theme_cherryDark",
            "theme_cherryLight", "theme_classicDark", "theme_cottonCandy", "theme_crimsonScale", "theme_darkest",
            "theme_duskHorizon", "theme_grapeDark", "theme_grapeLight", "theme_jadeApple", "theme_lemonBerry",
            "theme_limeDark", "theme_limeLight", "theme_monochromeDark", "theme_monochromeLight", "theme_neonRider",
            "theme_retro", "theme_roseDark", "theme_roseLight", "theme_springBlossom", "theme_steelEmulsion",
            "theme_summerBreeze", "theme_twilightSpace", "theme_winterWhisper" };

    private static final String[] BORDERS = { "border_air", "border_asteroids", "border_defaultColor", "border_earth",
            "border_emeraldShine", "border_fire", "border_floral", "border_fourSeasons", "border_halloween2026",
            "border_moon", "border_premium", "border_rainbow", "border_rubyShine", "border_sapphireShine",
            "border_solar", "border_starShine", "border_steelShade", "border_summer", "border_water" };

    private static final String[] BANNERS = { "banner_beta", "banner_constellation", "banner_defaultColor",
            "banner_fall", "banner_fireworks", "banner_oceanFloor", "banner_plains", "banner_winter" };

    private Customizations() {
    }

    public static void onResponse(String url) {
        RESPONSE_PROCEDURES.remove();
        if (url == null || !url.contains(PROFILE_PROCEDURE) || !PatchSettings.areCustomizationsUnlocked()) {
            return;
        }
        try {
            URI response = new URI(url);
            String host = response.getHost();
            String path = response.getPath();
            if (path != null && host != null && (API_DOMAIN.equals(host) || host.endsWith("." + API_DOMAIN))) {
                RESPONSE_PROCEDURES.set(path.substring(path.lastIndexOf('/') + 1).split(","));
            }
        } catch (URISyntaxException ex) {
            Log.w(PatchSettings.LOG_TAG, "Failed to parse profile response URL");
        }
    }

    public static String rewriteBody(String body) {
        String[] procedures = takeProcedures();
        return (procedures == null || body == null) ? body : rewriteJson(body, procedures);
    }

    public static byte[] rewriteBody(byte[] body) {
        String[] procedures = takeProcedures();
        if (procedures == null || body == null) {
            return body;
        }
        String original = new String(body, StandardCharsets.UTF_8);
        String rewritten = rewriteJson(original, procedures);
        return rewritten.equals(original) ? body : rewritten.getBytes(StandardCharsets.UTF_8);
    }

    private static String[] takeProcedures() {
        String[] procedures = RESPONSE_PROCEDURES.get();
        RESPONSE_PROCEDURES.remove();
        return procedures;
    }

    private static String rewriteJson(String body, String[] procedures) {
        try {
            Object root = new JSONTokener(body).nextValue();
            boolean changed = false;
            if (root instanceof JSONArray) {
                JSONArray responses = (JSONArray) root;
                for (int index = 0; index < procedures.length; index++) {
                    if (PROFILE_PROCEDURE.equals(procedures[index])) {
                        changed |= grantCustomizations(responses.optJSONObject(index));
                    }
                }
            } else if (root instanceof JSONObject && procedures.length == 1
                    && PROFILE_PROCEDURE.equals(procedures[0])) {
                changed = grantCustomizations((JSONObject) root);
            }
            return changed ? root.toString() : body;
        } catch (JSONException ex) {
            Log.w(PatchSettings.LOG_TAG, "Failed to parse profile customization response");
            return body;
        }
    }

    private static boolean grantCustomizations(JSONObject response) throws JSONException {
        JSONObject result = (response != null) ? response.optJSONObject("result") : null;
        JSONObject data = (result != null) ? result.optJSONObject("data") : null;
        JSONObject profile = (data != null) ? data.optJSONObject("json") : null;
        JSONObject purchases = (profile != null) ? profile.optJSONObject("purchases") : null;
        if (purchases == null) {
            return false;
        }
        boolean changed = appendItems(purchases.optJSONArray("purchasedThemes"), THEMES);
        changed |= appendItems(purchases.optJSONArray("purchasedProfileBorders"), BORDERS);
        changed |= appendItems(purchases.optJSONArray("purchasedProfileBanners"), BANNERS);
        return changed;
    }

    private static boolean appendItems(JSONArray owned, String[] available) {
        if (owned == null) {
            return false;
        }
        Set<String> identifiers = new HashSet<>();
        for (int index = 0; index < owned.length(); index++) {
            identifiers.add(owned.optString(index));
        }
        boolean changed = false;
        for (String identifier : available) {
            if (identifiers.add(identifier)) {
                owned.put(identifier);
                changed = true;
            }
        }
        return changed;
    }

}
