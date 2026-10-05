/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.shared;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class GmsCoreSignIn {

    private static final String TAG = "GmsCoreSignIn";

    private static final String ACCOUNT_TYPE = "app.revanced";

    private static final char MARKER = '\u0001';

    private static final Pattern MARKED_FIELD = Pattern.compile("(\\w+)=" + MARKER + "(\\d+)" + MARKER);

    private GmsCoreSignIn() {
    }

    public static void getCredential(Activity activity, String serverClientId, Object callback) {
        if (activity == null) {
            fail(callback, "NO_ACTIVITY", "No activity available");
            return;
        }
        if (serverClientId == null || serverClientId.isEmpty()) {
            fail(callback, "MISSING_SERVER_CLIENT_ID", "CredentialManager requires a serverClientId.");
            return;
        }

        AccountManager.get(activity)
            .getAuthTokenByFeatures(ACCOUNT_TYPE, "audience:server:client_id:" + serverClientId, null, activity, null,
                    null, new TokenCallback(callback), null);
    }

    private static Class<?> successClass() throws ClassNotFoundException {
        return Class.forName("<success-class>");
    }

    private static Class<?> failureClass() throws ClassNotFoundException {
        return Class.forName("<failure-class>");
    }

    private static Class<?> resultClass() throws ClassNotFoundException {
        return Class.forName("<result-class>");
    }

    private static void succeed(Object callback, String idToken, String accountName) {
        try {
            JSONObject claims = decodeClaims(idToken);
            String email = claims.optString("email", accountName);

            Map<String, String> fields = new HashMap<>();
            fields.put("displayName", claims.optString("name", null));
            fields.put("givenName", claims.optString("given_name", null));
            fields.put("familyName", claims.optString("family_name", null));
            fields.put("email", email);
            fields.put("id", email);
            fields.put("uniqueId", claims.optString("sub", email));
            fields.put("idToken", idToken);
            fields.put("profilePictureUri", claims.optString("picture", null));

            Constructor<?> successConstructor = singleArgumentConstructor(successClass());
            Object credential = newCredential(successConstructor.getParameterTypes()[0], fields);

            complete(callback, successConstructor.newInstance(credential));
        }
        catch (Throwable throwable) {
            Log.e(TAG, "Could not deliver the credential", throwable);
            fail(callback, "UNKNOWN", String.valueOf(throwable.getMessage()));
        }
    }

    private static Object newCredential(Class<?> credentialClass, Map<String, String> fields) throws Exception {
        Constructor<?> constructor = stringConstructor(credentialClass);
        int parameterCount = constructor.getParameterTypes().length;

        String[] markers = new String[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            markers[i] = MARKER + Integer.toString(i) + MARKER;
        }

        String[] arguments = new String[parameterCount];
        Matcher matcher = MARKED_FIELD.matcher(constructor.newInstance((Object[]) markers).toString());
        while (matcher.find()) {
            arguments[Integer.parseInt(matcher.group(2))] = fields.get(matcher.group(1));
        }

        return constructor.newInstance((Object[]) arguments);
    }

    private static Constructor<?> stringConstructor(Class<?> type) throws NoSuchMethodException {
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            boolean allStrings = parameterTypes.length > 0;
            for (Class<?> parameterType : parameterTypes) {
                allStrings &= parameterType == String.class;
            }
            if (allStrings) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(type.getName() + " has no String constructor");
    }

    private static Constructor<?> singleArgumentConstructor(Class<?> type) throws NoSuchMethodException {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterTypes().length == 1) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(type.getName() + " has no single-argument constructor");
    }

    private static void fail(Object callback, String type, String message) {
        try {
            Constructor<?> failureConstructor = null;
            for (Constructor<?> constructor : failureClass().getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3 && parameterTypes[0].isEnum()) {
                    failureConstructor = constructor;
                }
            }
            if (failureConstructor == null) {
                throw new NoSuchMethodException("No failure constructor");
            }

            Object failureType = enumValue(failureConstructor.getParameterTypes()[0], type);
            complete(callback, failureConstructor.newInstance(failureType, message, null));
        }
        catch (Throwable throwable) {
            Log.e(TAG, "Could not deliver the failure", throwable);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object enumValue(Class<?> typeClass, String name) {
        return Enum.valueOf((Class<Enum>) typeClass.asSubclass(Enum.class), name);
    }

    private static void complete(Object callback, Object value) throws Exception {
        Constructor<?> resultConstructor = resultClass().getDeclaredConstructor(Object.class);
        resultConstructor.setAccessible(true);

        Method invoke = callback.getClass().getMethod("invoke", Object.class);
        invoke.setAccessible(true);
        invoke.invoke(callback, resultConstructor.newInstance(value));
    }

    private static JSONObject decodeClaims(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return new JSONObject();
            }
            byte[] payload = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return new JSONObject(new String(payload, "UTF-8"));
        }
        catch (Throwable throwable) {
            return new JSONObject();
        }
    }

    private static final class TokenCallback implements AccountManagerCallback<Bundle> {

        private final Object callback;

        TokenCallback(Object callback) {
            this.callback = callback;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle result = future.getResult();
                String idToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (idToken == null) {
                    fail(this.callback, "NO_CREDENTIAL", "GmsCore returned no token");
                    return;
                }
                succeed(this.callback, idToken, result.getString(AccountManager.KEY_ACCOUNT_NAME));
            }
            catch (Throwable throwable) {
                fail(this.callback, "UNKNOWN", String.valueOf(throwable.getMessage()));
            }
        }

    }

}
