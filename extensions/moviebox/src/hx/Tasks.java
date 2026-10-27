/*
 * Copyright (C) 2026 hxreborn
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
 */
package hx;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

final class Tasks {

    private Tasks() {
    }

    static <T> T await(Future<T> task) throws IOException {
        try {
            return task.get();
        } catch (ExecutionException ex) {
            throw failureOf(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
    }

    static IOException failureOf(ExecutionException ex) {
        Throwable cause = ex.getCause();
        return (cause instanceof IOException) ? (IOException) cause : new IOException(cause);
    }

}
