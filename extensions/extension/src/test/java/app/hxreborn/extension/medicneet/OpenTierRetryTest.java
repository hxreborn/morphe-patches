/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.medicneet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class OpenTierRetryTest {

    private static final String QUESTIONS = "subjects/Biology/classes/Class 11/topics/Leaf/segments/Memory based/questions";

    private static final String SEGMENT = "subjects/Biology/classes/Class 11/topics/Leaf/segments/Memory based";

    private static final Object TIER_FIELD = "tier-field";

    private static final Exception DENIED = new Exception(
            new RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions."));

    @Test
    public void addsTheOpenTierFilterWhenAQuestionListIsDenied() {
        // given
        final List<List<Object>> where = new ArrayList<>();

        // when
        final boolean retry = OpenTierRetry.shouldRetry(DENIED, QUESTIONS, where, TIER_FIELD);

        // then
        assertTrue(retry);
        assertEquals(1, where.size());
        assertEquals(Arrays.<Object>asList(TIER_FIELD, "in", Arrays.asList(1L, 2L)), where.get(0));
    }

    @Test
    public void retriesOnlyOnce() {
        // given
        final List<List<Object>> where = new ArrayList<>();
        OpenTierRetry.shouldRetry(DENIED, QUESTIONS, where, TIER_FIELD);

        // when
        final boolean again = OpenTierRetry.shouldRetry(DENIED, QUESTIONS, where, TIER_FIELD);

        // then
        assertFalse(again);
        assertEquals(1, where.size());
    }

    @Test
    public void keepsTheClausesTheAppAlreadyAsked() {
        // given
        final List<List<Object>> where = new ArrayList<>();
        where.add(Arrays.<Object>asList("concept", "==", "leaf"));

        // when
        OpenTierRetry.shouldRetry(DENIED, QUESTIONS, where, TIER_FIELD);

        // then
        assertEquals(2, where.size());
        assertEquals("concept", where.get(0).get(0));
    }

    @Test
    public void ignoresFailuresThatAreNotPermissionDenied() {
        // given
        final List<List<Object>> where = new ArrayList<>();

        // when
        final boolean retry = OpenTierRetry.shouldRetry(new Exception("UNAVAILABLE"), QUESTIONS, where, TIER_FIELD);

        // then
        assertFalse(retry);
        assertTrue(where.isEmpty());
    }

    @Test
    public void ignoresCollectionsThatAreNotQuestionLists() {
        // given
        final List<List<Object>> where = new ArrayList<>();

        // when
        final boolean retry = OpenTierRetry.shouldRetry(DENIED, SEGMENT + "/cards", where, TIER_FIELD);

        // then
        assertFalse(retry);
        assertTrue(where.isEmpty());
    }

    @Test
    public void ignoresAQueryWithoutParameters() {
        assertFalse(OpenTierRetry.shouldRetry(DENIED, QUESTIONS, null, TIER_FIELD));
    }

    @Test
    public void ignoresAClauseListThatCannotGrow() {
        assertFalse(OpenTierRetry.shouldRetry(DENIED, QUESTIONS, Collections.emptyList(), TIER_FIELD));
    }

    @Test
    public void answersADeniedSegmentDocument() {
        assertTrue(OpenTierRetry.isOpenSegmentRead(DENIED, SEGMENT));
    }

    @Test
    public void leavesADeniedQuestionListToTheRetry() {
        assertFalse(OpenTierRetry.isOpenSegmentRead(DENIED, QUESTIONS));
    }

    @Test
    public void leavesADeniedDocumentInsideASegmentAlone() {
        assertFalse(OpenTierRetry.isOpenSegmentRead(DENIED, SEGMENT + "/cards/c1"));
    }

    @Test
    public void leavesOtherDeniedDocumentsAlone() {
        assertFalse(OpenTierRetry.isOpenSegmentRead(DENIED, "payments/abc"));
    }

    @Test
    public void leavesOtherFailuresOfASegmentDocumentAlone() {
        assertFalse(OpenTierRetry.isOpenSegmentRead(new Exception("UNAVAILABLE"), SEGMENT));
    }

}
