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
package app.hxreborn.extension.marks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class QuestionFormatFilter implements TypeAdapterFactory {

    private static volatile List<String> selectedFormats = Collections.emptyList();

    private final Class<?> questionsType;

    private final Class<?> filtersType;

    public QuestionFormatFilter(Class<?> questionsType, Class<?> filtersType) {
        this.questionsType = questionsType;
        this.filtersType = filtersType;
    }

    public static boolean select(List<String> formats) {
        if (formats == null || formats.isEmpty()) {
            selectedFormats = Collections.emptyList();
            return false;
        }
        if (formats.contains("all")) {
            selectedFormats = Collections.emptyList();
            return true;
        }
        selectedFormats = new ArrayList<>(formats);
        return true;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<?> rawType = type.getRawType();
        if (rawType != this.questionsType && rawType != this.filtersType) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<T>() {
            @Override
            public T read(JsonReader reader) throws IOException {
                JsonElement body = gson.fromJson(reader, JsonElement.class);
                if (body != null && body.isJsonObject()) {
                    if (rawType == QuestionFormatFilter.this.questionsType) {
                        filterQuestions(body.getAsJsonObject());
                    } else {
                        unlockFormats(body.getAsJsonObject());
                    }
                }
                return delegate.fromJsonTree(body);
            }

            @Override
            public void write(JsonWriter writer, T value) throws IOException {
                delegate.write(writer, value);
            }
        };
    }

    private static void filterQuestions(JsonObject body) {
        List<String> formats = selectedFormats;
        if (formats.isEmpty()) {
            return;
        }
        JsonObject data = object(body, "data");
        if (data == null || !data.has("questions") || !data.get("questions").isJsonArray()) {
            return;
        }
        JsonArray selected = new JsonArray();
        for (JsonElement question : data.getAsJsonArray("questions")) {
            if (!question.isJsonObject()) {
                continue;
            }
            JsonElement structure = question.getAsJsonObject().get("questionStructure");
            if (structure == null || !structure.isJsonArray()) {
                continue;
            }
            for (JsonElement value : structure.getAsJsonArray()) {
                if (value.isJsonPrimitive() && formats.contains(value.getAsString())) {
                    selected.add(question);
                    break;
                }
            }
        }
        data.add("questions", selected);
        data.addProperty("total", selected.size());
        data.addProperty("showing", selected.size());
        data.addProperty("isFilterApplied", true);
    }

    private static void unlockFormats(JsonObject body) {
        JsonObject data = object(body, "data");
        if (data == null) {
            return;
        }
        JsonObject filters = object(data, "filters");
        if (filters == null || !filters.has("sections") || !filters.get("sections").isJsonArray()) {
            return;
        }
        for (JsonElement section : filters.getAsJsonArray("sections")) {
            if (!section.isJsonObject()) {
                continue;
            }
            JsonObject sectionObject = section.getAsJsonObject();
            JsonElement key = sectionObject.get("key");
            if (key == null || !key.isJsonPrimitive() || !"questionStructure".equals(key.getAsString())
                    || !sectionObject.has("items")) {
                continue;
            }
            for (JsonElement item : sectionObject.getAsJsonArray("items")) {
                if (item.isJsonObject()) {
                    item.getAsJsonObject().addProperty("isPremium", false);
                }
            }
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            return null;
        }
        return value.getAsJsonObject();
    }

}
