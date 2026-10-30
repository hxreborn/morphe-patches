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
package app.hxreborn.extension.bereal.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public final class BackupRunTest {

    private final List<String> marked = new ArrayList<>();

    private final Set<String> done = new HashSet<>();

    private final List<String> fetched = new ArrayList<>();

    private final List<int[]> progress = new ArrayList<>();

    private IOException momentError;

    private MemorySaver.Result result = MemorySaver.Result.SAVED;

    private BackupRun.Outcome run(final String... ids) {
        final StringBuilder page = new StringBuilder("{\"data\":[");
        for (int i = 0; i < ids.length; i++) {
            page.append(i > 0 ? "," : "").append("{\"momentId\":\"").append(ids[i]).append("\"}");
        }
        page.append("]}");
        final MemoriesApi api = new MemoriesApi() {
            @Override
            public String memories() {
                return page.toString();
            }

            @Override
            public String moment(final String momentId) throws IOException {
                fetched.add(momentId);
                if (momentError != null) {
                    throw momentError;
                }
                return "{\"posts\":[{\"id\":\"p-" + momentId
                        + "\",\"primary\":{\"url\":\"https://cdn.example/a.webp\"}}]}";
            }
        };
        final PostSaver saver = post -> result;
        final SavedMoments store = new SavedMoments() {
            @Override
            public Set<String> done() {
                return done;
            }

            @Override
            public void mark(final String momentId) {
                marked.add(momentId);
            }
        };
        final BackupProgress listener = (finished, total) -> progress.add(new int[] {finished, total});
        return new BackupRun(api, saver, store, listener, 0).run(new AtomicBoolean());
    }

    @Test
    public void savesNewMomentsAndMarksThem() {
        // when
        final BackupRun.Outcome outcome = run("m1", "m2");

        // then
        assertEquals(2, outcome.saved);
        assertFalse(outcome.stoppedEarly);
        assertEquals(Arrays.asList("m1", "m2"), marked);
        assertEquals(2, progress.get(progress.size() - 1)[0]);
    }

    @Test
    public void skipsMomentsAlreadyInTheStore() {
        // given
        done.add("m1");

        // when
        final BackupRun.Outcome outcome = run("m1", "m2");

        // then
        assertEquals(Collections.singletonList("m2"), fetched);
        assertEquals(1, outcome.saved);
        assertEquals(1, outcome.skipped);
    }

    @Test
    public void stopsOnAnExpiredLogin() {
        // given
        momentError = new IOException("HTTP 401");

        // when
        final BackupRun.Outcome outcome = run("m1", "m2");

        // then
        assertTrue(outcome.stoppedEarly);
        assertEquals(BackupRun.REASON_LOGIN, outcome.reason);
        assertEquals(Collections.singletonList("m1"), fetched);
        assertTrue(marked.isEmpty());
    }

    @Test
    public void stopsAfterThreeFailedMomentsInARow() {
        // given
        result = MemorySaver.Result.FAILED;

        // when
        final BackupRun.Outcome outcome = run("m1", "m2", "m3", "m4");

        // then
        assertEquals(BackupRun.REASON_FAILURES, outcome.reason);
        assertEquals(3, outcome.failed);
        assertEquals(Arrays.asList("m1", "m2", "m3"), fetched);
        assertTrue(marked.isEmpty());
    }

}
