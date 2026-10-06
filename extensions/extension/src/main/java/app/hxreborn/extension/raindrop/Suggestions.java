/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Suggestions {

    private static final int MAX_COLLECTIONS = 3;

    private static final int MAX_TAGS = 5;

    private Suggestions() {
    }

    static JSONObject forBookmark(JSONObject bookmark, List<JSONObject> library) throws JSONException {
        long id = bookmark.optLong("_id");
        String domain = domainOf(bookmark);
        int ownCollection = bookmark.optInt("collectionId", BookmarkIndex.UNSORTED);
        Set<String> ownTags = tagsOf(bookmark);
        String title = " " + bookmark.optString("title").toLowerCase(Locale.ROOT) + " ";

        Map<Integer, Integer> collectionVotes = new LinkedHashMap<>();
        Map<String, Integer> tagVotes = new LinkedHashMap<>();
        for (JSONObject other : library) {
            if (other.optLong("_id") == id) {
                continue;
            }
            boolean sameDomain = !domain.isEmpty() && domain.equals(domainOf(other));
            if (sameDomain) {
                int collection = other.optInt("collectionId", BookmarkIndex.UNSORTED);
                if (collection > 0 && collection != ownCollection) {
                    increment(collectionVotes, collection, 1);
                }
            }
            for (String tag : tagsOf(other)) {
                if (ownTags.contains(tag)) {
                    continue;
                }
                if (sameDomain) {
                    increment(tagVotes, tag, 2);
                } else if (title.contains(" " + tag.toLowerCase(Locale.ROOT) + " ")) {
                    increment(tagVotes, tag, 1);
                }
            }
        }

        JSONArray collections = new JSONArray();
        for (Integer collection : topKeys(collectionVotes, MAX_COLLECTIONS)) {
            collections.put(new JSONObject().put("$id", collection));
        }
        JSONArray tags = new JSONArray();
        for (String tag : topKeys(tagVotes, MAX_TAGS)) {
            tags.put(tag);
        }
        JSONObject item = new JSONObject().put("collections", collections)
            .put("tags", tags)
            .put("new_tags", new JSONArray());
        return new JSONObject().put("result", true).put("item", item);
    }

    private static String domainOf(JSONObject bookmark) {
        String domain = bookmark.optString("domain").toLowerCase(Locale.ROOT);
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private static Set<String> tagsOf(JSONObject bookmark) {
        Set<String> tags = new HashSet<>();
        JSONArray array = bookmark.optJSONArray("tags");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String tag = array.optString(i);
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private static <K> void increment(Map<K, Integer> votes, K key, int weight) {
        Integer current = votes.get(key);
        votes.put(key, ((current != null) ? current : 0) + weight);
    }

    private static <K> List<K> topKeys(final Map<K, Integer> votes, int limit) {
        List<K> keys = new ArrayList<>(votes.keySet());
        Collections.sort(keys, (left, right) -> Integer.compare(votes.get(right), votes.get(left)));
        return (keys.size() > limit) ? keys.subList(0, limit) : keys;
    }

}
