/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * See LICENSE for GPLv3.
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package hx.util;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class Tasks {

    private Tasks() {
    }

    public static <T> T await(Future<T> task) throws IOException {
        try {
            return task.get();
        } catch (ExecutionException ex) {
            throw failureOf(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
    }

    public static IOException failureOf(ExecutionException ex) {
        Throwable cause = ex.getCause();
        return (cause instanceof IOException) ? (IOException) cause : new IOException(cause);
    }

}
