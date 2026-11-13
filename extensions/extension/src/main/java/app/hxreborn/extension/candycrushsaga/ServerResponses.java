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
package app.hxreborn.extension.candycrushsaga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class ServerResponses {

    private static final int MAX_BYTES = 8 * 1024 * 1024;

    private static final int BUFFER_BYTES = 8192;

    private static final int GZIP_MAGIC_FIRST = 0x1f;

    private static final int GZIP_MAGIC_SECOND = 0x8b;

    private static final int BYTE_MASK = 0xff;

    private static final String PLUGINS = "UopFeatureStatusApi.getAllPlugins";

    private static final Set<String> AD_PLUGINS = new HashSet<>(Arrays.asList("ads_ool", "ads_egp", "ads_pre_level",
            "ads_map_balloon", "ads_treat_machine", "ads_hard_level_offer", "ads_arcade", "ads_quest",
            "ads_episode_race", "ads_skip_an_ad", "mini_ads_game", "ads_interstitials"));

    private static final String POPUPS = "CandyPopupConfigApi.getPopupUpdatedConfig";

    private static final String[] OFFER_CATEGORIES = { "dynamic_offer", "build_a_bundle", "endless_offer",
            "booster_offer_popup", "sale", "milestone_offer", "hard_level_offer", "onfire_offer", "piggy_bank",
            "season_pass", "economy_loyalty", "marketing_message" };

    private ServerResponses() {
    }

    private static boolean removeOffers() {
        return false;
    }

    private static boolean removeAdOffers() {
        return false;
    }

    public static InputStream open(HttpURLConnection connection, Object task) throws IOException {
        byte[] request = requestBody(task);
        InputStream source = connection.getInputStream();
        if (!"POST".equals(connection.getRequestMethod()) || !"/rpc/ClientApi".equals(connection.getURL().getPath())
                || request == null || request.length > MAX_BYTES) {
            return source;
        }
        return response(source, request, selectedMethods(), selectedPlugins());
    }

    static InputStream response(InputStream source, byte[] request, Set<String> targets, Set<String> plugins)
            throws IOException {
        if (request == null || request.length > MAX_BYTES) {
            return source;
        }
        Map<String, String> methods;
        try {
            methods = methods(decode(request), targets);
        } catch (IOException | JSONException ex) {
            Log.w(Boosters.LOG_TAG, "Configuration request could not be read");
            return source;
        }
        if (methods.isEmpty()) {
            return source;
        }
        try {
            return bufferResponse(source, methods, plugins);
        } catch (IOException | RuntimeException ex) {
            try {
                source.close();
            } catch (IOException closeFailure) {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }
    }

    private static InputStream bufferResponse(InputStream source, Map<String, String> methods, Set<String> plugins)
            throws IOException {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_BYTES];
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Configuration response interrupted");
            }
            int count = source.read(buffer);
            if (count < 0) {
                break;
            }
            original.write(buffer, 0, count);
            if (original.size() > MAX_BYTES) {
                Log.w(Boosters.LOG_TAG, "Configuration response exceeds byte limit");
                return new SequenceInputStream(new ByteArrayInputStream(original.toByteArray()), source);
            }
        }
        byte[] bytes = original.toByteArray();
        try {
            String decoded = decode(bytes);
            String edited = edit(decoded, methods, plugins);
            if (!decoded.equals(edited)) {
                byte[] replacement = edited.getBytes(StandardCharsets.UTF_8);
                if (gzip(bytes)) {
                    ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                    try (GZIPOutputStream output = new GZIPOutputStream(compressed)) {
                        output.write(replacement);
                    }
                    replacement = compressed.toByteArray();
                }
                bytes = replacement;
            }
        } catch (IOException | JSONException ex) {
            Log.w(Boosters.LOG_TAG, "Configuration response could not be edited");
        }
        return new SequenceInputStream(new ByteArrayInputStream(bytes), source);
    }

    private static byte[] requestBody(Object task) {
        return null;
    }

    private static Set<String> selectedMethods() {
        Set<String> targets = new HashSet<>();
        if (removeOffers()) {
            targets.add(POPUPS);
        }
        if (removeAdOffers()) {
            targets.add(PLUGINS);
        }
        return targets;
    }

    static Map<String, String> methods(String request, Set<String> targets) throws JSONException {
        Map<String, String> methods = new HashMap<>();
        for (JsonText call : JsonText.parseShallow(request).values) {
            JsonText method = call.fields.get("method");
            JsonText id = call.fields.get("id");
            if (method != null && id != null && targets.contains(method.string())) {
                methods.put(id.raw(), method.string());
            }
        }
        return methods;
    }

    private static Set<String> selectedPlugins() {
        Set<String> plugins = new HashSet<>();
        if (removeAdOffers()) {
            plugins.addAll(AD_PLUGINS);
        }
        return plugins;
    }

    static String edit(String response, Map<String, String> methods, Set<String> plugins) throws JSONException {
        TreeMap<Integer, Replacement> edits = new TreeMap<>();
        for (JsonText reply : JsonText.parseShallow(response).values) {
            JsonText id = reply.fields.get("id");
            JsonText result = reply.fields.get("result");
            if (id == null || result == null) {
                continue;
            }
            String method = methods.get(id.raw());
            if (method == null) {
                continue;
            }
            result = JsonText.expand(result);
            if (POPUPS.equals(method)) {
                JsonText config = result.fields.get("configJsonString");
                if (config != null) {
                    replace(edits, config, JSONObject.quote(blockOffers(config.string())));
                }
            } else if (PLUGINS.equals(method)) {
                disablePlugins(result, plugins, edits);

            }
        }
        return splice(response, edits);
    }

    private static void disablePlugins(JsonText plugins, Set<String> names, TreeMap<Integer, Replacement> edits)
            throws JSONException {
        for (JsonText plugin : plugins.values) {
            JsonText name = plugin.fields.get("name");
            JsonText enabled = plugin.fields.get("enabled");
            if (name != null && enabled != null && names.contains(name.string())) {
                replace(edits, enabled, "false");
            }
        }
    }

    static String blockOffers(String config) throws JSONException {
        JsonText states = JsonText.parse(config).fields.get("configuration_states");
        if (states == null) {
            return config;
        }
        TreeMap<Integer, Replacement> edits = new TreeMap<>();
        for (JsonText state : states.values) {
            JsonText block = state.fields.get("block_list");
            JsonText categories = (block != null) ? block.fields.get("categories") : null;
            if (categories != null && categories.text.charAt(categories.start) != '[') {
                throw new JSONException("Popup category list rejected");
            }
            StringBuilder additions = new StringBuilder();
            for (String category : OFFER_CATEGORIES) {
                boolean present = false;
                for (JsonText existing : (categories != null) ? categories.values : Collections.<JsonText>emptyList()) {
                    present |= category.equals(existing.string());
                }
                if (!present) {
                    if ((categories != null && !categories.values.isEmpty()) || additions.length() > 0) {
                        additions.append(',');
                    }
                    additions.append(JSONObject.quote(category));
                }
            }
            if (categories != null) {
                String original = categories.raw();
                replace(edits, categories, original.substring(0, original.length() - 1) + additions + "]");
            } else if (block != null) {
                replace(edits, block, appendField(block, "categories", "[" + additions + "]"));
            } else {
                replace(edits, state, appendField(state, "block_list", "{\"categories\":[" + additions + "]}"));
            }
        }
        return splice(config, edits);
    }

    private static String appendField(JsonText object, String key, String value) throws JSONException {
        if (object.text.charAt(object.start) != '{') {
            throw new JSONException("Popup object rejected");
        }
        String original = object.raw();
        return original.substring(0, original.length() - 1) + (object.fields.isEmpty() ? "" : ",")
                + JSONObject.quote(key) + ":" + value + "}";
    }

    private static void replace(TreeMap<Integer, Replacement> edits, JsonText value, String replacement) {
        edits.put(value.start, new Replacement(value.end, replacement));
    }

    private static String splice(String text, TreeMap<Integer, Replacement> edits) {
        StringBuilder output = new StringBuilder(text);
        for (Map.Entry<Integer, Replacement> edit : edits.descendingMap().entrySet()) {
            output.replace(edit.getKey(), edit.getValue().end, edit.getValue().text);
        }
        return output.toString();
    }

    private static boolean gzip(byte[] bytes) {
        return bytes.length > 1 && (bytes[0] & BYTE_MASK) == GZIP_MAGIC_FIRST
                && (bytes[1] & BYTE_MASK) == GZIP_MAGIC_SECOND;
    }

    private static String decode(byte[] bytes) throws IOException {
        InputStream input = new ByteArrayInputStream(bytes);
        if (gzip(bytes)) {
            input = new GZIPInputStream(input);
        }
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_BYTES];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (count > MAX_BYTES - output.size()) {
                    throw new IOException("Configuration payload exceeds byte limit");
                }
                output.write(buffer, 0, count);
            }
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(output.toByteArray()))
                .toString();
        }
    }

    private static final class Replacement {

        final int end;

        final String text;

        Replacement(int end, String text) {
            this.end = end;
            this.text = text;
        }

    }

}
