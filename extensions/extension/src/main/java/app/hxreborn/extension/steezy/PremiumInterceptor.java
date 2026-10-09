/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.steezy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("unused")
public final class PremiumInterceptor implements Interceptor {

    private static final String API_HOST = "v2.api.steezy.co";

    private static final String CLASS_LOCKED = "\"canUserTakeClass\":false";

    private static final String CLASS_OPEN = "\"canUserTakeClass\":true";

    private static final String SUBSCRIPTION_FIELD = "\"primarySubscription\"";

    private static final String PREMIUM_ACCESS = "premium";

    private static final String PROVIDER_ID = "revenuecat";

    private static final String JSON_SUBTYPE = "json";

    private static final String CONTENT_LENGTH = "Content-Length";

    private PremiumInterceptor() {
    }

    public static OkHttpClient.Builder install(OkHttpClient.Builder builder) {
        return builder.addInterceptor(new PremiumInterceptor());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        ResponseBody body = response.body();
        if (body == null || !API_HOST.equals(request.url().host()) || !isJson(body.contentType())) {
            return response;
        }
        String json = body.string();
        byte[] unlocked = unlock(json).getBytes(StandardCharsets.UTF_8);
        return response.newBuilder()
            .removeHeader(CONTENT_LENGTH)
            .body(ResponseBody.create(body.contentType(), unlocked))
            .build();
    }

    private static boolean isJson(MediaType type) {
        return type != null && type.subtype().endsWith(JSON_SUBTYPE);
    }

    private static String unlock(String json) {
        String unlocked = json.replace(CLASS_LOCKED, CLASS_OPEN);
        if (!unlocked.contains(SUBSCRIPTION_FIELD)) {
            return unlocked;
        }
        try {
            JSONObject root = new JSONObject(unlocked);
            JSONObject me = root.getJSONObject("data").optJSONObject("me");
            if (me == null || !me.has("isActive")) {
                return unlocked;
            }
            me.put("isActive", true);
            if (me.isNull("primarySubscription")) {
                me.put("primarySubscription", premiumSubscription());
            }
            return root.toString();
        } catch (JSONException ex) {
            return unlocked;
        }
    }

    private static JSONObject premiumSubscription() throws JSONException {
        JSONObject plan = new JSONObject().put("id", PREMIUM_ACCESS)
            .put("name", "Premium")
            .put("accessType", PREMIUM_ACCESS)
            .put("isSpecial", false)
            .put("isLegacy", false)
            .put("periodUnit", "year")
            .put("platform", "android")
            .put("priceInCents", 0)
            .put("monthlyCostInCents", 0)
            .put("provider", provider())
            .put("__typename", "Plan");
        return new JSONObject().put("id", PREMIUM_ACCESS)
            .put("isActive", true)
            .put("isCancelable", false)
            .put("isReactivatableV2", false)
            .put("isPauseScheduled", false)
            .put("isPastDue", false)
            .put("nextBillingAt", JSONObject.NULL)
            .put("status", "active")
            .put("trialEndTime", JSONObject.NULL)
            .put("currentTermEnd", JSONObject.NULL)
            .put("cancelationTime", JSONObject.NULL)
            .put("pauseTime", JSONObject.NULL)
            .put("resumeTime", JSONObject.NULL)
            .put("provider", provider())
            .put("plan", plan)
            .put("invoiceEstimate", JSONObject.NULL)
            .put("invoiceDue", JSONObject.NULL)
            .put("__typename", "Subscription");
    }

    private static JSONObject provider() throws JSONException {
        return new JSONObject().put("id", PROVIDER_ID).put("name", PROVIDER_ID).put("__typename", "Provider");
    }

}
