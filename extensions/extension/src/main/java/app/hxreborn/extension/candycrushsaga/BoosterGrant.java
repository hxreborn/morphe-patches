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

    private final Object requestParsingLock = new Object();

    private final Set<String> ineligibleTransactionIds = new HashSet<>();

    private int ineligibleIdCharacters;

    private final Map<Long, Request> requests = new HashMap<>();

    private final Map<String, Integer> transactions = new HashMap<>();

    private final Set<Integer> acceptedItems = new HashSet<>();

    private State state = State.WAITING;

    private int attempts;

    private int unparsedIneligibleRequests;

    BoosterGrant(int[] itemIds, int amount) {
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
        int submissionAttempt;
        synchronized (this) {
            if (this.state == State.COMPLETE || this.state == State.UNKNOWN) {
                return;
            }
            submissionAttempt = (this.state == State.SUBMITTING || this.state == State.AWAITING) ? this.attempts : 0;
            if (submissionAttempt == 0) {
                if (this.unparsedIneligibleRequests >= MAX_UNPARSED_INELIGIBLE_REQUESTS) {
                    reject("Inventory parsing limit reached, pending=" + this.unparsedIneligibleRequests + ", limit="
                            + MAX_UNPARSED_INELIGIBLE_REQUESTS);
                    return;
                }
                this.unparsedIneligibleRequests++;
            }
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
            if (id.isEmpty() || this.ineligibleTransactionIds.contains(id)) {
                continue;
            }
            if (this.ineligibleTransactionIds.size() >= MAX_INELIGIBLE_IDS
                    || id.length() > MAX_INELIGIBLE_ID_CHARACTERS - this.ineligibleIdCharacters) {
                reject("Inventory identity limit reached, count=" + this.ineligibleTransactionIds.size() + ", limit="
                        + MAX_INELIGIBLE_IDS + ", characters=" + this.ineligibleIdCharacters + ", characterLimit="
                        + MAX_INELIGIBLE_ID_CHARACTERS);
                return;
            }
            this.ineligibleTransactionIds.add(id);
            this.ineligibleIdCharacters += id.length();
        }
    }

    private Set<String> recordTransactions(JSONArray entries) throws JSONException, IOException {
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
            Integer previous = this.transactions.get(id);
            JSONObject metadata = transaction.optJSONObject("m");
            if (metadata == null || metadata.optInt("transactionType") != REWARD_TRANSACTION_TYPE) {
                if (previous != null) {
                    reject("Reward metadata changed, transactions=" + this.transactions.size());
                    return found;
                }
                continue;
            }
            JSONArray updates = transaction.optJSONArray("u");
            Integer matchedItem = null;
            for (int updateIndex = 0; updates != null && updateIndex < updates.length(); updateIndex++) {
                JSONObject update = updates.optJSONObject(updateIndex);
                JSONObject item = (update != null) ? update.optJSONObject("h") : null;
                if (item == null || update.optInt("fT") != AMOUNT_FIELD_TYPE) {
                    continue;
                }
                int itemId = item.optInt("i");
                if (Arrays.binarySearch(this.itemIds, itemId) < 0) {
                    continue;
                }
                String payload = update.getString("p");
                validateNesting(payload);
                if (new JSONObject(payload).optInt("value") != this.amount) {
                    continue;
                }
                if (matchedItem != null && matchedItem != itemId) {
                    reject("Ambiguous reward updates, count=" + updates.length());
                    return found;
                }
                matchedItem = itemId;
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

    private synchronized void readResponse(Request request, JSONArray responses) throws JSONException {
        if (this.state == State.COMPLETE || this.state == State.UNKNOWN) {
            return;
        }
        Set<String> completed = new HashSet<>();
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
        if (!completed.containsAll(request.transactionIds.keySet())) {
            reject("Reward RPC reply missing, expected=" + request.transactionIds.size() + ", received="
                    + completed.size());
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
        if (!request.transactionIds.isEmpty()) {
            reject(message);
        } else if (this.state != State.COMPLETE && this.state != State.UNKNOWN) {
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
        if (this.state == State.AWAITING && this.transactions.size() == this.itemIds.length
                && this.acceptedItems.size() == this.itemIds.length) {
            this.state = State.COMPLETE;
            Log.i(Boosters.LOG_TAG, "Booster transactions accepted, rC=0, count=" + this.acceptedItems.size());
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

    private static final class Request {

        final Map<String, Set<String>> transactionIds = new HashMap<>();

        final Set<String> inventoryRpcIds = new HashSet<>();

        final ByteArrayOutputStream response = new ByteArrayOutputStream();

        boolean overflow;

    }

}
