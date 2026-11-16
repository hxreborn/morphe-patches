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
package app.hxreborn.extension.quranify;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

final class WebApiSession {

    private static final String WEB_ORIGIN = "https://quranify.pro";

    private static final String SESSION_URL = WEB_ORIGIN + "/api/data-session";

    private static final String BACKEND_URL = WEB_ORIGIN + "/api/backend";

    private static final String SESSION_HEADER = "x-quranify-data-session";

    private static final String APP_SESSION_HEADER = "x-session-token";

    private static final byte[] SHARED_KEY_LABEL = "quranify-api-shared-key-v2\0".getBytes(StandardCharsets.UTF_8);

    private static final byte[] REQUEST_AAD = "quranify-api-request-v2".getBytes(StandardCharsets.UTF_8);

    private static final byte[] RESPONSE_AAD = "quranify-api-response-v2".getBytes(StandardCharsets.UTF_8);

    private static final List<String> RETURNED_HEADERS = List.of(
            "etag",
            "retry-after",
            "x-billing-environment");

    private static final int PROTOCOL_VERSION = 1;

    private static final int UNCOMPRESSED_POINT_TAG = 4;

    private static final int COORDINATE_BYTES = 32;

    private static final int POINT_BYTES = 1 + 2 * COORDINATE_BYTES;

    private static final int IV_BYTES = 12;

    private static final int AUTH_TAG_BITS = 128;

    private static final int PAYLOAD_HEADER_BYTES = 1 + IV_BYTES;

    private static final int MIN_RESPONSE_BYTES = 30;

    private static final long REFRESH_MARGIN_MS = 15000;

    private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecretKeySpec key;

    private String token;

    private long expiresAtMillis;

    synchronized ApiResponse send(ApiRequest request) throws IOException, JSONException, GeneralSecurityException {
        if (this.key == null || this.expiresAtMillis <= System.currentTimeMillis() + REFRESH_MARGIN_MS) {
            this.open();
        }
        Map<String, String> headers = new HashMap<>(request.headers);
        headers.remove(APP_SESSION_HEADER);
        JSONObject envelope = new JSONObject().put("v", PROTOCOL_VERSION)
            .put("method", request.method)
            .put("path", request.path)
            .put("query", request.query)
            .put("headers", new JSONObject(headers));
        if (request.body.length > 0) {
            envelope.put("body", new String(request.body, StandardCharsets.UTF_8));
        }
        Map<String, String> transportHeaders = new HashMap<>();
        transportHeaders.put("content-type", "application/octet-stream");
        transportHeaders.put(SESSION_HEADER, this.token);
        ApiResponse transport = HttpExchange.send(BACKEND_URL, "POST", transportHeaders,
                this.encrypt(envelope.toString().getBytes(StandardCharsets.UTF_8)));
        if (transport.status != HttpURLConnection.HTTP_OK || transport.body.length < MIN_RESPONSE_BYTES
                || transport.body[0] != PROTOCOL_VERSION) {
            this.key = null;
            throw new IOException("Invalid web API response");
        }
        JSONObject response = new JSONObject(new String(this.decrypt(transport.body), StandardCharsets.UTF_8));
        Map<String, String> returnedHeaders = new HashMap<>();
        JSONObject receivedHeaders = response.optJSONObject("headers");
        if (receivedHeaders != null) {
            for (String name : RETURNED_HEADERS) {
                if (receivedHeaders.has(name)) {
                    returnedHeaders.put(name, receivedHeaders.getString(name));
                }
            }
        }
        return new ApiResponse(response.getInt("status"), response.getString("contentType"),
                response.getString("body").getBytes(StandardCharsets.UTF_8), returnedHeaders);
    }

    private byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, this.key, new GCMParameterSpec(AUTH_TAG_BITS, iv));
        cipher.updateAAD(REQUEST_AAD);
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] payload = new byte[PAYLOAD_HEADER_BYTES + ciphertext.length];
        payload[0] = PROTOCOL_VERSION;
        System.arraycopy(iv, 0, payload, 1, IV_BYTES);
        System.arraycopy(ciphertext, 0, payload, PAYLOAD_HEADER_BYTES, ciphertext.length);
        return payload;
    }

    private byte[] decrypt(byte[] payload) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, this.key,
                new GCMParameterSpec(AUTH_TAG_BITS, Arrays.copyOfRange(payload, 1, PAYLOAD_HEADER_BYTES)));
        cipher.updateAAD(RESPONSE_AAD);
        return cipher.doFinal(Arrays.copyOfRange(payload, PAYLOAD_HEADER_BYTES, payload.length));
    }

    private void open() throws GeneralSecurityException, IOException, JSONException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        ECPublicKey publicKey = (ECPublicKey) pair.getPublic();
        JSONObject request = new JSONObject().put("publicKey",
                Base64.encodeToString(encodePoint(publicKey.getW()), BASE64_FLAGS));
        ApiResponse response = HttpExchange.send(SESSION_URL, "POST",
                Collections.singletonMap("content-type", "application/json"),
                request.toString().getBytes(StandardCharsets.UTF_8));
        if (response.status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Could not create web API session");
        }
        JSONObject session = new JSONObject(new String(response.body, StandardCharsets.UTF_8));
        byte[] remotePoint = Base64.decode(session.getString("publicKey"), BASE64_FLAGS);
        if (session.getInt("v") != PROTOCOL_VERSION || remotePoint.length != POINT_BYTES
                || remotePoint[0] != UNCOMPRESSED_POINT_TAG) {
            throw new IOException("Invalid web API session");
        }
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(pair.getPrivate());
        agreement.doPhase(KeyFactory.getInstance("EC")
            .generatePublic(new ECPublicKeySpec(decodePoint(remotePoint), publicKey.getParams())), true);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(SHARED_KEY_LABEL);
        this.key = new SecretKeySpec(digest.digest(agreement.generateSecret()), "AES");
        this.token = session.getString("sessionToken");
        this.expiresAtMillis = session.getLong("expiresAt");
    }

    private static byte[] encodePoint(ECPoint point) {
        byte[] encoded = new byte[POINT_BYTES];
        encoded[0] = UNCOMPRESSED_POINT_TAG;
        copyCoordinate(point.getAffineX(), encoded, 1);
        copyCoordinate(point.getAffineY(), encoded, 1 + COORDINATE_BYTES);
        return encoded;
    }

    private static ECPoint decodePoint(byte[] encoded) {
        return new ECPoint(new BigInteger(1, Arrays.copyOfRange(encoded, 1, 1 + COORDINATE_BYTES)),
                new BigInteger(1, Arrays.copyOfRange(encoded, 1 + COORDINATE_BYTES, POINT_BYTES)));
    }

    private static void copyCoordinate(BigInteger value, byte[] target, int offset) {
        byte[] source = value.toByteArray();
        int count = Math.min(source.length, COORDINATE_BYTES);
        System.arraycopy(source, source.length - count, target, offset + COORDINATE_BYTES - count, count);
    }

}
