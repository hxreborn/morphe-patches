/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class BookmarkIndexTest {

    private static String normalize(String host, int port, String path, String query) {
        String link = "https://" + host + ((port != -1) ? ":" + port : "") + ((path != null) ? path : "")
                + ((query != null) ? "?" + query : "");
        return BookmarkIndex.normalizeLink(link);
    }

    private static JSONObject inCollection(int collectionId) throws JSONException {
        return new JSONObject().put("_id", 1).put("collectionId", collectionId);
    }

    @Test
    public void includesEveryCollectionExceptTrashInTheAllCollection() throws JSONException {
        assertTrue(BookmarkIndex.inCollection(inCollection(12), BookmarkIndex.ALL));
        assertTrue(BookmarkIndex.inCollection(inCollection(BookmarkIndex.UNSORTED), BookmarkIndex.ALL));
        assertTrue(BookmarkIndex.inCollection(inCollection(BookmarkIndex.ALL), BookmarkIndex.ALL));
        assertFalse(BookmarkIndex.inCollection(inCollection(BookmarkIndex.TRASH), BookmarkIndex.ALL));
    }

    @Test
    public void matchesOnlyTheRequestedCollection() throws JSONException {
        assertTrue(BookmarkIndex.inCollection(inCollection(12), 12));
        assertFalse(BookmarkIndex.inCollection(inCollection(12), 13));
        assertFalse(BookmarkIndex.inCollection(inCollection(12), BookmarkIndex.UNSORTED));
        assertFalse(BookmarkIndex.inCollection(inCollection(12), BookmarkIndex.TRASH));
    }

    @Test
    public void matchesSystemCollectionsQueriedDirectly() throws JSONException {
        assertTrue(BookmarkIndex.inCollection(inCollection(BookmarkIndex.TRASH), BookmarkIndex.TRASH));
        assertTrue(BookmarkIndex.inCollection(inCollection(BookmarkIndex.UNSORTED), BookmarkIndex.UNSORTED));
        assertFalse(BookmarkIndex.inCollection(inCollection(BookmarkIndex.UNSORTED), BookmarkIndex.TRASH));
    }

    @Test
    public void countsABookmarkWithoutCollectionAsUnsorted() throws JSONException {
        JSONObject bookmark = new JSONObject().put("_id", 1);
        assertTrue(BookmarkIndex.inCollection(bookmark, BookmarkIndex.UNSORTED));
        assertTrue(BookmarkIndex.inCollection(bookmark, BookmarkIndex.ALL));
        assertFalse(BookmarkIndex.inCollection(bookmark, 12));
        assertFalse(BookmarkIndex.inCollection(bookmark, BookmarkIndex.TRASH));
    }

    @Test
    public void normalizesHostsToLowercaseWithoutLeadingWww() {
        assertEquals("example.com", normalize("WWW.Example.COM", -1, null, null));
        assertEquals("example.com", normalize("example.com", -1, null, null));
        assertEquals("wwwexample.com", normalize("wwwexample.com", -1, null, null));
        assertEquals("web.example.com", normalize("web.example.com", -1, null, null));
    }

    @Test
    public void dropsTrailingSlashesFromThePath() {
        assertEquals("example.com/a/b", normalize("example.com", -1, "/a/b", null));
        assertEquals("example.com/a/b", normalize("example.com", -1, "/a/b/", null));
        assertEquals("example.com/a/b", normalize("example.com", -1, "/a/b///", null));
        assertEquals("example.com", normalize("example.com", -1, "/", null));
        assertEquals("example.com", normalize("example.com", -1, "", null));
    }

    @Test
    public void keepsEncodedPathUntouched() {
        assertEquals("example.com/a%20b/C", normalize("example.com", -1, "/a%20b/C/", null));
    }

    @Test
    public void keepsThePortWhenPresent() {
        assertEquals("example.com:8080/a", normalize("example.com", 8080, "/a/", null));
        assertEquals("example.com/a", normalize("example.com", -1, "/a/", null));
    }

    @Test
    public void dropsUtmParametersKeepingTheRestInOrder() {
        assertEquals("example.com/a?id=7&page=2",
                normalize("example.com", -1, "/a", "utm_source=news&id=7&utm_medium=mail&page=2&utm_campaign=x"));
    }

    @Test
    public void omitsTheQuestionMarkWhenNoParameterSurvives() {
        assertEquals("example.com/a", normalize("example.com", -1, "/a", "utm_source=x&utm_term=y"));
        assertEquals("example.com/a", normalize("example.com", -1, "/a", ""));
        assertEquals("example.com/a", normalize("example.com", -1, "/a", "&&"));
    }

    @Test
    public void dropsEmptyParametersBetweenSeparators() {
        assertEquals("example.com/a?x=1&y=2", normalize("example.com", -1, "/a", "&x=1&&y=2&"));
    }

    @Test
    public void dropsOnlyParametersStartingWithUtmUnderscore() {
        assertEquals("example.com/a?xutm_source=1&utm=2&source=utm_x",
                normalize("example.com", -1, "/a", "xutm_source=1&utm=2&source=utm_x"));
    }

    @Test
    public void sharesAKeyOnlyAcrossEquivalentLinks() {
        String plain = normalize("example.com", -1, "/post/1", "id=3");
        assertEquals(plain, normalize("www.EXAMPLE.com", -1, "/post/1/", "id=3&utm_source=feed"));
        assertNotEquals(plain, normalize("example.com", -1, "/post/2", "id=3"));
        assertNotEquals(plain, normalize("example.com", -1, "/post/1", "id=4"));
        assertNotEquals(plain, normalize("example.org", -1, "/post/1", "id=3"));
        assertNotEquals(plain, normalize("example.com", 8443, "/post/1", "id=3"));
    }

    @Test
    public void reportsNothingFromAnEmptyIndex() {
        assertFalse(BookmarkIndex.isLoaded());
        assertEquals(Collections.emptyList(), BookmarkIndex.snapshot());
        assertNull(BookmarkIndex.find(1));
        assertNull(BookmarkIndex.linkOf(1));
        assertEquals(0, BookmarkIndex.duplicateCount());
        assertEquals(0, BookmarkIndex.originalOf(1));
    }

    @Test
    public void ignoresObservedBookmarksUntilTheIndexIsLoaded() throws JSONException {
        JSONObject bookmark = new JSONObject().put("_id", 42).put("link", "https://example.com/a");
        BookmarkIndex.observe(Arrays.asList(bookmark));
        BookmarkIndex.observe(Collections.<JSONObject>emptyList());
        assertFalse(BookmarkIndex.isLoaded());
        assertEquals(Collections.emptyList(), BookmarkIndex.snapshot());
        assertNull(BookmarkIndex.find(42));
        assertNull(BookmarkIndex.linkOf(42));
        assertEquals(0, BookmarkIndex.duplicateCount());
    }

}
