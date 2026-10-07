/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.raindrop;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class LinkStoreTest {

    private LinkStore store;

    private static Set<String> links(String... links) {
        return new HashSet<>(Arrays.asList(links));
    }

    @Before
    public void openStore() {
        this.store = new LinkStore(RuntimeEnvironment.getApplication(), 1);
    }

    @After
    public void closeStore() {
        this.store.close();
    }

    private void addPage(String link, String text) {
        this.store.recordHealth(link, null, 0, text);
    }

    @Test
    public void matchesEachWordAsAPrefix() {
        addPage("a", "hello world");
        addPage("b", "hello there");
        addPage("c", "help me");
        assertEquals(links("a", "b"), this.store.linksContaining("hello"));
        assertEquals(links("a", "b", "c"), this.store.linksContaining("hel"));
        assertEquals(links("a"), this.store.linksContaining("hello wor"));
        assertEquals(Collections.emptySet(), this.store.linksContaining("ello"));
    }

    @Test
    public void requiresEveryWordOfTheQuery() {
        addPage("a", "alpha beta");
        addPage("b", "alpha");
        addPage("c", "beta");
        assertEquals(links("a"), this.store.linksContaining("alpha beta"));
        assertEquals(links("a"), this.store.linksContaining("beta alpha"));
    }

    @Test
    public void ignoresTheCaseOfTheQuery() {
        addPage("a", "hello world");
        assertEquals(links("a"), this.store.linksContaining("Hello WORLD"));
        assertEquals(links("a"), this.store.linksContaining("HELLO"));
    }

    @Test
    public void splitsQueriesOnNonAlphanumericCharacters() {
        addPage("a", "foo bar baz");
        addPage("b", "snake case");
        addPage("c", "don t stop");
        assertEquals(links("a"), this.store.linksContaining("foo-bar, baz!"));
        assertEquals(links("b"), this.store.linksContaining("snake_case"));
        assertEquals(links("c"), this.store.linksContaining("don't"));
    }

    @Test
    public void ignoresSurplusSeparators() {
        addPage("a", "hi there");
        addPage("b", "a b");
        assertEquals(links("a"), this.store.linksContaining("  hi  "));
        assertEquals(links("b"), this.store.linksContaining("...a   ---   b..."));
    }

    @Test
    public void matchesNothingForEmptyOrSymbolOnlyQueries() {
        addPage("a", "hello world");
        assertEquals(Collections.emptySet(), this.store.linksContaining(""));
        assertEquals(Collections.emptySet(), this.store.linksContaining("   "));
        assertEquals(Collections.emptySet(), this.store.linksContaining("!!! --- ***"));
    }

    @Test
    public void treatsFullTextOperatorsAsPlainWords() {
        // given
        addPage("a", "hello or not near 3 x");
        addPage("b", "hello");
        addPage("c", "x");

        // when
        Set<String> matches = this.store.linksContaining("\"hello\" OR NOT NEAR/3 (x)");

        // then
        assertEquals(links("a"), matches);
    }

    @Test
    public void treatsDashedWordsAsInclusions() {
        // given
        addPage("a", "a b");
        addPage("b", "a");
        addPage("c", "b");

        // when
        Set<String> matches = this.store.linksContaining("a* -b");

        // then
        assertEquals(links("a"), matches);
    }

    @Test
    public void treatsColumnFiltersAsPlainWords() {
        // given
        addPage("a", "col body x");
        addPage("b", "x");

        // when
        Set<String> matches = this.store.linksContaining("col:body x");

        // then
        assertEquals(links("a"), matches);
    }

    @Test
    public void keepsDigits() {
        addPage("a", "2024 report v2");
        addPage("b", "report v2");
        assertEquals(links("a"), this.store.linksContaining("2024 report v2"));
        assertEquals(links("a"), this.store.linksContaining("202"));
        assertEquals(links("a", "b"), this.store.linksContaining("v2"));
    }

    @Test
    public void keepsLettersOfOtherScripts() {
        addPage("a", "Café ÜNÏ");
        addPage("b", "日本語 тест");
        assertEquals(links("a"), this.store.linksContaining("CafÉ ÜNÏ"));
        assertEquals(links("b"), this.store.linksContaining("日本語, ТЕСТ"));
    }

    @Test
    public void returnsTheSameLinksForTheSameQueryTwice() {
        addPage("a", "rust async await");
        addPage("b", "rust sync");
        Set<String> once = this.store.linksContaining("Rust async/await");
        assertEquals(links("a"), once);
        assertEquals(once, this.store.linksContaining("Rust async/await"));
    }

}
