/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class HttpStatusTest {

    @Test
    public void successCoversExactlyTheTwoHundredRange() {
        assertFalse(HttpStatus.isSuccess(199));
        assertTrue(HttpStatus.isSuccess(200));
        assertTrue(HttpStatus.isSuccess(204));
        assertTrue(HttpStatus.isSuccess(299));
        assertFalse(HttpStatus.isSuccess(300));
        assertFalse(HttpStatus.isSuccess(404));
        assertFalse(HttpStatus.isSuccess(500));
    }

    @Test
    public void noResponseIsNotSuccess() {
        assertFalse(HttpStatus.isSuccess(HttpStatus.NONE));
        assertFalse(HttpStatus.isSuccess(0));
    }

    @Test
    public void onlyNotFoundIsPending() {
        assertTrue(HttpStatus.isPending(404));
        for (int code : new int[] { 200, 202, 403, 405, 410, 500, 0, HttpStatus.NONE }) {
            assertFalse(String.valueOf(code), HttpStatus.isPending(code));
        }
    }

    @Test
    public void namesEveryKnownCode() {
        int[] codes = { 200, 201, 202, 204, 301, 302, 304, 400, 401, 403, 404, 408, 409, 429, 500, 502, 503, 504 };
        String[] reasons = { "OK", "Created", "Accepted", "No Content", "Moved Permanently", "Found", "Not Modified",
                "Bad Request", "Unauthorized", "Forbidden", "Not Found", "Request Timeout", "Conflict",
                "Too Many Requests", "Internal Server Error", "Bad Gateway", "Service Unavailable", "Gateway Timeout" };
        for (int i = 0; i < codes.length; i++) {
            assertEquals(String.valueOf(codes[i]), reasons[i], HttpStatus.reason(codes[i]));
        }
    }

    @Test
    public void unknownCodesHaveNoReason() {
        for (int code : new int[] { 0, 100, 199, 203, 299, 418, 499, 501, 599, 600, HttpStatus.NONE }) {
            assertEquals(String.valueOf(code), "", HttpStatus.reason(code));
        }
    }

}
