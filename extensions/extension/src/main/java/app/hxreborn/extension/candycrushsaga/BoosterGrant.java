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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class BoosterGrant {

    private static final String INVENTORY_METHOD = "AppUserInventoryApi.applyTransactions7";

    private static final int REWARD_TRANSACTION_TYPE = 5503;

    private static final int AMOUNT_FIELD_TYPE = 1;

    private static final int TIMER_START_FIELD_TYPE = 17002;

    private static final int REPLACE_FIELD_STRATEGY = 2;

    private static final int ADD_FIELD_STRATEGY = 1;

    private static final int TIMED_LIVES_REQUIRED_UPDATES = 2;

    private static final int MAX_TIMER_CANDIDATES = 64;

    private static final int MAX_TIMER_CANDIDATE_CHARACTERS = 262144;

    private static final int ACCEPTED_TRANSACTION = 1;

    private static final int MAX_PAYLOAD_BYTES = 16777216;

    private static final int MAX_JSON_DEPTH = 32;

    private static final int DECODE_BUFFER_SIZE = 4096;

    private static final int MAX_IN_FLIGHT_REQUESTS = 64;

    private static final int MAX_UNPARSED_INELIGIBLE_REQUESTS = 64;

    private static final int MAX_ATTEMPTS = 12;

    private static final int MAX_INELIGIBLE_IDS = 4096;

    private static final int MAX_INELIGIBLE_ID_CHARACTERS = 262144;

    private static final String JSON_WHITESPACE = " \t\r\n";

    private static final String LITERAL_DELIMITERS = "{}[]/\\:,=;# \t\f\r\n";

    private final int[] itemIds;

    private final int amount;

    private final boolean timedLives;

    private final Object requestParsingLock = new Object();

    private final Set<String> ineligibleTransactionIds = new HashSet<>();

    private int ineligibleIdCharacters;

    private final Map<Long, Request> requests = new HashMap<>();

    private final Map<String, String> transactions = new HashMap<>();

    private final Map<String, TimerCandidate> timedTransactionValues = new HashMap<>();

    private final Map<String, TimerCandidate> timerCandidates = new HashMap<>();

    private int timerCandidateCharacters;

    private final Set<String> acceptedItems = new HashSet<>();

    private State state = State.WAITING;

    private int attempts;

    private int unparsedIneligibleRequests;

    BoosterGrant(int[] itemIds, int amount) {
        this(itemIds, amount, false);
    }

    BoosterGrant(int[] itemIds, int amount, boolean timedLives) {
        this.timedLives = timedLives;
        this.itemIds = itemIds.clone();
        Arrays.sort(this.itemIds);
        this.amount = amount;
    }

    synchronized State state() {
        return this.state;
    }

    synchronized boolean reserve() {
        if (this.state != State.READY) {
            return false;
        }
        if (this.attempts >= MAX_ATTEMPTS) {
            reject("Booster submission limit reached, limit=" + MAX_ATTEMPTS);
            return false;
        }
        this.state = State.QUEUED;
        return true;
    }

    synchronized boolean begin() {
        if (this.state != State.QUEUED) {
            return false;
        }
        this.state = State.SUBMITTING;
        this.attempts++;
        return true;
    }

    synchronized void cancelQueued() {
        if (this.state == State.QUEUED) {
            this.state = State.READY;
        }
    }

    synchronized void submitted(int result) {
        if (this.state != State.SUBMITTING) {
            return;
        }
        if (result == Boosters.NOT_SUBMITTED) {
            if (this.transactions.isEmpty()) {
                for (String id : this.timerCandidates.keySet()) {
                    if (!rememberIneligible(id)) {
                        return;
                    }
                }
                this.timerCandidates.clear();
                this.timerCandidateCharacters = 0;
                this.state = State.READY;
            } else {
                reject("Unsubmitted reward transactions observed, count=" + this.transactions.size());
            }
        } else if (result == Boosters.SUBMITTED) {
            this.state = State.AWAITING;
            finishIfAccepted();
        } else {
            reject("Native reward result unknown, transactions=" + this.transactions.size());
        }
    }

    synchronized void fail() {
        reject("Booster grant aborted, transactions=" + this.transactions.size());
    }

    void onRequest(long requestId, byte[] body) {
        onRequest(requestId, body, reserveObservation());
    }

    synchronized int reserveObservation() {
        if (this.state == State.COMPLETE || this.state == State.UNKNOWN) {
            return -1;
        }
        int submissionAttempt = (this.state == State.SUBMITTING || this.state == State.AWAITING) ? this.attempts : 0;
        if (submissionAttempt == 0) {
            if (this.unparsedIneligibleRequests >= MAX_UNPARSED_INELIGIBLE_REQUESTS) {
                reject("Inventory parsing limit reached, pending=" + this.unparsedIneligibleRequests + ", limit="
                        + MAX_UNPARSED_INELIGIBLE_REQUESTS);
                return -1;
            }
            this.unparsedIneligibleRequests++;
        }
        return submissionAttempt;
    }

    void onRequest(long requestId, byte[] body, int submissionAttempt) {
        if (submissionAttempt < 0) {
            return;
        }
        synchronized (this.requestParsingLock) {
            try {
                readRequest(requestId, body, submissionAttempt);
            } finally {
                if (submissionAttempt == 0) {
                    synchronized (this) {
                        this.unparsedIneligibleRequests--;
                    }
                }
            }
        }
    }

    private void readRequest(long requestId, byte[] body, int submissionAttempt) {
        if (body == null || body.length > MAX_PAYLOAD_BYTES) {
            failPending("Inventory request size rejected, bytes=" + ((body != null) ? body.length : 0) + ", limit="
                    + MAX_PAYLOAD_BYTES);
            return;
        }
        try {
            JSONArray calls = array(decode(body));
            Request request = new Request();
            synchronized (this) {
                if (this.state == State.COMPLETE || this.state == State.UNKNOWN) {
                    return;
                }
                for (int index = 0; index < calls.length(); index++) {
                    JSONObject call = calls.optJSONObject(index);
                    if (call == null || !INVENTORY_METHOD.equals(call.optString("method"))) {
                        continue;
                    }
                    JSONArray params = call.optJSONArray("params");
                    JSONObject inventory = (params != null) ? params.optJSONObject(0) : null;
                    JSONArray entries = (inventory != null) ? inventory.optJSONArray("t") : null;
                    if (entries == null || !call.has("id")) {
                        continue;
                    }
                    String rpcId = call.getString("id");
                    request.inventoryRpcIds.add(rpcId);
                    if (submissionAttempt != 0 && submissionAttempt == this.attempts
                            && (this.state == State.SUBMITTING || this.state == State.AWAITING)) {
                        if (this.unparsedIneligibleRequests != 0) {
                            reject("Inventory request ordering uncertain, pending=" + this.unparsedIneligibleRequests);
                            return;
                        }
                        Set<String> ids = recordTransactions(entries);
                        if (!ids.isEmpty()) {
                            request.transactionIds.put(rpcId, ids);
                        }
                    } else {
                        rememberIneligible(entries);
                    }
                    if (this.state == State.UNKNOWN) {
                        return;
                    }
                }
                if (this.timedLives) {
                    resolveTimerCandidates(new HashSet<>(this.timerCandidates.keySet()), false);
                    if (this.state == State.UNKNOWN) {
                        return;
                    }
                }
                if (!request.inventoryRpcIds.isEmpty()) {
                    if (this.requests.containsKey(requestId)) {
                        reject("Duplicate inventory request, pending=" + this.requests.size());
                    } else if (this.requests.size() >= MAX_IN_FLIGHT_REQUESTS) {
                        reject("Inventory request limit reached, pending=" + this.requests.size() + ", limit="
                                + MAX_IN_FLIGHT_REQUESTS);
                    } else {
                        this.requests.put(requestId, request);
                    }
                }
            }
        } catch (JSONException | IOException ex) {
            failPending("Inventory request could not be read");
        }
    }

    synchronized void onData(long requestId, byte[] bytes, int length) {
        Request request = this.requests.get(requestId);
        if (request == null || request.overflow) {
            return;
        }
        if (bytes == null || length < 0 || length > bytes.length
                || length > MAX_PAYLOAD_BYTES - request.response.size()) {
            request.overflow = true;
            markUnknownIfGrant(request,
                    "Inventory response chunk rejected, buffered=" + request.response.size() + ", bytes="
                            + ((bytes != null) ? bytes.length : 0) + ", length=" + length + ", limit="
                            + MAX_PAYLOAD_BYTES);
            return;
        }
        request.response.write(bytes, 0, length);
    }

    void onResponse(long requestId) {
        Request request;
        synchronized (this) {
            request = this.requests.remove(requestId);
        }
        if (request == null) {
            return;
        }
        if (request.overflow) {
            return;
        }
        try {
            readResponse(request, array(decode(request.response.toByteArray())));
        } catch (JSONException | IOException ex) {
            markUnknownIfGrant(request, "Inventory response could not be read");
        }
    }

    private void rememberIneligible(JSONArray entries) {
        for (int index = 0; index < entries.length(); index++) {
            JSONObject transaction = entries.optJSONObject(index);
            String id = (transaction != null) ? transaction.optString("id") : "";
            if (!rememberIneligible(id)) {
                return;
            }
        }
    }

    private boolean rememberIneligible(String id) {
        if (id.isEmpty() || this.ineligibleTransactionIds.contains(id)) {
            return true;
        }
        if (this.ineligibleTransactionIds.size() >= MAX_INELIGIBLE_IDS
                || id.length() > MAX_INELIGIBLE_ID_CHARACTERS - this.ineligibleIdCharacters) {
            reject("Inventory identity limit reached, count=" + this.ineligibleTransactionIds.size() + ", limit="
                    + MAX_INELIGIBLE_IDS + ", characters=" + this.ineligibleIdCharacters + ", characterLimit="
                    + MAX_INELIGIBLE_ID_CHARACTERS);
            return false;
        }
        this.ineligibleTransactionIds.add(id);
        this.ineligibleIdCharacters += id.length();
        return true;
    }

    private Set<String> recordTransactions(JSONArray entries) throws JSONException, IOException {
        if (this.timedLives) {
            return recordTimerTransactions(entries);
        }
        Set<String> found = new HashSet<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject transaction = entries.optJSONObject(index);
            if (transaction == null) {
                continue;
            }
            String id = transaction.optString("id");
            if (this.ineligibleTransactionIds.contains(id)) {
                continue;
            }
            String previous = this.transactions.get(id);
            JSONObject metadata = transaction.optJSONObject("m");
            if (metadata == null || metadata.optInt("transactionType") != REWARD_TRANSACTION_TYPE) {
                if (previous != null) {
                    reject("Reward metadata changed, transactions=" + this.transactions.size());
                    return found;
                }
                continue;
            }
            JSONArray updates = transaction.optJSONArray("u");
            String matchedItem = null;
            for (int updateIndex = 0; updates != null && updateIndex < updates.length(); updateIndex++) {
                JSONObject update = updates.optJSONObject(updateIndex);
                JSONObject item = (update != null) ? update.optJSONObject("h") : null;
                if (item == null) {
                    continue;
                }
                int fieldType = update.optInt("fT");
                if (fieldType != AMOUNT_FIELD_TYPE && (!this.timedLives || fieldType != TIMER_START_FIELD_TYPE)) {
                    continue;
                }
                int itemId = item.optInt("i");
                if (Arrays.binarySearch(this.itemIds, itemId) < 0) {
                    continue;
                }
                String payload = update.getString("p");
                validateNesting(payload);
                JSONObject values = new JSONObject(payload);
                long grantedAmount = values.optLong("value");
                if (grantedAmount != this.amount) {
                    continue;
                }
                String itemField = itemId + ":" + fieldType;
                if (matchedItem != null && !matchedItem.equals(itemField)) {
                    reject("Ambiguous reward updates, count=" + updates.length());
                    return found;
                }
                matchedItem = itemField;
            }
            if (previous != null && !previous.equals(matchedItem)) {
                reject("Reward content changed, transactions=" + this.transactions.size());
                return found;
            }
            if (matchedItem == null) {
                continue;
            }
            if (id.isEmpty()) {
                reject("Reward transaction identifier missing, updates=" + updates.length());
                return found;
            }
            if (previous == null && this.transactions.containsValue(matchedItem)) {
                reject("Ambiguous reward transaction, transactions=" + this.transactions.size());
                return found;
            }
            this.transactions.put(id, matchedItem);
            found.add(id);
        }
        return found;
    }

    private Set<String> recordTimerTransactions(JSONArray entries) throws JSONException, IOException {
        Set<String> observed = new HashSet<>();
        for (int index = 0; index < entries.length(); index++) {
            JSONObject transaction = entries.optJSONObject(index);
            if (transaction == null) {
                continue;
            }
            String id = transaction.optString("id");
            if (this.ineligibleTransactionIds.contains(id)) {
                continue;
            }
            TimerCandidate previous = this.timedTransactionValues.get(id);
            if (previous == null) {
                previous = this.timerCandidates.get(id);
            }
            TimerCandidate candidate = timerCandidate(transaction);
            if (previous != null) {
                if (!previous.sameUpdate(candidate)) {
                    reject("Reward content changed, transactions=" + this.transactions.size());
                    return observed;
                }
                observed.add(id);
                continue;
            }
            if (candidate == null) {
                continue;
            }
            if (id.isEmpty()) {
                reject("Reward transaction identifier missing, updates=1");
                return observed;
            }
            if (this.timerCandidates.size() >= MAX_TIMER_CANDIDATES
                    || candidate.characters() > MAX_TIMER_CANDIDATE_CHARACTERS - this.timerCandidateCharacters) {
                reject("Timer candidate limit reached, count=" + this.timerCandidates.size() + ", characters="
                        + this.timerCandidateCharacters);
                return observed;
            }
            this.timerCandidates.put(id, candidate);
            this.timerCandidateCharacters += candidate.characters();
            observed.add(id);
        }
        resolveTimerCandidates(observed, true);
        Set<String> tracked = new HashSet<>(this.transactions.keySet());
        tracked.addAll(this.timerCandidates.keySet());
        observed.retainAll(tracked);
        return observed;
    }

    private TimerCandidate timerCandidate(JSONObject transaction) throws JSONException, IOException {
        JSONObject metadata = transaction.optJSONObject("m");
        if (metadata == null || metadata.optInt("transactionType") != REWARD_TRANSACTION_TYPE) {
            return null;
        }
        String group = new JSONArray().put(metadata.optString("txnRef"))
            .put(metadata.optString("details"))
            .put(metadata.optLong("gameRoundId"))
            .toString();
        TimerCandidate found = null;
        JSONArray updates = transaction.optJSONArray("u");
        for (int index = 0; updates != null && index < updates.length(); index++) {
            JSONObject update = updates.optJSONObject(index);
            JSONObject item = (update != null) ? update.optJSONObject("h") : null;
            if (item == null || Arrays.binarySearch(this.itemIds, item.optInt("i")) < 0) {
                continue;
            }
            int field = update.optInt("fT");
            if (field != AMOUNT_FIELD_TYPE && field != TIMER_START_FIELD_TYPE) {
                continue;
            }
            String payload = update.getString("p");
            validateNesting(payload);
            JSONObject values = new JSONObject(payload);
            long oldValue = values.optLong("oldValue", -1);
            long value = values.optLong("value", -1);
            int strategy = values.optInt("strategy");
            if (oldValue < 0 || value < 0 || (strategy != ADD_FIELD_STRATEGY && strategy != REPLACE_FIELD_STRATEGY)
                    || (field == TIMER_START_FIELD_TYPE && (value == 0 || strategy != REPLACE_FIELD_STRATEGY))) {
                continue;
            }
            if (found != null) {
                reject("Ambiguous reward updates, count=" + updates.length());
                return null;
            }
            found = new TimerCandidate(transaction.optString("id"), item.optInt("i"), field, oldValue, value, strategy,
                    group);
        }
        return found;
    }

    private void resolveTimerCandidates(Set<String> ids, boolean sameRpc) {
        if (this.state == State.UNKNOWN) {
            return;
        }
        Map<String, Set<TimerCandidate>> groups = new HashMap<>();
        for (String id : ids) {
            TimerCandidate candidate = this.timerCandidates.get(id);
            if (candidate == null) {
                continue;
            }
            String group = candidate.item + ":" + candidate.group;
            Set<TimerCandidate> candidates = groups.get(group);
            if (candidates == null) {
                candidates = new HashSet<>();
                groups.put(group, candidates);
            }
            candidates.add(candidate);
        }
        for (Set<TimerCandidate> candidates : groups.values()) {
            TimerCandidate timestamp = null;
            TimerCandidate duration = null;
            int timestamps = 0;
            int durations = 0;
            for (TimerCandidate candidate : candidates) {
                if (candidate.field == TIMER_START_FIELD_TYPE) {
                    timestamp = candidate;
                    timestamps++;
                } else {
                    duration = candidate;
                    durations++;
                }
            }
            if (timestamps == 0 || durations == 0) {
                continue;
            }
            int matchingDurations = 0;
            for (TimerCandidate candidate : candidates) {
                if (candidate.field != AMOUNT_FIELD_TYPE) {
                    continue;
                }
                long added = (candidate.strategy == ADD_FIELD_STRATEGY) ? candidate.value
                        : candidate.value - candidate.oldValue;
                if (added == this.amount) {
                    duration = candidate;
                    matchingDurations++;
                } else {
                    removeTimerCandidate(candidate);
                    rememberIneligible(candidate.id);
                }
            }
            if (matchingDurations == 0) {
                if (timestamps == durations) {
                    for (TimerCandidate candidate : candidates) {
                        if (candidate.field == TIMER_START_FIELD_TYPE) {
                            removeTimerCandidate(candidate);
                            rememberIneligible(candidate.id);
                        }
                    }
                }
                continue;
            }
            if (!sameRpc) {
                timestamps = 0;
                for (TimerCandidate candidate : this.timerCandidates.values()) {
                    if (candidate.item == duration.item && candidate.field == TIMER_START_FIELD_TYPE) {
                        timestamps++;
                    }
                }
            }
            if (timestamps != 1 || matchingDurations != 1) {
                reject("Timer reward pairing ambiguous, timestamps=" + timestamps + ", durations=" + matchingDurations);
                return;
            }
            removeTimerCandidate(timestamp);
            removeTimerCandidate(duration);
            ownTimerCandidate(timestamp);
            ownTimerCandidate(duration);
        }
    }

    private void removeTimerCandidate(TimerCandidate candidate) {
        this.timerCandidates.remove(candidate.id);
        this.timerCandidateCharacters -= candidate.characters();
    }

    private void ownTimerCandidate(TimerCandidate candidate) {
        String field = candidate.item + ":" + candidate.field;
        if (this.transactions.containsValue(field)) {
            reject("Ambiguous reward transaction, transactions=" + this.transactions.size());
            return;
        }
        this.transactions.put(candidate.id, field);
        this.timedTransactionValues.put(candidate.id, candidate);
        if (candidate.acknowledged != null) {
            if (!candidate.acknowledged) {
                reject("Timer reward acknowledgement rejected, transactions=" + this.transactions.size());
            } else {
                this.acceptedItems.add(field);
            }
        }
    }

    private synchronized void readResponse(Request request, JSONArray responses) throws JSONException {
        if (this.state == State.COMPLETE || this.state == State.UNKNOWN) {
            return;
        }
        Set<String> completed = new HashSet<>();
        Set<String> requiredRpcIds = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : request.transactionIds.entrySet()) {
            for (String id : entry.getValue()) {
                if (this.transactions.containsKey(id)) {
                    requiredRpcIds.add(entry.getKey());
                }
            }
        }
        for (int index = 0; index < responses.length(); index++) {
            JSONObject response = responses.optJSONObject(index);
            if (response == null || !response.has("id")) {
                continue;
            }
            String rpcId = response.getString("id");
            JSONObject result = response.optJSONObject("result");
            if (request.inventoryRpcIds.contains(rpcId) && result != null && result.optInt("rC", -1) == 0
                    && result.optJSONObject("sI") != null && this.state == State.WAITING) {
                this.state = State.READY;
                Log.d(Boosters.LOG_TAG, "Inventory synchronized");
            }
            Set<String> expected = request.transactionIds.get(rpcId);
            if (expected == null) {
                continue;
            }
            completed.add(rpcId);
            JSONArray replies = (result != null) ? result.optJSONArray("tR") : null;
            int resultCode = (result != null) ? result.optInt("rC", -1) : -1;
            if (this.timedLives) {
                Set<String> accepted = new HashSet<>();
                for (int replyIndex = 0; replies != null && replyIndex < replies.length(); replyIndex++) {
                    JSONObject reply = replies.optJSONObject(replyIndex);
                    if (reply != null && resultCode == 0 && reply.optInt("tR", -1) == ACCEPTED_TRANSACTION) {
                        accepted.add(reply.optString("tId"));
                    }
                }
                for (String id : expected) {
                    TimerCandidate candidate = this.timerCandidates.get(id);
                    if (candidate != null) {
                        candidate.acknowledged = !Boolean.FALSE.equals(candidate.acknowledged) && accepted.contains(id);
                    }
                }
            }
            expected = new HashSet<>(expected);
            expected.retainAll(this.transactions.keySet());
            if (expected.isEmpty()) {
                continue;
            }
            if (resultCode != 0 || replies == null) {
                reject("Reward acknowledgement rejected, rC=" + resultCode + ", expected=" + expected.size()
                        + ", replies=" + ((replies != null) ? replies.length() : 0));
                return;
            }
            Set<String> acknowledged = new HashSet<>();
            for (int replyIndex = 0; replyIndex < replies.length(); replyIndex++) {
                JSONObject reply = replies.optJSONObject(replyIndex);
                if (reply == null) {
                    continue;
                }
                String id = reply.optString("tId");
                if (expected.contains(id) && reply.optInt("tR", -1) == ACCEPTED_TRANSACTION) {
                    acknowledged.add(id);
                    this.acceptedItems.add(this.transactions.get(id));
                }
            }
            if (!acknowledged.equals(expected)) {
                reject("Reward transactions unacknowledged, rC=" + resultCode + ", expected=" + expected.size()
                        + ", accepted=" + acknowledged.size());
                return;
            }
        }
        for (Map.Entry<String, Set<String>> entry : request.transactionIds.entrySet()) {
            if (!completed.contains(entry.getKey())) {
                for (String id : entry.getValue()) {
                    TimerCandidate candidate = this.timerCandidates.get(id);
                    if (candidate != null) {
                        candidate.acknowledged = false;
                    }
                }
            }
        }
        if (!completed.containsAll(requiredRpcIds)) {
            reject("Reward RPC reply missing, expected=" + requiredRpcIds.size() + ", received=" + completed.size());
            return;
        }
        finishIfAccepted();
    }

    private synchronized void failPending(String message) {
        if (this.state == State.SUBMITTING || this.state == State.AWAITING || this.state == State.QUEUED) {
            reject(message);
        } else if (this.state != State.COMPLETE && this.state != State.UNKNOWN) {
            Log.w(Boosters.LOG_TAG, message);
        }
    }

    private synchronized void markUnknownIfGrant(Request request, String message) {
        boolean pending = false;
        for (Set<String> ids : request.transactionIds.values()) {
            for (String id : ids) {
                if (this.transactions.containsKey(id)) {
                    reject(message);
                    return;
                }
                TimerCandidate candidate = this.timerCandidates.get(id);
                if (candidate != null) {
                    candidate.acknowledged = false;
                    pending = true;
                }
            }
        }
        if (!pending && this.state != State.COMPLETE && this.state != State.UNKNOWN) {
            Log.w(Boosters.LOG_TAG, message);
        }
    }

    private void reject(String message) {
        if (this.state != State.COMPLETE && this.state != State.UNKNOWN) {
            this.state = State.UNKNOWN;
            Log.w(Boosters.LOG_TAG, message);
        }
    }

    private void finishIfAccepted() {
        int requiredUpdates = this.itemIds.length * (this.timedLives ? TIMED_LIVES_REQUIRED_UPDATES : 1);
        if (this.state == State.AWAITING && this.transactions.size() == requiredUpdates
                && this.acceptedItems.size() == requiredUpdates) {
            this.state = State.COMPLETE;
            Log.i(Boosters.LOG_TAG, (this.timedLives ? "Lives" : "Booster") + " transactions accepted, rC=0, count="
                    + this.itemIds.length);
        }
    }

    private static JSONArray array(String json) throws IOException, JSONException {
        validateNesting(json);
        return new JSONArray(json);
    }

    private static void validateNesting(String json) throws IOException {
        int depth = 0;
        for (int index = 0; index < json.length(); index++) {
            char character = json.charAt(index);
            if (character == '=' || (character == ':' && index + 1 < json.length() && json.charAt(index + 1) == '>')) {
                throw new IOException("Inventory separator rejected");
            }
            if (character == '{' || character == '[') {
                if (++depth > MAX_JSON_DEPTH) {
                    throw new IOException("Inventory nesting too deep");
                }
            } else if (character == '}' || character == ']') {
                if (--depth < 0) {
                    throw new IOException("Inventory nesting invalid");
                }
            } else if (character == '"' || character == '\'') {
                char quote = character;
                boolean closed = false;
                while (++index < json.length()) {
                    char next = json.charAt(index);
                    if (next == '\\') {
                        index++;
                    } else if (next == quote) {
                        closed = true;
                        break;
                    }
                }
                if (!closed) {
                    throw new IOException("Inventory string incomplete");
                }
            } else if (character == '/' || character == '#') {
                throw new IOException("Inventory comments rejected");
            } else if (JSON_WHITESPACE.indexOf(character) < 0 && character != ':' && character != ','
                    && character != ';' && character != '\\') {
                while (index + 1 < json.length() && LITERAL_DELIMITERS.indexOf(json.charAt(index + 1)) < 0) {
                    index++;
                }
            }
        }
        if (depth != 0) {
            throw new IOException("Inventory nesting incomplete");
        }
    }

    private static String decode(byte[] bytes) throws IOException {
        InputStream input = new ByteArrayInputStream(bytes);
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b) {
            input = new GZIPInputStream(input);
        }
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[DECODE_BUFFER_SIZE];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (count > MAX_PAYLOAD_BYTES - output.size()) {
                    throw new IOException("Inventory payload too large");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    enum State {

        WAITING, READY, QUEUED, SUBMITTING, AWAITING, COMPLETE, UNKNOWN

    }

    private static final class TimerCandidate {

        final String id;

        final int item;

        final int field;

        final long oldValue;

        final long value;

        final int strategy;

        final String group;

        Boolean acknowledged;

        TimerCandidate(String id, int item, int field, long oldValue, long value, int strategy, String group) {
            this.id = id;
            this.item = item;
            this.field = field;
            this.oldValue = oldValue;
            this.value = value;
            this.strategy = strategy;
            this.group = group;
        }

        int characters() {
            return this.id.length() + this.group.length();
        }

        boolean sameUpdate(TimerCandidate other) {
            return other != null && this.item == other.item && this.field == other.field
                    && this.oldValue == other.oldValue && this.value == other.value && this.strategy == other.strategy
                    && this.group.equals(other.group);
        }

    }

    private static final class Request {

        final Map<String, Set<String>> transactionIds = new HashMap<>();

        final Set<String> inventoryRpcIds = new HashSet<>();

        final ByteArrayOutputStream response = new ByteArrayOutputStream();

        boolean overflow;

    }

}
