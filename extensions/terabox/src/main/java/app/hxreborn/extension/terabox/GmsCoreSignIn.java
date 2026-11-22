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
package app.hxreborn.extension.terabox;

import java.io.IOException;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorDescription;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class GmsCoreSignIn {

    private static final String ACCOUNT_TYPE = "app.revanced";

    private static final String SILENT_FAILURE_MESSAGE = "cracked app blocked";

    private static final int SILENT_FAILURE_CODE = -1;

    private static final String SIGN_IN_FAILED = "Google sign-in failed";

    private static final String GMS_CORE_MISSING = "Google sign-in needs MicroG-RE";

    private GmsCoreSignIn() {
    }

    public static void signIn(Activity activity, String clientId, Object listener) {
        if (activity == null || clientId == null || clientId.isEmpty() || listener == null) {
            return;
        }
        if (!hasGmsCoreAuthenticator(activity)) {
            Log.w(PatchSettings.LOG_TAG, "GmsCore authenticator is not installed");
            failSignIn(activity, listener, GMS_CORE_MISSING);
            return;
        }
        try {
            AccountManager.get(activity)
                .getAuthTokenByFeatures(ACCOUNT_TYPE, "audience:server:client_id:" + clientId, null, activity, null,
                        null, new TokenCallback(activity, listener), null);
        } catch (RuntimeException ex) {
            Log.w(PatchSettings.LOG_TAG, "Cannot start GmsCore sign-in: " + ex.getClass().getSimpleName());
            failSignIn(activity, listener, SIGN_IN_FAILED);
        }
    }

    private static boolean hasGmsCoreAuthenticator(Activity activity) {
        for (AuthenticatorDescription authenticator : AccountManager.get(activity).getAuthenticatorTypes()) {
            if (ACCOUNT_TYPE.equals(authenticator.type)) {
                return true;
            }
        }
        return false;
    }

    private static void failSignIn(Activity activity, Object listener, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        reportFailure(listener);
    }

    private static void reportSuccess(Object listener, String token, String accountName) {
        if (listener instanceof WebView) {
            ((WebView) listener).evaluateJavascript(
                    "window.__tbDeliver&&window.__tbDeliver(" + JSONObject.quote(token) + ")", null);
            return;
        }
        deliverSuccess(listener, token, accountName);
    }

    private static void reportFailure(Object listener) {
        if (listener instanceof WebView) {
            return;
        }
        deliverFailure(listener, SILENT_FAILURE_MESSAGE, SILENT_FAILURE_CODE);
    }

    private static void deliverSuccess(Object listener, String token, String accountName) {
    }

    private static void deliverFailure(Object listener, String message, int code) {
    }

    private static final class TokenCallback implements AccountManagerCallback<Bundle> {

        private final Activity activity;

        private final Object listener;

        TokenCallback(Activity activity, Object listener) {
            this.activity = activity;
            this.listener = listener;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle result = future.getResult();
                String token = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (token == null || token.isEmpty()) {
                    Log.w(PatchSettings.LOG_TAG, "GmsCore returned no Google token");
                    failSignIn(this.activity, this.listener, SIGN_IN_FAILED);
                    return;
                }
                reportSuccess(this.listener, token, result.getString(AccountManager.KEY_ACCOUNT_NAME));
            } catch (OperationCanceledException ex) {
                Log.i(PatchSettings.LOG_TAG, "GmsCore sign-in cancelled");
                reportFailure(this.listener);
            } catch (AuthenticatorException | IOException ex) {
                Log.w(PatchSettings.LOG_TAG, "GmsCore sign-in failed: " + ex.getClass().getSimpleName());
                failSignIn(this.activity, this.listener, SIGN_IN_FAILED);
            }
        }

    }

}
