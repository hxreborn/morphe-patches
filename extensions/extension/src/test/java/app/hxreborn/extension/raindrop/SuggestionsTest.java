/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class SuggestionsTest {

    private static JSONObject bookmark(long id, String domain, int collection, String title, String... tags)
            throws JSONException {
        return new JSONObject().put("_id", id)
            .put("domain", domain)
            .put("collectionId", collection)
            .put("title", title)
            .put("tags", new JSONArray(Arrays.asList(tags)));
    }

    private static JSONObject onDomain(long id, String domain, int collection, String... tags) throws JSONException {
        return bookmark(id, domain, collection, "", tags);
    }

    private static List<Integer> collectionsOf(JSONObject result) throws JSONException {
        JSONArray array = result.getJSONObject("item").getJSONArray("collections");
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            ids.add(array.getJSONObject(i).getInt("$id"));
        }
        return ids;
    }

    private static List<String> tagsOf(JSONObject result) throws JSONException {
        JSONArray array = result.getJSONObject("item").getJSONArray("tags");
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            tags.add(array.getString(i));
        }
        return tags;
    }

    private static JSONObject suggest(JSONObject bookmark, JSONObject... library) throws JSONException {
        return Suggestions.forBookmark(bookmark, Arrays.asList(library));
    }

    @Test
    public void yieldsEmptySuggestionsFromAnEmptyLibrary() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", 5));

        // then
        assertTrue(result.getBoolean("result"));
        assertEquals(Collections.emptyList(), collectionsOf(result));
        assertEquals(Collections.emptyList(), tagsOf(result));
        assertEquals(0, result.getJSONObject("item").getJSONArray("new_tags").length());
    }

    @Test
    public void toleratesBookmarksWithoutFields() throws JSONException {
        // when
        JSONObject result = suggest(new JSONObject(), new JSONObject(), new JSONObject().put("_id", 4));

        // then
        assertTrue(result.getBoolean("result"));
        assertEquals(Collections.emptyList(), collectionsOf(result));
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void suggestsCollectionsOfSameDomainBookmarks() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5),
                onDomain(3, "other.org", 8));

        // then
        assertEquals(Collections.singletonList(5), collectionsOf(result));
    }

    @Test
    public void ranksCollectionsByVotesThenFirstSeen() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5),
                onDomain(3, "example.com", 9), onDomain(4, "example.com", 9), onDomain(5, "example.com", 7));

        // then
        assertEquals(Arrays.asList(9, 5, 7), collectionsOf(result));
    }

    @Test
    public void limitsCollectionsToThree() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 10),
                onDomain(3, "example.com", 11), onDomain(4, "example.com", 12), onDomain(5, "example.com", 13));

        // then
        assertEquals(Arrays.asList(10, 11, 12), collectionsOf(result));
    }

    @Test
    public void skipsTheBookmarksOwnCollection() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", 5), onDomain(2, "example.com", 5),
                onDomain(3, "example.com", 6));

        // then
        assertEquals(Collections.singletonList(6), collectionsOf(result));
    }

    @Test
    public void omitsSystemCollections() throws JSONException {
        // given
        JSONObject withoutCollection = new JSONObject().put("_id", 5).put("domain", "example.com");

        // when
        JSONObject result = suggest(onDomain(1, "example.com", 3), onDomain(2, "example.com", 0),
                onDomain(3, "example.com", BookmarkIndex.UNSORTED), onDomain(4, "example.com", BookmarkIndex.TRASH),
                withoutCollection);

        // then
        assertEquals(Collections.emptyList(), collectionsOf(result));
    }

    @Test
    public void treatsABookmarkWithoutCollectionAsUnsorted() throws JSONException {
        // given
        JSONObject own = new JSONObject().put("_id", 1).put("domain", "example.com");

        // when
        JSONObject result = suggest(own, onDomain(2, "example.com", 4));

        // then
        assertEquals(Collections.singletonList(4), collectionsOf(result));
    }

    @Test
    public void ignoresCollectionsOfOtherDomains() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.org", 5),
                onDomain(3, "sub.example.com", 6));

        // then
        assertEquals(Collections.emptyList(), collectionsOf(result));
    }

    @Test
    public void matchesDomainsCaseInsensitivelyWithoutLeadingWww() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "WWW.Example.com", -1), onDomain(2, "example.com", 5),
                onDomain(3, "www.EXAMPLE.COM", 6));

        // then
        assertEquals(Arrays.asList(5, 6), collectionsOf(result));
    }

    @Test
    public void matchesNothingOnEmptyDomains() throws JSONException {
        // given
        JSONObject noDomain = new JSONObject().put("_id", 1).put("collectionId", -1);
        JSONObject otherWithoutDomain = new JSONObject().put("_id", 2)
            .put("collectionId", 5)
            .put("tags", new JSONArray().put("news"));

        // when
        JSONObject result = suggest(noDomain, otherWithoutDomain, onDomain(3, "", 6, "misc"));

        // then
        assertEquals(Collections.emptyList(), collectionsOf(result));
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void matchesNothingOnBareWwwDomains() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "www.", -1), onDomain(2, "www.", 5, "a"));

        // then
        assertEquals(Collections.emptyList(), collectionsOf(result));
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void suggestsTagsOfSameDomainBookmarks() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "news"),
                onDomain(3, "other.org", 5, "unrelated"));

        // then
        assertEquals(Collections.singletonList("news"), tagsOf(result));
    }

    @Test
    public void suggestsTagsWhoseWordAppearsInTheTitle() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "Learning Rust the hard way");

        // when
        JSONObject result = suggest(own, bookmark(2, "other.org", 5, "", "rust"),
                bookmark(3, "other.org", 5, "", "go"));

        // then
        assertEquals(Collections.singletonList("rust"), tagsOf(result));
    }

    @Test
    public void matchesTitleWordsCaseInsensitively() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "LEARNING RUST");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "Rust"));

        // then
        assertEquals(Collections.singletonList("Rust"), tagsOf(result));
    }

    @Test
    public void requiresWholeWordsInTitleMatches() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "Trusty rusty tools");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "rust"), onDomain(3, "other.org", 5, "rus"),
                onDomain(4, "other.org", 5, "tool"));

        // then
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void matchesAMultiWordTagAgainstTheTitlePhrase() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "An intro to machine learning");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "machine learning"),
                onDomain(3, "other.org", 5, "machine vision"));

        // then
        assertEquals(Collections.singletonList("machine learning"), tagsOf(result));
    }

    @Test
    public void skipsTitleMatchesWithoutATitle() throws JSONException {
        // given
        JSONObject own = new JSONObject().put("_id", 1).put("domain", "example.com");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "rust"));

        // then
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void skipsTagsTheBookmarkAlreadyHas() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "rust notes", "rust", "news");

        // when
        JSONObject result = suggest(own, onDomain(2, "example.com", 5, "rust", "news", "tech"));

        // then
        assertEquals(Collections.singletonList("tech"), tagsOf(result));
    }

    @Test
    public void ranksASameDomainTagAboveASingleTitleMatch() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "about zig");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "zig"), onDomain(3, "example.com", 5, "news"));

        // then
        assertEquals(Arrays.asList("news", "zig"), tagsOf(result));
    }

    @Test
    public void skipsTitleVotesForSameDomainTags() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "rust and zig");

        // when
        JSONObject result = suggest(own, onDomain(2, "other.org", 5, "zig"), onDomain(3, "other.org", 5, "zig"),
                onDomain(4, "example.com", 5, "rust"));

        // then
        assertEquals(Arrays.asList("zig", "rust"), tagsOf(result));
    }

    @Test
    public void accumulatesVotesAcrossBookmarks() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "a"),
                onDomain(3, "example.com", 5, "b"), onDomain(4, "example.com", 5, "b"));

        // then
        assertEquals(Arrays.asList("b", "a"), tagsOf(result));
    }

    @Test
    public void ranksEqualTagVotesByFirstSeen() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "c"),
                onDomain(3, "example.com", 5, "a"), onDomain(4, "example.com", 5, "b"));

        // then
        assertEquals(Arrays.asList("c", "a", "b"), tagsOf(result));
    }

    @Test
    public void limitsTagsToFive() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "t1"),
                onDomain(3, "example.com", 5, "t2"), onDomain(4, "example.com", 5, "t3"),
                onDomain(5, "example.com", 5, "t4"), onDomain(6, "example.com", 5, "t5"),
                onDomain(7, "example.com", 5, "t6"), onDomain(8, "example.com", 5, "t7"));

        // then
        assertEquals(Arrays.asList("t1", "t2", "t3", "t4", "t5"), tagsOf(result));
    }

    @Test
    public void countsRepeatedTagOfOneBookmarkOnce() throws JSONException {
        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "a", "a"),
                onDomain(3, "example.com", 5, "b"), onDomain(4, "example.com", 5, "b"));

        // then
        assertEquals(Arrays.asList("b", "a"), tagsOf(result));
    }

    @Test
    public void ignoresEmptyTagsOrMissingTagArrays() throws JSONException {
        // given
        JSONObject withoutTags = new JSONObject().put("_id", 3).put("domain", "example.com");

        // when
        JSONObject result = suggest(onDomain(1, "example.com", -1), onDomain(2, "example.com", 5, "", "real"),
                withoutTags);

        // then
        assertEquals(Collections.singletonList("real"), tagsOf(result));
    }

    @Test
    public void ignoresTheBookmarkItselfInTheLibrary() throws JSONException {
        // given
        JSONObject own = onDomain(1, "example.com", 3, "mine");

        // when
        JSONObject result = suggest(own, onDomain(1, "example.com", 9, "mine-too"), onDomain(2, "example.com", 4));

        // then
        assertEquals(Collections.singletonList(4), collectionsOf(result));
        assertEquals(Collections.emptyList(), tagsOf(result));
    }

    @Test
    public void returnsTheSameSuggestionsOnRepeatedCallsWithoutMutatingInputs() throws JSONException {
        // given
        JSONObject own = bookmark(1, "example.com", -1, "rust notes");
        JSONObject first = onDomain(2, "example.com", 5, "news");
        JSONObject second = bookmark(3, "other.org", 6, "", "rust");
        String ownBefore = own.toString();
        String firstBefore = first.toString();
        String secondBefore = second.toString();
        JSONObject once = suggest(own, first, second);

        // when
        JSONObject twice = suggest(own, first, second);

        // then
        assertEquals(collectionsOf(once), collectionsOf(twice));
        assertEquals(tagsOf(once), tagsOf(twice));
        assertEquals(Arrays.asList("news", "rust"), tagsOf(once));
        assertEquals(ownBefore, own.toString());
        assertEquals(firstBefore, first.toString());
        assertEquals(secondBefore, second.toString());
    }

}
