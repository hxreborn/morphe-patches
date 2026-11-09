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
package app.hxreborn.extension.realmelink;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import app.hxreborn.extension.shared.ProxyHandler;

@SuppressWarnings("unused")
public final class SessionRenewal {

    private static final String LOG_TAG = "hxRealmeLink";

    private static final long SDK_TOKEN_TIMEOUT_SECONDS = 10L;

    private SessionRenewal() {

    }

    public static boolean renew() {
        try {
            Object accountService = accountService();
            String storedToken = heytapToken(accountService);
            String sdkToken = sdkHeytapTokenOrNull();
            boolean sdkTokenDiffers = sdkToken != null && !sdkToken.isEmpty() && !sdkToken.equals(storedToken);
            if (sdkTokenDiffers && exchangeAndStore(accountService, sdkToken)) {
                return true;
            }
            return storedToken != null && exchangeAndStore(accountService, storedToken);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Cannot renew the session", ex);
            return false;
        }
    }

    private static String sdkHeytapTokenOrNull() {
        try {
            return sdkHeytapToken();
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Cannot read the HeyTap token from the account SDK", ex);
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.w(LOG_TAG, "Interrupted while waiting for the account SDK token", ex);
            return null;
        }
    }

    private static String sdkHeytapToken() throws InterruptedException {
        SdkTokenCallback callback = new SdkTokenCallback();
        Class<?> callbackType = sdkTokenCallbackType();
        requestSdkToken(
                Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType }, callback));
        if (!callback.received.await(SDK_TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            Log.w(LOG_TAG, "The account SDK did not return a token in time");
            return null;
        }
        Object response = callback.response;
        return (response != null) ? sdkAccessToken(response) : null;
    }

    private static boolean exchangeAndStore(Object accountService, String heytapToken) {
        try {
            Object result = exchange(heytapToken);
            if (!isSuccess(result)) {
                Log.w(LOG_TAG, "The token exchange returned an unsuccessful result");
                return false;
            }

            String linkToken = linkToken(result);
            if (linkToken == null) {
                Log.w(LOG_TAG, "The token exchange returned no link token");
                return false;
            }
            storeTokens(accountService, heytapToken, linkToken);
            return true;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Cannot exchange the HeyTap token", ex);
            return false;
        }
    }

    static Object accountService() {
        return null;
    }

    static String heytapToken(Object accountService) {
        return null;
    }

    static Class<?> sdkTokenCallbackType() {
        return null;
    }

    static void requestSdkToken(Object callback) {
    }

    static String sdkAccessToken(Object response) {
        return null;
    }

    static Object exchange(String heytapToken) {
        return null;
    }

    static boolean isSuccess(Object result) {
        return false;
    }

    static String linkToken(Object result) {
        return null;
    }

    static void storeTokens(Object accountService, String heytapToken, String linkToken) {
    }

    private static final class SdkTokenCallback extends ProxyHandler {

        private final CountDownLatch received = new CountDownLatch(1);

        private volatile Object response;

        @Override
        protected String describe() {
            return "hxRealmeLinkSdkToken";
        }

        @Override
        protected Object handle(Method method, Object[] args) {
            if (args != null && args.length == 1) {
                this.response = args[0];
                this.received.countDown();
            }
            return null;
        }

    }

}
