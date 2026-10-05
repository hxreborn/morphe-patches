/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.keepa;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class AccountBridge {

    private static final String TAG = "hx.KeepaAccounts";

    private AccountBridge() {

    }

    public static String call(Context context, String method, String json) {
        try {
            return new JSONObject().put("value", dispatch(context, AccountStore.of(context), method, json)).toString();
        }
        catch (Throwable throwable) {
            Log.e(TAG, "Bridge call " + method + " failed", throwable);
            return "{\"error\":" + JSONObject.quote(method + ": " + throwable) + "}";
        }
    }

    static synchronized void update(Context context, AccountsUpdate change) throws JSONException {
        final AccountStore store = AccountStore.of(context);
        final Accounts accounts = store.load();
        change.apply(accounts);
        store.save(accounts);
    }

    static synchronized void requestOperation(Context context, JSONObject operation) throws JSONException {
        AccountStore.of(context).writePending(operation.toString());
    }

    static synchronized Object dispatch(Context context, AccountStore store, String method, String json)
            throws JSONException {
        final JSONObject arguments = (json == null || json.isEmpty()) ? new JSONObject() : new JSONObject(json);
        switch (method) {
            case "setPending":
                store.writePending(json);
                return snapshot(store, store.load());
            case "clearPending":
                store.clearPending();
                return snapshot(store, store.load());
            case "openAccounts":
                context
                    .startActivity(new Intent().setClassName(context.getPackageName(), AccountsActivity.class.getName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "";
            default:
                break;
        }

        final Accounts accounts = store.load();
        final long now = System.currentTimeMillis();
        switch (method) {
            case "load":
                return snapshot(store, accounts);
            case "allocate":
                return accounts.allocate(arguments.getString("asin"), now);
            case "mergeOverviews":
                final JSONObject merged = accounts.mergeOverviews(arguments.getJSONObject("responses"), now);
                if (arguments.getBoolean("commit")) {
                    store.save(accounts);
                }
                return merged;
            case "onServerData":
                accounts.upsertByUsername(arguments.getString("token"), arguments.getString("username"),
                        arguments.optString("email", ""), now);
                break;
            case "upsertToken":
                accounts.updateToken(arguments.getString("id"), arguments.getString("token"));
                break;
            case "setPrimary":
                accounts.setPrimary(arguments.getString("id"));
                break;
            case "invalidate":
                accounts.invalidate(arguments.getString("id"));
                break;
            case "remove":
                accounts.remove(arguments.getString("id"));
                break;
            case "applyAddResult":
                accounts.applyAddResult(arguments.getString("asin"), arguments.getString("accountId"),
                        arguments.getBoolean("ok"), arguments.optString("errorType", ""));
                break;
            case "applyReject":
                accounts.applyReject(arguments.getString("accountId"), arguments.getInt("status"), now);
                break;
            case "applyDelete":
                final JSONArray asins = arguments.getJSONArray("asins");
                for (int i = 0; i < asins.length(); i++) {
                    accounts.applyDelete(asins.getString(i), arguments.getString("accountId"));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown bridge method " + method);
        }
        store.save(accounts);
        return snapshot(store, accounts);
    }

    private static JSONObject snapshot(AccountStore store, Accounts accounts) throws JSONException {
        final String pending = store.pending();
        return new JSONObject().put("accounts", accounts.toJson())
            .put("owners", accounts.owners())
            .put("pending", (pending.isEmpty()) ? JSONObject.NULL : new JSONObject(pending))
            .put("summary", accounts.summary())
            .put("limitTotal", accounts.limitTotal());
    }

}
