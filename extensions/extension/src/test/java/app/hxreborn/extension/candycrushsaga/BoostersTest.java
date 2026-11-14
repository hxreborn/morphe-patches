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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class BoostersTest {

    private static final int[] ITEMS = { 3101, 3105, 3106, 3108, 3118, 3122, 3124, 3240, 3241, 3257 };

    private static final int AMOUNT = 1000;

    private static final int HAMMER_INDEX = 1;

    private static final int PAYLOAD_LIMIT = 16777216;

    private static final int REQUEST_LIMIT = 64;

    private static final int INELIGIBLE_ID_LIMIT = 4096;

    private static final int INELIGIBLE_CHARACTER_LIMIT = 262144;

    private static final int ATTEMPT_LIMIT = 12;

    private static final String TRANSACTION_GROUP = "current";

    private static final String OLD_TRANSACTION_GROUP = "older";

    private static final int REWARD_TYPE = 5503;

    private static final int HELD_REWARD_TYPE = 402;

    private static final int FIRST_SPLIT_COUNT = 6;

    private static final int LARGE_PAYLOAD_BYTES = 2097152;

    private static final int EXCESSIVE_DEPTH = 20000;

    private static final String PRIVATE_MARKER = "PRIVATE_INVENTORY_MARKER";

    private BoosterGrant grant;

    @Before
    public void reset() {
        this.grant = new BoosterGrant(ITEMS, AMOUNT);
        ShadowLog.clear();
        BlockingJsonTokener.pause.set(false);
        BlockingJsonTokener.parsing = new CountDownLatch(1);
        BlockingJsonTokener.resume = new CountDownLatch(1);
    }

    @Test
    public void ignoresOneHammerBeforeAcceptingTheCurrentGrant() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        JSONArray unrelated = calls(request(8, PRIVATE_MARKER, REWARD_TYPE, HAMMER_INDEX, HAMMER_INDEX + 1));
        update(unrelated).put("p", new JSONObject().put("value", 1).toString());
        observe(41, bytes(unrelated.toString()), response(8, PRIVATE_MARKER, HAMMER_INDEX, HAMMER_INDEX + 1, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        acceptCurrentGrant();
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    public void ignoresAnItemOutsideTheConfiguredSet() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        JSONArray unrelated = calls(request(8, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        update(unrelated).getJSONObject("h").put("i", 3102);
        update(unrelated).put("p", deepJson());
        observe(41, bytes(unrelated.toString()), response(8, PRIVATE_MARKER, 0, 1, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        acceptCurrentGrant();
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    public void excludesOldTransactionIdsResentUnderANewRequestId() throws Exception {
        ready();
        byte[] oldRequest = request(8, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length);
        this.grant.onRequest(41, oldRequest);
        assertTrue(this.grant.reserve());
        assertTrue(this.grant.begin());
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, oldRequest, response(8, PRIVATE_MARKER, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        acceptCurrentGrant();
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    @Config(shadows = BlockingJsonTokener.class)
    public void excludesResentIdsWhenTheirEarlierParseFinishesAfterSubmission() throws Exception {
        ready();
        byte[] oldRequest = request(8, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length);
        byte[] oldResponse = response(8, PRIVATE_MARKER, 0, ITEMS.length, 0);
        assertTrue(this.grant.reserve());
        BlockingJsonTokener.pause.set(true);
        FutureTask<Void> earlier = new FutureTask<>(() -> {
            this.grant.onRequest(41, oldRequest);
            return null;
        });
        new Thread(earlier).start();
        FutureTask<Void> resend = null;
        try {
            assertTrue(BlockingJsonTokener.parsing.await(5, TimeUnit.SECONDS));
            assertTrue(this.grant.begin());
            this.grant.submitted(Boosters.SUBMITTED);
            CountDownLatch entered = new CountDownLatch(1);
            resend = new FutureTask<>(() -> {
                entered.countDown();
                observe(42, oldRequest, oldResponse);
                return null;
            });
            new Thread(resend).start();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(earlier.isDone());
        } finally {
            BlockingJsonTokener.resume.countDown();
        }
        earlier.get(5, TimeUnit.SECONDS);
        assertTrue(resend != null);
        resend.get(5, TimeUnit.SECONDS);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        acceptCurrentGrant();
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    @Config(shadows = BlockingJsonTokener.class)
    public void retainsIneligibilityWhileARequestWaitsForAnotherParse() throws Exception {
        ready();
        byte[] firstRequest = inventoryRequest(6, new JSONArray());
        byte[] oldRequest = request(8, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length);
        assertTrue(this.grant.reserve());
        BlockingJsonTokener.pause.set(true);
        FutureTask<Void> first = new FutureTask<>(() -> {
            this.grant.onRequest(40, firstRequest);
            return null;
        });
        FutureTask<Void> queued = new FutureTask<>(() -> {
            this.grant.onRequest(41, oldRequest);
            return null;
        });
        Thread waiting = new Thread(queued);
        new Thread(first).start();
        try {
            assertTrue(BlockingJsonTokener.parsing.await(5, TimeUnit.SECONDS));
            waiting.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (waiting.getState() != Thread.State.BLOCKED && !queued.isDone() && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(Thread.State.BLOCKED, waiting.getState());
            assertTrue(this.grant.begin());
            this.grant.submitted(Boosters.SUBMITTED);
        } finally {
            BlockingJsonTokener.resume.countDown();
        }
        first.get(5, TimeUnit.SECONDS);
        queued.get(5, TimeUnit.SECONDS);
        observe(42, oldRequest, response(8, PRIVATE_MARKER, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        acceptCurrentGrant();
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    public void warnsOnceForANullRequest() throws Exception {
        begin();
        this.grant.onRequest(42, null);
        this.grant.onRequest(43, null);
        assertUnknownWarning("Inventory request size rejected, bytes=0, limit=" + PAYLOAD_LIMIT);
    }

    @Test
    public void warnsOnceForAnOversizeRequest() throws Exception {
        begin();
        this.grant.onRequest(42, new byte[PAYLOAD_LIMIT + 1]);
        assertUnknownWarning(
                "Inventory request size rejected, bytes=" + (PAYLOAD_LIMIT + 1) + ", limit=" + PAYLOAD_LIMIT);
    }

    @Test
    public void warnsOnceForAnInvalidResponseChunk() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length));
        this.grant.onData(42, new byte[1], 2);
        this.grant.onData(42, null, 0);
        this.grant.onResponse(42);
        assertUnknownWarning(
                "Inventory response chunk rejected, buffered=0, bytes=1, length=2, limit=" + PAYLOAD_LIMIT);
    }

    @Test
    public void warnsOnceWhenResponseChunksExceedTheByteLimit() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length));
        byte[] chunk = new byte[LARGE_PAYLOAD_BYTES];
        for (int index = 0; index < PAYLOAD_LIMIT / chunk.length; index++) {
            this.grant.onData(42, chunk, chunk.length);
        }
        this.grant.onData(42, new byte[1], 1);
        this.grant.onResponse(42);
        assertUnknownWarning("Inventory response chunk rejected, buffered=" + PAYLOAD_LIMIT
                + ", bytes=1, length=1, limit=" + PAYLOAD_LIMIT);
    }

    @Test
    public void warnsOnceWhenTheRequestMapIsFull() throws Exception {
        begin();
        for (int index = 0; index <= REQUEST_LIMIT; index++) {
            this.grant.onRequest(index, inventoryRequest(7, new JSONArray()));
        }
        assertUnknownWarning("Inventory request limit reached, pending=" + REQUEST_LIMIT + ", limit=" + REQUEST_LIMIT);
    }

    @Test
    public void warnsOnceForADuplicateHttpRequestId() throws Exception {
        begin();
        byte[] body = request(7, PRIVATE_MARKER, REWARD_TYPE, 0, ITEMS.length);
        this.grant.onRequest(42, body);
        this.grant.onRequest(42, body);
        assertUnknownWarning("Duplicate inventory request, pending=1");
    }

    @Test
    public void boundsTheNumberOfIneligibleIdsWithoutEviction() throws Exception {
        JSONArray entries = new JSONArray();
        for (int index = 0; index <= INELIGIBLE_ID_LIMIT; index++) {
            entries.put(new JSONObject().put("id", PRIVATE_MARKER + index));
        }
        this.grant.onRequest(41, inventoryRequest(7, entries));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertWarningCount(1);
        assertTrue(ShadowLog.getLogsForTag(Boosters.LOG_TAG).get(0).msg
            .startsWith("Inventory identity limit reached, count=" + INELIGIBLE_ID_LIMIT));
        assertPrivateMarkerAbsent();
        assertFalse(this.grant.reserve());
    }

    @Test
    public void boundsTheCombinedLengthOfIneligibleIds() throws Exception {
        char[] characters = new char[INELIGIBLE_CHARACTER_LIMIT + 1];
        Arrays.fill(characters, 'x');
        JSONArray entries = new JSONArray().put(new JSONObject().put("id", new String(characters)));
        this.grant.onRequest(41, inventoryRequest(7, entries));
        assertUnknownWarning("Inventory identity limit reached, count=0, limit=" + INELIGIBLE_ID_LIMIT
                + ", characters=0, characterLimit=" + INELIGIBLE_CHARACTER_LIMIT);
    }

    @Test
    public void rejectsChangedMetadataForAnAttributedId() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        this.grant.onRequest(43, request(8, PRIVATE_MARKER, HELD_REWARD_TYPE, 0, 1));
        assertUnknownWarning("Reward metadata changed, transactions=1");
    }

    @Test
    public void rejectsChangedAmountForAnAttributedId() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        JSONArray changed = calls(request(8, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        update(changed).put("p", new JSONObject().put("value", 1).toString());
        this.grant.onRequest(43, bytes(changed.toString()));
        assertUnknownWarning("Reward content changed, transactions=1");
    }

    @Test
    public void rejectsMissingUpdatesForAnAttributedId() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        JSONArray changed = calls(request(8, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        transaction(changed).remove("u");
        this.grant.onRequest(43, bytes(changed.toString()));
        assertUnknownWarning("Reward content changed, transactions=1");
    }

    @Test
    public void warnsForAMatchingRewardWithoutAnId() throws Exception {
        begin();
        JSONArray changed = calls(request(7, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        transaction(changed).remove("id");
        this.grant.onRequest(42, bytes(changed.toString()));
        assertUnknownWarning("Reward transaction identifier missing, updates=1");
    }

    @Test
    public void warnsForAnAmbiguousMatchingReward() throws Exception {
        begin();
        this.grant.onRequest(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, 1));
        this.grant.onRequest(43, request(8, PRIVATE_MARKER, REWARD_TYPE, 0, 1));
        assertUnknownWarning("Ambiguous reward transaction, transactions=1");
    }

    @Test
    public void warnsWhenSubmissionAttemptsReachTheirLimit() throws Exception {
        ready();
        for (int index = 0; index < ATTEMPT_LIMIT; index++) {
            assertTrue(this.grant.reserve());
            assertTrue(this.grant.begin());
            this.grant.submitted(Boosters.NOT_SUBMITTED);
        }
        assertFalse(this.grant.reserve());
        assertUnknownWarning("Booster submission limit reached, limit=" + ATTEMPT_LIMIT);
    }

    @Test
    public void completesAfterEveryMatchingTransactionIsAccepted() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void ignoresOlderIdenticalRewardsWhenCurrentDeliveryFails() throws Exception {
        ready();
        this.grant.onRequest(41, request(6, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length));
        assertTrue(this.grant.reserve());
        assertTrue(this.grant.begin());
        this.grant.submitted(Boosters.SUBMITTED);
        byte[] oldResponse = response(6, OLD_TRANSACTION_GROUP, 0, ITEMS.length, 0);
        this.grant.onData(41, oldResponse, oldResponse.length);
        this.grant.onResponse(41);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length, 7));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Reward acknowledgement rejected, rC=7, expected=10, replies=10");
    }

    @Test
    public void acceptsSplitAcknowledgementsAndExcludesHeldRewards() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(40, request(90, TRANSACTION_GROUP, HELD_REWARD_TYPE, 0, ITEMS.length),
                response(90, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(41, request(91, TRANSACTION_GROUP, REWARD_TYPE, 0, FIRST_SPLIT_COUNT),
                response(91, TRANSACTION_GROUP, 0, FIRST_SPLIT_COUNT, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(43, request(93, TRANSACTION_GROUP, REWARD_TYPE, FIRST_SPLIT_COUNT, ITEMS.length),
                response(93, TRANSACTION_GROUP, FIRST_SPLIT_COUNT, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void acceptsOnePlusNineAcknowledgements() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(41, request(91, TRANSACTION_GROUP, REWARD_TYPE, 0, 1), response(91, TRANSACTION_GROUP, 0, 1, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(43, request(93, TRANSACTION_GROUP, REWARD_TYPE, 1, ITEMS.length),
                response(93, TRANSACTION_GROUP, 1, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void ignoresRewardsObservedWhileSubmissionIsQueued() throws Exception {
        ready();
        assertTrue(this.grant.reserve());
        this.grant.onRequest(41, request(6, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length));
        assertTrue(this.grant.begin());
        this.grant.submitted(Boosters.SUBMITTED);
        byte[] oldResponse = response(6, OLD_TRANSACTION_GROUP, 0, ITEMS.length, 0);
        this.grant.onData(41, oldResponse, oldResponse.length);
        this.grant.onResponse(41);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void logsOnlyAcceptanceAtInfoForInventoryRequests() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        int count = 0;
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(Boosters.LOG_TAG)) {
            if (item.type == Log.INFO) {
                assertEquals("Booster transactions accepted, rC=0, count=10", item.msg);
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    public void logsAnUnacknowledgedTransaction() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length - 1, 0));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Reward transactions unacknowledged, rC=0, expected=10, accepted=9");
    }

    @Test
    public void logsAMissingTransactionReplyArray() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                bytes(new JSONArray().put(new JSONObject().put("id", 7).put("result", new JSONObject().put("rC", 0)))
                    .toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Reward acknowledgement rejected, rC=0, expected=10, replies=0");
    }

    @Test
    public void logsAMissingRpcReply() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), bytes("[]"));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Reward RPC reply missing, expected=1, received=0");
    }

    @Test
    public void completesWhenAcknowledgementPrecedesNativeReturn() throws Exception {
        begin();
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.SUBMITTING, this.grant.state());
        this.grant.submitted(Boosters.SUBMITTED);
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void preventsAnotherSubmissionWhenAWaitingCallbackResumes() throws Exception {
        begin();
        AtomicInteger submissions = new AtomicInteger(1);
        CountDownLatch entered = new CountDownLatch(1);
        Thread callback = new Thread(() -> {
            entered.countDown();
            if (this.grant.reserve()) {
                submissions.incrementAndGet();
            }
        });
        synchronized (this.grant) {
            callback.start();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                    response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
            this.grant.submitted(Boosters.SUBMITTED);
        }
        callback.join(5000);
        assertFalse(callback.isAlive());
        assertEquals(1, submissions.get());
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void acceptsInitialInventoryWithPendingTransactions() throws Exception {
        ready();
        assertEquals(BoosterGrant.State.READY, this.grant.state());
    }

    @Test
    public void readsCompressedResponsesLargerThanOneMebibyte() throws Exception {
        byte[] padding = new byte[LARGE_PAYLOAD_BYTES];
        Arrays.fill(padding, (byte) ' ');
        byte[] reply = bytes(new JSONArray()
            .put(new JSONObject().put("id", 7).put("result", new JSONObject().put("rC", 0).put("sI", new JSONObject())))
            .toString() + new String(padding, StandardCharsets.UTF_8));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(reply);
        }
        observe(42, request(7, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), compressed.toByteArray());
        assertEquals(BoosterGrant.State.READY, this.grant.state());
    }

    @Test
    public void rejectsDeepRequestsThroughTheCheckedFailurePath() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        this.grant.onRequest(42, bytes(deepJson()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
    }

    @Test
    public void boundsNestingAfterAnUnquotedKeyWithUnicodeWhitespace() throws Exception {
        begin();
        this.grant.onRequest(42, bytes("{\u2000\"x:" + deepJson() + "\"}"));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
    }

    @Test
    public void rejectsDeepResponsesWithoutLeavingAwaiting() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), bytes(deepJson()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory response could not be read");
    }

    @Test
    public void rejectsDeepNestedUpdatePayloads() throws Exception {
        begin();
        JSONArray calls = new JSONArray(
                new String(request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), StandardCharsets.UTF_8));
        update(calls).put("p", deepJson());
        this.grant.onRequest(42, bytes(calls.toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
    }

    @Test
    public void rejectsArrowAssignmentInRequests() throws Exception {
        rejectRequestSeparator("=>");
    }

    @Test
    public void rejectsArrowColonInRequests() throws Exception {
        rejectRequestSeparator(":>");
    }

    @Test
    public void rejectsArrowAssignmentInAssociatedResponses() throws Exception {
        rejectResponseSeparator("=>");
    }

    @Test
    public void rejectsArrowColonInAssociatedResponses() throws Exception {
        rejectResponseSeparator(":>");
    }

    @Test
    public void rejectsArrowAssignmentInNestedUpdates() throws Exception {
        rejectUpdateSeparator("=>");
    }

    @Test
    public void rejectsArrowColonInNestedUpdates() throws Exception {
        rejectUpdateSeparator(":>");
    }

    @Test
    public void ignoresBracketsAndEscapesInsideStrings() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        JSONArray calls = new JSONArray(
                new String(request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), StandardCharsets.UTF_8));
        update(calls).put("p",
                new JSONObject().put("value", AMOUNT).put("text", "=> :> [\\\"" + deepJson() + "\\\"]").toString());
        observe(42, bytes(calls.toString()), response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void excludesPrivateMarkersFromRequestParseLogs() throws Exception {
        begin();
        this.grant.onRequest(42, bytes("[{\"sK\":\"" + PRIVATE_MARKER + "\",\"broken\":}]"));
        assertLog("Inventory request could not be read");
        assertPrivateMarkerAbsent();
    }

    @Test
    public void excludesPrivateMarkersFromResponseParseLogs() throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                bytes("[{\"sK\":\"" + PRIVATE_MARKER + "\",\"broken\":}]"));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory response could not be read");
        assertPrivateMarkerAbsent();
    }

    @Test
    public void excludesPrivateMarkersFromNestedPayloadParseLogs() throws Exception {
        begin();
        JSONArray calls = new JSONArray(
                new String(request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), StandardCharsets.UTF_8));
        update(calls).put("p", "{\"secret\":\"" + PRIVATE_MARKER + "\",\"value\":}");
        this.grant.onRequest(42, bytes(calls.toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
        assertPrivateMarkerAbsent();
    }

    private void rejectRequestSeparator(String separator) throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        this.grant.onRequest(42, bytes(separatorPayload(separator)));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
    }

    private void rejectResponseSeparator(String separator) throws Exception {
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        observe(42, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), bytes(separatorPayload(separator)));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory response could not be read");
    }

    private void rejectUpdateSeparator(String separator) throws Exception {
        begin();
        JSONArray calls = new JSONArray(
                new String(request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), StandardCharsets.UTF_8));
        update(calls).put("p", separatorPayload(separator));
        this.grant.onRequest(42, bytes(calls.toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog("Inventory request could not be read");
    }

    private static String separatorPayload(String separator) {
        return "[{x" + separator + "\":\",y:" + deepJson() + ",z" + separator + "\":x\"}]";
    }

    private void acceptCurrentGrant() throws Exception {
        observe(44, request(9, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(9, TRANSACTION_GROUP, 0, ITEMS.length, 0));
    }

    private static JSONArray calls(byte[] bytes) throws Exception {
        return new JSONArray(new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void excludesRequestReservedBeforeAnotherObserverFinishesParsing() throws Exception {
        ready();
        int observation = this.grant.reserveObservation();
        assertTrue(this.grant.reserve());
        assertTrue(this.grant.begin());
        this.grant.submitted(Boosters.SUBMITTED);
        byte[] oldRequest = request(7, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length);
        this.grant.onRequest(42, oldRequest, observation);
        observe(43, oldRequest, response(7, OLD_TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        observe(44, request(8, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                response(8, TRANSACTION_GROUP, 0, ITEMS.length, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void acceptsTimedLivesDurationAddedToExistingTime() throws Exception {
        observeTimedLives(12000, 98400);
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertLog("Lives transactions accepted, rC=0, count=1");
    }

    @Test
    public void ignoresTimedLivesWithAnotherDuration() throws Exception {
        observeTimedLives(12000, 98401);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
    }

    @Test
    public void acceptsTimedLivesWithoutExistingTime() throws Exception {
        observeTimedLives(0, 86400);
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void ignoresTimedLivesWithNegativePreviousDuration() throws Exception {
        observeTimedLives(Integer.MIN_VALUE, Integer.MIN_VALUE + 86400L);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
    }

    private void observeTimedLives(long oldValue, long value) throws Exception {
        this.grant = new BoosterGrant(new int[] { 3646 }, 86400, true);
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
        JSONArray calls = timedLivesCalls(oldValue, value);
        JSONArray replies = new JSONArray(new String(response(7, TRANSACTION_GROUP, 0, 1, 0), StandardCharsets.UTF_8));
        replies.getJSONObject(0)
            .getJSONObject("result")
            .getJSONArray("tR")
            .put(new JSONObject().put("tId", "timer-start").put("tR", 1));
        observe(42, bytes(calls.toString()), bytes(replies.toString()));
    }

    private static JSONArray timedLivesCalls(long oldValue, long value) throws Exception {
        JSONArray calls = calls(request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, 1));
        update(calls).getJSONObject("h").put("i", 3646);
        update(calls).put("p",
                new JSONObject().put("oldValue", oldValue).put("value", value).put("strategy", 2).toString());
        JSONObject timerStart = new JSONObject().put("id", "timer-start")
            .put("m", new JSONObject().put("transactionType", REWARD_TYPE))
            .put("u", new JSONArray().put(new JSONObject().put("h", new JSONObject().put("i", 3646))
                .put("fT", 17002)
                .put("p", new JSONObject().put("oldValue", 0).put("value", 1794502628).put("strategy", 2).toString())));
        calls.getJSONObject(0).getJSONArray("params").getJSONObject(0).getJSONArray("t").put(timerStart);
        return calls;
    }

    @Test
    public void rejectsChangedOwnedTimerStart() throws Exception {
        beginTimedLives();
        JSONArray original = timedLivesCalls(0, 86400);
        this.grant.onRequest(42, bytes(original.toString()));
        JSONObject timestamp = original.getJSONObject(0)
            .getJSONArray("params")
            .getJSONObject(0)
            .getJSONArray("t")
            .getJSONObject(1)
            .getJSONArray("u")
            .getJSONObject(0);
        timestamp.put("p", new JSONObject().put("oldValue", 0).put("value", 1).put("strategy", 2).toString());
        this.grant.onRequest(43, bytes(original.toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
    }

    @Test
    public void rejectsChangedOwnedTimerDurationWithSameDifference() throws Exception {
        beginTimedLives();
        this.grant.onRequest(42, bytes(timedLivesCalls(0, 86400).toString()));
        this.grant.onRequest(43, bytes(timedLivesCalls(1, 86401).toString()));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
    }

    @Test
    public void waitsForTimerStartAcknowledgement() throws Exception {
        beginTimedLives();
        JSONArray duration = timedLivesCalls(0, 86400);
        duration.getJSONObject(0).getJSONArray("params").getJSONObject(0).getJSONArray("t").remove(1);
        observe(42, bytes(duration.toString()), response(7, TRANSACTION_GROUP, 0, 1, 0));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
    }

    @Test
    public void acceptsTimedLivesAcrossSeparateReplies() throws Exception {
        beginTimedLives();
        JSONArray timestamp = timedLivesCalls(0, 86400);
        timestamp.getJSONObject(0).getJSONArray("params").getJSONObject(0).getJSONArray("t").remove(0);
        byte[] timestampReply = bytes(
                new JSONArray()
                    .put(new JSONObject().put("id", 7)
                        .put("result", new JSONObject().put("rC", 0)
                            .put("tR", new JSONArray().put(new JSONObject().put("tId", "timer-start").put("tR", 1)))))
                    .toString());
        observe(42, bytes(timestamp.toString()), timestampReply);
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        JSONArray duration = timedLivesCalls(0, 86400);
        duration.getJSONObject(0).put("id", 8).getJSONArray("params").getJSONObject(0).getJSONArray("t").remove(1);
        observe(43, bytes(duration.toString()), response(8, TRANSACTION_GROUP, 0, 1, 0));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void rejectsMissingTimerStartAcceptance() throws Exception {
        beginTimedLives();
        observe(42, bytes(timedLivesCalls(0, 86400).toString()), response(7, TRANSACTION_GROUP, 0, 1, 0));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
    }

    @Test
    public void acceptsAdditiveTimedLivesWithExistingDuration() throws Exception {
        beginTimedLives();
        JSONArray calls = timedLivesCalls(1810, 86400);
        update(calls).put("p",
                new JSONObject().put("oldValue", 1810).put("value", 86400).put("strategy", 1).toString());
        JSONArray replies = new JSONArray(new String(response(7, TRANSACTION_GROUP, 0, 1, 0), StandardCharsets.UTF_8));
        replies.getJSONObject(0)
            .getJSONObject("result")
            .getJSONArray("tR")
            .put(new JSONObject().put("tId", "timer-start").put("tR", 1));
        observe(42, bytes(calls.toString()), bytes(replies.toString()));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
    }

    @Test
    public void ignoresOneHourPairBeforeAcceptingLivesAlone() throws Exception {
        checkUnrelatedLivesPair(false, true);
    }

    @Test
    public void ignoresOneHourPairBeforeAcceptingCombinedRewards() throws Exception {
        checkUnrelatedLivesPair(true, true);
    }

    @Test
    public void waitsForCurrentTimestampAfterOneHourPairWithLivesAlone() throws Exception {
        checkUnrelatedLivesPair(false, false);
    }

    @Test
    public void waitsForCurrentTimestampAfterOneHourPairWithCombinedRewards() throws Exception {
        checkUnrelatedLivesPair(true, false);
    }

    @Test
    public void ignoresBatchedOneHourPairsBeforeAcceptingLivesAlone() throws Exception {
        checkUnrelatedLivesPairs(false, true, true, true);
    }

    @Test
    public void ignoresBatchedOneHourPairsBeforeAcceptingCombinedRewards() throws Exception {
        checkUnrelatedLivesPairs(true, true, true, true);
    }

    @Test
    public void ignoresSeparateOneHourPairsBeforeAcceptingLivesAlone() throws Exception {
        checkUnrelatedLivesPairs(false, true, true, false);
    }

    @Test
    public void ignoresSeparateOneHourPairsBeforeAcceptingCombinedRewards() throws Exception {
        checkUnrelatedLivesPairs(true, true, true, false);
    }

    private void checkUnrelatedLivesPair(boolean combined, boolean includeTimestamp) throws Exception {
        checkUnrelatedLivesPairs(combined, includeTimestamp, false, false);
    }

    @SuppressWarnings("unchecked")
    private void checkUnrelatedLivesPairs(boolean combined, boolean includeTimestamp, boolean twoPairs, boolean batched)
            throws Exception {
        Field rewardsField = Boosters.class.getDeclaredField("rewards");
        rewardsField.setAccessible(true);
        List<Boosters> rewards = (List<Boosters>) rewardsField.get(null);
        List<Boosters> savedRewards = new ArrayList<>(rewards);
        Field verifiedField = Boosters.class.getDeclaredField("verified");
        verifiedField.setAccessible(true);
        boolean savedVerified = verifiedField.getBoolean(null);
        Field activityField = Boosters.class.getDeclaredField("activity");
        activityField.setAccessible(true);
        Object savedActivity = activityField.get(null);
        Constructor<Boosters> constructor = Boosters.class.getDeclaredConstructor(int[].class, int.class,
                BoosterGrant.class);
        constructor.setAccessible(true);
        BoosterGrant boosters = new BoosterGrant(ITEMS, AMOUNT);
        this.grant = new BoosterGrant(new int[] { 3646 }, 86400, true);
        try {
            rewards.clear();
            if (combined) {
                rewards.add(constructor.newInstance(ITEMS, AMOUNT, boosters));
            }
            rewards.add(constructor.newInstance(new int[] { 3669 }, 24, this.grant));
            verifiedField.setBoolean(null, true);
            activityField.set(null, new WeakReference<>(null));
            byte[] initial = bytes(new JSONArray()
                .put(new JSONObject().put("id", 6)
                    .put("result", new JSONObject().put("rC", 0).put("sI", new JSONObject())))
                .toString());
            dispatch(40, request(6, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, 0), initial);
            assertTrue(this.grant.reserve());
            assertTrue(this.grant.begin());
            this.grant.submitted(Boosters.SUBMITTED);
            if (combined) {
                assertTrue(boosters.reserve());
                assertTrue(boosters.begin());
                boosters.submitted(Boosters.SUBMITTED);
                dispatch(41, request(7, TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length),
                        response(7, TRANSACTION_GROUP, 0, ITEMS.length, 0));
                assertEquals(BoosterGrant.State.COMPLETE, boosters.state());
            }
            JSONArray otherCalls = new JSONArray(
                    new String(timerPair(10, "other", 3600, true), StandardCharsets.UTF_8));
            JSONArray otherReplies = new JSONArray(
                    new String(timerPairReply(10, "other", true), StandardCharsets.UTF_8));
            if (twoPairs && batched) {
                JSONArray extraTransactions = new JSONArray(
                        new String(timerPair(10, "second", 3600, true), StandardCharsets.UTF_8))
                    .getJSONObject(0)
                    .getJSONArray("params")
                    .getJSONObject(0)
                    .getJSONArray("t");
                JSONArray entries = otherCalls.getJSONObject(0)
                    .getJSONArray("params")
                    .getJSONObject(0)
                    .getJSONArray("t");
                entries.put(extraTransactions.getJSONObject(0)).put(extraTransactions.getJSONObject(1));
                JSONArray replies = otherReplies.getJSONObject(0).getJSONObject("result").getJSONArray("tR");
                replies.put(new JSONObject().put("tId", "secondduration").put("tR", 1))
                    .put(new JSONObject().put("tId", "secondstart").put("tR", 1));
            }
            dispatch(61, bytes(otherCalls.toString()), bytes(otherReplies.toString()));
            if (twoPairs && !batched) {
                dispatch(63, timerPair(12, "second", 3600, true), timerPairReply(12, "second", true));
            }
            assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
            Field transactions = BoosterGrant.class.getDeclaredField("transactions");
            transactions.setAccessible(true);
            Field accepted = BoosterGrant.class.getDeclaredField("acceptedItems");
            accepted.setAccessible(true);
            assertTrue(((Map<?, ?>) transactions.get(this.grant)).isEmpty());
            assertTrue(((java.util.Set<?>) accepted.get(this.grant)).isEmpty());
            dispatch(62, timerPair(11, "current", 86400, includeTimestamp),
                    timerPairReply(11, "current", includeTimestamp));
            assertEquals(includeTimestamp ? BoosterGrant.State.COMPLETE : BoosterGrant.State.AWAITING,
                    this.grant.state());
            if (!includeTimestamp) {
                assertTrue(((Map<?, ?>) transactions.get(this.grant)).isEmpty());
                assertTrue(((java.util.Set<?>) accepted.get(this.grant)).isEmpty());
            }
            if (combined) {
                assertEquals(BoosterGrant.State.COMPLETE, boosters.state());
            }
            assertWarningCount(0);
        } finally {
            rewards.clear();
            rewards.addAll(savedRewards);
            verifiedField.setBoolean(null, savedVerified);
            activityField.set(null, savedActivity);
        }
    }

    private static void dispatch(long id, byte[] request, byte[] response) {
        Boosters.onRequest(id, 0, "https://example.test/rpc/ClientApi", 0, false, null, request);
        Boosters.onData(id, response, response.length);
        Boosters.onResponse(id, false);
    }

    private static byte[] timerPair(int rpcId, String group, int seconds, boolean includeTimestamp) throws Exception {
        JSONArray calls = timedLivesCalls(0, seconds);
        JSONObject call = calls.getJSONObject(0).put("id", rpcId);
        JSONArray transactions = call.getJSONArray("params").getJSONObject(0).getJSONArray("t");
        transactions.getJSONObject(0).put("id", group + "duration");
        transactions.getJSONObject(1).put("id", group + "start");
        if (!includeTimestamp) {
            transactions.remove(1);
        }
        return bytes(calls.toString());
    }

    private static byte[] timerPairReply(int rpcId, String group, boolean includeTimestamp) throws Exception {
        JSONArray replies = new JSONArray().put(new JSONObject().put("tId", group + "duration").put("tR", 1));
        if (includeTimestamp) {
            replies.put(new JSONObject().put("tId", group + "start").put("tR", 1));
        }
        return bytes(new JSONArray()
            .put(new JSONObject().put("id", rpcId).put("result", new JSONObject().put("rC", 0).put("tR", replies)))
            .toString());
    }

    @Test
    public void rejectsMultipleSplitTimerStartCandidates() throws Exception {
        beginTimedLives();
        observe(42, timerPart(7, "other", 3600, true), timerPartReply(7, "other", true, true));
        observe(43, timerPart(8, "current", 86400, true), timerPartReply(8, "current", true, true));
        this.grant.onRequest(44, timerPart(9, "current", 86400, false));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertWarningCount(1);
        assertEquals(0, timerOwnedCount());
    }

    @Test
    public void pairsCurrentRpcBeforeConsideringUnrelatedSplitTimestamp() throws Exception {
        beginTimedLives();
        observe(42, timerPart(7, "other", 3600, true), timerPartReply(7, "other", true, true));
        observe(43, timerPair(8, "current", 86400, true), timerPairReply(8, "current", true));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertEquals(2, timerOwnedCount());
        assertWarningCount(0);
    }

    @Test
    public void discardsDifferentDurationPairSplitAcrossRpcs() throws Exception {
        beginTimedLives();
        observe(42, timerPart(7, "other", 3600, true), timerPartReply(7, "other", true, true));
        observe(43, timerPart(8, "other", 3600, false), timerPartReply(8, "other", false, true));
        assertEquals(0, timerOwnedCount());
        assertEquals(0, timerPendingCount());
        observe(44, timerPair(9, "current", 86400, true), timerPairReply(9, "current", true));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    public void retainsRejectedPendingAcknowledgementAcrossAcceptedResend() throws Exception {
        beginTimedLives();
        observe(42, timerPart(7, "current", 86400, true), timerPartReply(7, "current", true, false));
        observe(43, timerPart(8, "current", 86400, true), timerPartReply(8, "current", true, true));
        observe(44, timerPart(9, "current", 86400, false), timerPartReply(9, "current", false, true));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertWarningCount(1);
    }

    @Test
    public void pairsLaterRpcBeforeConsideringEarlierSplitTimestampInBatch() throws Exception {
        beginTimedLives();
        observe(42, timerPart(7, "current", 86400, false), timerPartReply(7, "current", false, true));
        JSONArray calls = new JSONArray(new String(timerPart(8, "other", 3600, true), StandardCharsets.UTF_8));
        calls.put(new JSONArray(new String(timerPair(9, "current", 86400, true), StandardCharsets.UTF_8))
            .getJSONObject(0));
        JSONArray replies = new JSONArray(new String(timerPartReply(8, "other", true, true), StandardCharsets.UTF_8));
        replies.put(
                new JSONArray(new String(timerPairReply(9, "current", true), StandardCharsets.UTF_8)).getJSONObject(0));
        observe(43, bytes(calls.toString()), bytes(replies.toString()));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertEquals(2, timerOwnedCount());
        assertWarningCount(0);
    }

    @Test
    public void excludesTimerCandidatesFromRefusedNativeAttempt() throws Exception {
        this.grant = new BoosterGrant(new int[] { 3646 }, 86400, true);
        begin();
        observe(42, timerPart(7, "other", 3600, true), timerPartReply(7, "other", true, true));
        this.grant.submitted(Boosters.NOT_SUBMITTED);
        assertEquals(BoosterGrant.State.READY, this.grant.state());
        assertEquals(0, timerPendingCount());
        assertTrue(this.grant.reserve());
        assertTrue(this.grant.begin());
        this.grant.submitted(Boosters.SUBMITTED);
        observe(43, timerPart(8, "other", 3600, true), timerPartReply(8, "other", true, true));
        assertEquals(0, timerPendingCount());
        observe(44, timerPair(9, "current", 86400, true), timerPairReply(9, "current", true));
        assertEquals(BoosterGrant.State.COMPLETE, this.grant.state());
        assertWarningCount(0);
    }

    @Test
    public void retainsSixtyFourTimerCandidatesAndRejectsNextWithoutEviction() throws Exception {
        beginTimedLives();
        JSONArray starts = new JSONArray();
        for (int index = 0; index < 64; index++) {
            JSONArray calls = new JSONArray(
                    new String(timerPart(7, "candidate" + index, 86400, true), StandardCharsets.UTF_8));
            starts.put(transaction(calls));
        }
        this.grant.onRequest(42, inventoryRequest(7, starts));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        assertEquals(64, timerPendingCount());
        this.grant.onRequest(43, timerPart(8, "excess", 86400, true));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertEquals(64, timerPendingCount());
        assertWarningCount(1);
    }

    @Test
    public void boundsTimerCandidateCharactersAndIgnoresDuplicateAccounting() throws Exception {
        beginTimedLives();
        int groupLength = new JSONArray().put("").put("").put(0).toString().length();
        char[] characters = new char[262144 - groupLength];
        Arrays.fill(characters, 'x');
        JSONArray calls = new JSONArray(new String(timerPart(7, "current", 86400, true), StandardCharsets.UTF_8));
        transaction(calls).put("id", new String(characters));
        this.grant.onRequest(42, bytes(calls.toString()));
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        calls.getJSONObject(0).put("id", 8);
        this.grant.onRequest(43, bytes(calls.toString()));
        assertEquals(1, timerPendingCount());
        assertEquals(BoosterGrant.State.AWAITING, this.grant.state());
        this.grant.onRequest(44, timerPart(9, "extra", 86400, true));
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertEquals(1, timerPendingCount());
        assertWarningCount(1);
    }

    private int timerOwnedCount() throws Exception {
        Field field = BoosterGrant.class.getDeclaredField("transactions");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(this.grant)).size();
    }

    private int timerPendingCount() throws Exception {
        Field field = BoosterGrant.class.getDeclaredField("timerCandidates");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(this.grant)).size();
    }

    private static byte[] timerPart(int rpcId, String group, int seconds, boolean timestamp) throws Exception {
        JSONArray calls = new JSONArray(new String(timerPair(rpcId, group, seconds, true), StandardCharsets.UTF_8));
        calls.getJSONObject(0).getJSONArray("params").getJSONObject(0).getJSONArray("t").remove(timestamp ? 0 : 1);
        return bytes(calls.toString());
    }

    private static byte[] timerPartReply(int rpcId, String group, boolean timestamp, boolean accepted)
            throws Exception {
        String id = group + (timestamp ? "start" : "duration");
        JSONArray replies = new JSONArray().put(new JSONObject().put("tId", id).put("tR", accepted ? 1 : 0));
        return bytes(new JSONArray()
            .put(new JSONObject().put("id", rpcId).put("result", new JSONObject().put("rC", 0).put("tR", replies)))
            .toString());
    }

    private void beginTimedLives() throws Exception {
        this.grant = new BoosterGrant(new int[] { 3646 }, 86400, true);
        begin();
        this.grant.submitted(Boosters.SUBMITTED);
    }

    private static JSONObject transaction(JSONArray calls) throws Exception {
        return calls.getJSONObject(0).getJSONArray("params").getJSONObject(0).getJSONArray("t").getJSONObject(0);
    }

    private void assertUnknownWarning(String message) {
        assertEquals(BoosterGrant.State.UNKNOWN, this.grant.state());
        assertLog(message);
        assertWarningCount(1);
        assertPrivateMarkerAbsent();
    }

    private static void assertWarningCount(int expected) {
        int count = 0;
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(Boosters.LOG_TAG)) {
            if (item.type == Log.WARN) {
                count++;
                assertNull(item.throwable);
            }
        }
        assertEquals(expected, count);
    }

    private void begin() throws Exception {
        ready();
        assertTrue(this.grant.reserve());
        assertTrue(this.grant.begin());
    }

    private void ready() throws Exception {
        observe(40, request(6, OLD_TRANSACTION_GROUP, REWARD_TYPE, 0, ITEMS.length), bytes(new JSONArray()
            .put(new JSONObject().put("id", 6).put("result", new JSONObject().put("rC", 0).put("sI", new JSONObject())))
            .toString()));
    }

    private void observe(long requestId, byte[] request, byte[] response) {
        this.grant.onRequest(requestId, request);
        this.grant.onData(requestId, response, response.length);
        this.grant.onResponse(requestId);
    }

    private static JSONObject update(JSONArray calls) throws Exception {
        return calls.getJSONObject(0)
            .getJSONArray("params")
            .getJSONObject(0)
            .getJSONArray("t")
            .getJSONObject(0)
            .getJSONArray("u")
            .getJSONObject(0);
    }

    private static byte[] request(int rpcId, String transactionGroup, int type, int start, int end) throws Exception {
        JSONArray transactions = new JSONArray();
        for (int index = start; index < end; index++) {
            int item = ITEMS[index];
            JSONObject update = new JSONObject().put("h", new JSONObject().put("i", item))
                .put("fT", 1)
                .put("p", new JSONObject().put("value", AMOUNT).toString());
            transactions.put(new JSONObject().put("id", transactionGroup + "-" + item)
                .put("m", new JSONObject().put("transactionType", type).put("gameRoundId", 0))
                .put("u", new JSONArray().put(update)));
        }
        return inventoryRequest(rpcId, transactions);
    }

    private static byte[] inventoryRequest(int rpcId, JSONArray transactions) throws Exception {
        JSONObject call = new JSONObject().put("id", rpcId)
            .put("method", "AppUserInventoryApi.applyTransactions7")
            .put("params", new JSONArray().put(new JSONObject().put("t", transactions)));
        return bytes(new JSONArray().put(call).toString());
    }

    private static byte[] response(int rpcId, String transactionGroup, int start, int end, int code) throws Exception {
        JSONArray replies = new JSONArray();
        for (int index = start; index < end; index++) {
            replies.put(new JSONObject().put("tId", transactionGroup + "-" + ITEMS[index]).put("tR", 1));
        }
        return bytes(new JSONArray()
            .put(new JSONObject().put("id", rpcId).put("result", new JSONObject().put("rC", code).put("tR", replies)))
            .toString());
    }

    private static String deepJson() {
        char[] opens = new char[EXCESSIVE_DEPTH];
        char[] closes = new char[EXCESSIVE_DEPTH];
        Arrays.fill(opens, '[');
        Arrays.fill(closes, ']');
        return new String(opens) + "0" + new String(closes);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertLog(String expected) {
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(Boosters.LOG_TAG)) {
            if (expected.equals(item.msg)) {
                assertNull(item.throwable);
                return;
            }
        }
        throw new AssertionError("Missing log: " + expected);
    }

    private static void assertPrivateMarkerAbsent() {
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag(Boosters.LOG_TAG)) {
            assertFalse(item.msg.contains(PRIVATE_MARKER));
            assertNull(item.throwable);
        }
    }

    @Implements(JSONTokener.class)
    public static class BlockingJsonTokener {

        static final AtomicBoolean pause = new AtomicBoolean();

        static CountDownLatch parsing = new CountDownLatch(1);

        static CountDownLatch resume = new CountDownLatch(1);

        @RealObject
        private JSONTokener tokener;

        @Implementation
        protected Object nextValue() throws JSONException {
            if (pause.compareAndSet(true, false)) {
                parsing.countDown();
                try {
                    if (!resume.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Parser resume timed out");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
            }
            return Shadow.directlyOn(this.tokener, JSONTokener.class, "nextValue");
        }

    }

}
