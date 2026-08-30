package app.hxreborn.extension.allinonecalculator;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class GmsCoreSignIn {
    private static final String TAG = "GmsCoreSignIn";

    private static final String ACCOUNT_TYPE = "app.revanced";

    private static final String PIGEON_PACKAGE = "io.flutter.plugins.googlesignin.";

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

        AccountManager.get(activity).getAuthTokenByFeatures(
                ACCOUNT_TYPE,
                "audience:server:client_id:" + serverClientId,
                null,
                activity,
                null,
                null,
                new TokenCallback(callback),
                null);
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
                    fail(callback, "NO_CREDENTIAL", "GmsCore returned no token");
                    return;
                }
                succeed(callback, idToken, result.getString(AccountManager.KEY_ACCOUNT_NAME));
            } catch (Throwable throwable) {
                fail(callback, "UNKNOWN", String.valueOf(throwable.getMessage()));
            }
        }
    }

    private static void succeed(Object callback, String idToken, String accountName) {
        try {
            JSONObject claims = decodeClaims(idToken);
            String email = claims.optString("email", accountName);
            String subject = claims.optString("sub", email);

            Class<?> credentialClass = Class.forName(PIGEON_PACKAGE + "PlatformGoogleIdTokenCredential");
            Constructor<?> credentialConstructor = credentialClass.getConstructor(
                    String.class, String.class, String.class, String.class,
                    String.class, String.class, String.class);
            Object credential = credentialConstructor.newInstance(
                    claims.optString("name", null),
                    claims.optString("given_name", null),
                    claims.optString("family_name", null),
                    email,
                    subject,
                    idToken,
                    claims.optString("picture", null));

            Class<?> successClass = Class.forName(PIGEON_PACKAGE + "GetCredentialSuccess");
            Object success = successClass.getConstructor(credentialClass).newInstance(credential);

            complete(callback, success);
        } catch (Throwable throwable) {
            Log.e(TAG, "Could not deliver the credential", throwable);
            fail(callback, "UNKNOWN", String.valueOf(throwable.getMessage()));
        }
    }

    private static void fail(Object callback, String type, String message) {
        try {
            Class<?> typeClass = Class.forName(PIGEON_PACKAGE + "GetCredentialFailureType");
            Object failureType = enumValue(typeClass, type);

            Class<?> failureClass = Class.forName(PIGEON_PACKAGE + "GetCredentialFailure");
            Object failure = failureClass
                    .getConstructor(typeClass, String.class, String.class)
                    .newInstance(failureType, message, null);

            complete(callback, failure);
        } catch (Throwable throwable) {
            Log.e(TAG, "Could not deliver the failure", throwable);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> typeClass, String name) {
        return Enum.valueOf((Class<Enum>) typeClass.asSubclass(Enum.class), name);
    }

    private static void complete(Object callback, Object value) throws Exception {
        Class<?> resultUtils = Class.forName(PIGEON_PACKAGE + "ResultUtilsKt");
        for (Method method : resultUtils.getDeclaredMethods()) {
            if (method.getName().equals("completeWithValue") && method.getParameterTypes().length == 2) {
                method.setAccessible(true);
                method.invoke(null, callback, value);
                return;
            }
        }
        throw new NoSuchMethodException("completeWithValue");
    }

    private static JSONObject decodeClaims(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) return new JSONObject();
            byte[] payload = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return new JSONObject(new String(payload, "UTF-8"));
        } catch (Throwable throwable) {
            return new JSONObject();
        }
    }
}
