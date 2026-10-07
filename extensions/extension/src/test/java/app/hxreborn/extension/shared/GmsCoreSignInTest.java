/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = GmsCoreSignInTest.CapturingAccountManager.class)
public final class GmsCoreSignInTest {

    @Before
    public void forgetEarlierRequests() {
        CapturingAccountManager.requests.clear();
    }

    private static TokenRequest requestToken() {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        GmsCoreSignIn.getCredential(activity, "client-id", new Callback());
        assertEquals(1, CapturingAccountManager.requests.size());
        return CapturingAccountManager.requests.get(0);
    }

    private static Bundle bundleWith(String token, String accountName) {
        final Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_AUTHTOKEN, token);
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        return bundle;
    }

    private static AccountManagerFuture<Bundle> resultOf(Bundle bundle) {
        return new Result(bundle);
    }

    private static String claims(String json) {
        return Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private static JSONObject decodeClaims(String idToken) throws Exception {
        return (JSONObject) invoke("decodeClaims", new Class<?>[] { String.class }, idToken);
    }

    private static Object invoke(String name, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        final Method method = GmsCoreSignIn.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception) {
                throw (Exception) ex.getCause();
            }
            throw ex;
        }
    }

    private static void assertDeliveryFailed() {
        final List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag("GmsCoreSignIn");
        assertEquals(logs.toString(), 1, logs.size());
        assertEquals("Could not deliver the failure", logs.get(0).msg);
        assertTrue(String.valueOf(logs.get(0).throwable), logs.get(0).throwable instanceof ClassNotFoundException);
        assertEquals("<failure-class>", logs.get(0).throwable.getMessage());
    }

    private static Object newCredential(Class<?> type, Map<String, String> fields) throws Exception {
        return invoke("newCredential", new Class<?>[] { Class.class, Map.class }, type, fields);
    }

    private static Constructor<?> singleArgumentConstructor(Class<?> type) throws Exception {
        return (Constructor<?>) invoke("singleArgumentConstructor", new Class<?>[] { Class.class }, type);
    }

    private static Map<String, String> fields(String... pairs) {
        final Map<String, String> fields = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            fields.put(pairs[i], pairs[i + 1]);
        }
        return fields;
    }

    @Test
    public void reportsAMissingActivityWithoutAskingForAToken() {
        // given
        final Callback callback = new Callback();

        // when
        GmsCoreSignIn.getCredential(null, "client-id", callback);

        // then
        assertEquals(0, callback.invocations);
        assertTrue(CapturingAccountManager.requests.isEmpty());
        assertDeliveryFailed();
    }

    @Test
    public void reportsAMissingServerClientIdWithoutAskingForAToken() {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        for (String serverClientId : new String[] { null, "" }) {
            ShadowLog.clear();
            GmsCoreSignIn.getCredential(activity, serverClientId, new Callback());
            assertTrue(CapturingAccountManager.requests.isEmpty());
            assertDeliveryFailed();
        }
    }

    @Test
    public void asksGmsCoreForAnAudienceTokenOfTheServerClientId() {
        // given
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        // when
        GmsCoreSignIn.getCredential(activity, "client-id", new Callback());

        // then
        assertEquals(1, CapturingAccountManager.requests.size());
        final TokenRequest request = CapturingAccountManager.requests.get(0);
        assertEquals("app.revanced", request.accountType);
        assertEquals("audience:server:client_id:client-id", request.authTokenType);
        assertNull(request.features);
        assertSame(activity, request.activity);
    }

    @Test
    public void attemptsToDeliverACredentialWhenGmsCoreReturnsAToken() {
        // given
        final TokenRequest request = requestToken();

        // when
        request.callback.run(resultOf(bundleWith("header." + claims("{\"email\":\"a@x\"}") + ".signature", "a@x")));

        // then
        final List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag("GmsCoreSignIn");
        assertEquals(logs.toString(), 2, logs.size());
        assertEquals("Could not deliver the credential", logs.get(0).msg);
        assertEquals("Could not deliver the failure", logs.get(1).msg);
    }

    @Test
    public void reportsAMissingTokenAsNoCredential() {
        // when
        requestToken().callback.run(resultOf(bundleWith(null, "a@x")));

        // then
        assertDeliveryFailed();
    }

    @Test
    public void reportsAFailedTokenRequestAsAFailure() {
        // when
        requestToken().callback.run(new FailedResult());

        // then
        assertDeliveryFailed();
    }

    @Test
    public void decodesTheClaimsOfAnIdToken() throws Exception {
        // when
        final JSONObject decoded = decodeClaims(
                "header." + claims("{\"email\":\"a@x\",\"name\":\"Alice\",\"sub\":\"42\"}") + ".signature");

        // then
        assertEquals("a@x", decoded.getString("email"));
        assertEquals("Alice", decoded.getString("name"));
        assertEquals("42", decoded.getString("sub"));
    }

    @Test
    public void decodesUrlSafeClaimsWithoutPadding() throws Exception {
        // given
        final String json = "{\"picture\":\"https://x/?a=~~~&b=???\"}";
        final String payload = claims(json);
        assertTrue(payload, payload.contains("-") || payload.contains("_"));
        assertFalse(payload, payload.contains("="));

        // when
        final String decoded = decodeClaims("h." + payload + ".s").toString().replace("\\/", "/");

        // then
        assertEquals(json, decoded);
    }

    @Test
    public void decodesNoClaimsFromTokensWithoutAPayload() throws Exception {
        for (String token : new String[] { "", "nodots", "header." }) {
            assertEquals(token, 0, decodeClaims(token).length());
        }
    }

    @Test
    public void decodesNoClaimsFromAPayloadThatIsNotJson() throws Exception {
        assertEquals(0, decodeClaims("h." + claims("not json") + ".s").length());
        assertEquals(0, decodeClaims("h.!!!.s").length());
        assertEquals(0, decodeClaims("h..s").length());
    }

    @Test
    public void buildsCredentialFromNamedFields() throws Exception {
        // when
        final Object built = newCredential(Credential.class,
                fields("id", "a@x", "idToken", "token", "displayName", "Alice", "unused", "ignored"));

        // then
        final Credential credential = (Credential) built;
        assertEquals("a@x", credential.id);
        assertEquals("token", credential.idToken);
        assertEquals("Alice", credential.displayName);
    }

    @Test
    public void placesFieldsByConstructorPositionNotPrintOrder() throws Exception {
        // when
        final ShuffledCredential credential = (ShuffledCredential) newCredential(ShuffledCredential.class,
                fields("id", "a@x", "idToken", "token"));

        // then
        assertEquals("token", credential.idToken);
        assertEquals("a@x", credential.id);
    }

    @Test
    public void passesNullForFieldsTheMapLacks() throws Exception {
        // when
        final Credential credential = (Credential) newCredential(Credential.class, fields("id", "a@x"));

        // then
        assertEquals("a@x", credential.id);
        assertNull(credential.idToken);
        assertNull(credential.displayName);
    }

    @Test
    public void passesNullForParametersTheDescriptionDoesNotPrint() throws Exception {
        // when
        final SilentCredential credential = (SilentCredential) newCredential(SilentCredential.class,
                fields("id", "a@x", "hidden", "secret"));

        // then
        assertEquals("a@x", credential.id);
        assertNull(credential.hidden);
    }

    @Test
    public void picksTheAllStringConstructor() throws Exception {
        // when
        final MixedCredential credential = (MixedCredential) newCredential(MixedCredential.class, fields("id", "a@x"));

        // then
        assertEquals("a@x", credential.id);
        assertEquals(0, credential.count);
    }

    @Test
    public void failsNamingTheClassWithoutAStringConstructor() throws Exception {
        for (Class<?> type : new Class<?>[] { NumericCredential.class, Object.class }) {
            try {
                newCredential(type, fields());
                throw new AssertionError("expected a failure for " + type);
            } catch (NoSuchMethodException ex) {
                assertEquals(type.getName() + " has no String constructor", ex.getMessage());
            }
        }
    }

    @Test
    public void findsTheSingleArgumentConstructor() throws Exception {
        // when
        final Constructor<?> constructor = singleArgumentConstructor(Wrapper.class);

        // then
        assertEquals(1, constructor.getParameterTypes().length);
        assertEquals(Credential.class, constructor.getParameterTypes()[0]);
    }

    @Test
    public void failsNamingTheClassWithoutASingleArgumentConstructor() throws Exception {
        try {
            singleArgumentConstructor(Credential.class);
            throw new AssertionError("expected a failure");
        } catch (NoSuchMethodException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith(Credential.class.getName()));
            assertTrue(ex.getMessage(), ex.getMessage().endsWith("has no single-argument constructor"));
        }
    }

    private static final class TokenRequest {

        final String accountType;

        final String authTokenType;

        final String[] features;

        final Activity activity;

        final AccountManagerCallback<Bundle> callback;

        TokenRequest(String accountType, String authTokenType, String[] features, Activity activity,
                AccountManagerCallback<Bundle> callback) {
            this.accountType = accountType;
            this.authTokenType = authTokenType;
            this.features = features;
            this.activity = activity;
            this.callback = callback;
        }

    }

    @Implements(AccountManager.class)
    public static class CapturingAccountManager extends ShadowAccountManager {

        static final List<TokenRequest> requests = new ArrayList<>();

        @Implementation
        protected AccountManagerFuture<Bundle> getAuthTokenByFeatures(String accountType, String authTokenType,
                String[] features, Activity activity, Bundle addAccountOptions, Bundle getAuthTokenOptions,
                AccountManagerCallback<Bundle> callback, Handler handler) {
            requests.add(new TokenRequest(accountType, authTokenType, features, activity, callback));
            return null;
        }

    }

    private static final class Result implements AccountManagerFuture<Bundle> {

        private final Bundle bundle;

        Result(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Bundle getResult() {
            return this.bundle;
        }

        @Override
        public Bundle getResult(long timeout, TimeUnit unit) {
            return this.bundle;
        }

    }

    private static final class FailedResult implements AccountManagerFuture<Bundle> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Bundle getResult() throws OperationCanceledException, AuthenticatorException {
            throw new AuthenticatorException("no authenticator");
        }

        @Override
        public Bundle getResult(long timeout, TimeUnit unit) throws OperationCanceledException, AuthenticatorException {
            throw new AuthenticatorException("no authenticator");
        }

    }

    public static final class Callback {

        int invocations;

        public void invoke(Object result) {
            this.invocations++;
        }

    }

    public static final class Credential {

        final String id;

        final String idToken;

        final String displayName;

        public Credential(String id, String idToken, String displayName) {
            this.id = id;
            this.idToken = idToken;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return "Credential(id=" + this.id + ", idToken=" + this.idToken + ", displayName=" + this.displayName + ")";
        }

    }

    public static final class ShuffledCredential {

        final String idToken;

        final String id;

        public ShuffledCredential(String idToken, String id) {
            this.idToken = idToken;
            this.id = id;
        }

        @Override
        public String toString() {
            return "ShuffledCredential(id=" + this.id + ", idToken=" + this.idToken + ")";
        }

    }

    public static final class SilentCredential {

        final String id;

        final String hidden;

        public SilentCredential(String id, String hidden) {
            this.id = id;
            this.hidden = hidden;
        }

        @Override
        public String toString() {
            return "SilentCredential(id=" + this.id + ")";
        }

    }

    public static final class MixedCredential {

        final String id;

        final int count;

        public MixedCredential(String id, int count) {
            this.id = id;
            this.count = count;
        }

        public MixedCredential(String id) {
            this(id, 0);
        }

        @Override
        public String toString() {
            return "MixedCredential(id=" + this.id + ")";
        }

    }

    public static final class NumericCredential {

        public NumericCredential(int value) {
        }

    }

    public static final class Wrapper {

        public Wrapper(Credential credential) {
        }

        public Wrapper(String first, String second) {
        }

    }

}
