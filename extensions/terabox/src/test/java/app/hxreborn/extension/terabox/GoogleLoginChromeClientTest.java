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

import java.util.concurrent.TimeUnit;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorDescription;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAccountManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { 23, 36 },
        shadows = { GoogleLoginChromeClientTest.Accounts.class, GoogleLoginChromeClientTest.Delivery.class })
public final class GoogleLoginChromeClientTest {

    @Test
    public void requestsTokenWithTheHostClientIdAndDeliversIt() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Object listener = new Object();
        Accounts.callback = null;
        Delivery.listener = null;
        GmsCoreSignIn.signIn(activity, "native-client", listener);
        Assert.assertEquals("app.revanced", Accounts.accountType);
        Assert.assertEquals("audience:server:client_id:native-client", Accounts.tokenType);
        Assert.assertSame(activity, Accounts.activity);
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_AUTHTOKEN, "token");
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, "account");
        Accounts.callback.run(new Result(bundle));
        Assert.assertSame(listener, Delivery.listener);
        Assert.assertEquals("token", Delivery.token);
        Assert.assertEquals("account", Delivery.account);
    }

    @Test
    public void deliversTokenToTheWebLoginPage() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        WebView view = new WebView(activity);
        Accounts.callback = null;
        Delivery.listener = null;
        GmsCoreSignIn.signIn(activity, "web-client", view);
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_AUTHTOKEN, "token");
        Accounts.callback.run(new Result(bundle));
        Assert.assertNull(Delivery.listener);
        Assert.assertEquals("window.__tbDeliver&&window.__tbDeliver(\"token\")",
                Shadows.shadowOf(view).getLastEvaluatedJavascript());
    }

    @Test
    public void reportsSilentFailureWithoutGmsCore() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Object listener = new Object();
        Accounts.callback = null;
        Accounts.authenticatorInstalled = false;
        Delivery.failureListener = null;
        try {
            GmsCoreSignIn.signIn(activity, "native-client", listener);
        } finally {
            Accounts.authenticatorInstalled = true;
        }
        Assert.assertNull(Accounts.callback);
        Assert.assertSame(listener, Delivery.failureListener);
        Assert.assertEquals("cracked app blocked", Delivery.failureMessage);
        Assert.assertEquals(-1, Delivery.failureCode);
    }

    @Test
    public void skipsRequestsWithoutClientId() {
        Accounts.callback = null;
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        GmsCoreSignIn.signIn(activity, "", new Object());
        Assert.assertNull(Accounts.callback);
    }

    @Implements(AccountManager.class)
    public static class Accounts extends ShadowAccountManager {

        static String accountType;

        static String tokenType;

        static Activity activity;

        static AccountManagerCallback<Bundle> callback;

        static boolean authenticatorInstalled = true;

        @Implementation
        protected AuthenticatorDescription[] getAuthenticatorTypes() {
            if (!authenticatorInstalled) {
                return new AuthenticatorDescription[0];
            }
            return new AuthenticatorDescription[] {
                    new AuthenticatorDescription("app.revanced", "app.revanced.android.gms", 0, 0, 0, 0) };
        }

        @Implementation
        protected AccountManagerFuture<Bundle> getAuthTokenByFeatures(String type, String token, String[] features,
                Activity currentActivity, Bundle addOptions, Bundle tokenOptions, AccountManagerCallback<Bundle> result,
                Handler handler) {
            accountType = type;
            tokenType = token;
            activity = currentActivity;
            callback = result;
            return null;
        }

    }

    @Implements(value = GmsCoreSignIn.class, isInAndroidSdk = false)
    public static final class Delivery {

        static Object listener;

        static String token;

        static String account;

        @Implementation
        protected static void deliverSuccess(Object currentListener, String currentToken, String accountName) {
            listener = currentListener;
            token = currentToken;
            account = accountName;
        }

        static Object failureListener;

        static String failureMessage;

        static int failureCode;

        @Implementation
        protected static void deliverFailure(Object currentListener, String message, int code) {
            failureListener = currentListener;
            failureMessage = message;
            failureCode = code;
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

}
