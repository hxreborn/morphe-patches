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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;

final class BackupRun {

    static final String REASON_LOGIN = "login";

    static final String REASON_LIST = "list";

    static final String REASON_FAILURES = "failures";

    static final String REASON_ERROR = "error";

    private static final int MAX_FAILED_MOMENTS_IN_A_ROW = 3;

    private final MemoriesApi api;

    private final PostSaver saver;

    private final SavedMoments store;

    private final BackupProgress progress;

    private final long delayMillis;

    BackupRun(final MemoriesApi api, final PostSaver saver, final SavedMoments store,
            final BackupProgress progress, final long delayMillis) {
        this.api = api;
        this.saver = saver;
        this.store = store;
        this.progress = progress;
        this.delayMillis = delayMillis;
    }

    // Skipped counts posts the saver skipped and moments already backed up (one each)
    Outcome run(final AtomicBoolean cancel) {
        final Outcome outcome = new Outcome();
        try {
            run(cancel, outcome);
        } catch (RuntimeException ex) {
            // The counts so far stay in the outcome
            outcome.stop(REASON_ERROR);
        }
        return outcome;
    }

    static boolean isValidMomentId(final String momentId) {
        return momentId != null && !momentId.isEmpty() && !momentId.startsWith(".");
    }

    private void run(final AtomicBoolean cancel, final Outcome outcome) {
        final List<String> moments = new ArrayList<>();
        try {
            final String body = this.api.memories();
            for (final String id : MemoriesJson.momentIds(body)) {
                if (isValidMomentId(id)) {
                    moments.add(id);
                }
            }
        } catch (IOException | JSONException | RuntimeException ex) {
            outcome.stop(isLogin(ex) ? REASON_LOGIN : REASON_LIST);
            return;
        }
        final Set<String> done = this.store.done();
        final int total = moments.size();
        int finished = 0;
        for (final String momentId : moments) {
            if (done.contains(momentId)) {
                finished++;
                outcome.skipped++;
            }
        }
        this.progress.update(finished, total);
        int failedInARow = 0;
        for (final String momentId : moments) {
            if (done.contains(momentId)) {
                continue;
            }
            if (cancel.get() || !pause() || cancel.get()) {
                outcome.cancelled = true;
                return;
            }
            boolean allSaved = true;
            try {
                final List<MemoryPost> posts = MemoriesJson.posts(momentId, this.api.moment(momentId));
                for (final MemoryPost post : posts) {
                    if (cancel.get()) {
                        // The moment stays unmarked, so the next run retries it
                        outcome.cancelled = true;
                        return;
                    }
                    final MemorySaver.Result result = save(post);
                    if (result == MemorySaver.Result.SAVED) {
                        outcome.saved++;
                    } else if (result == MemorySaver.Result.SKIPPED) {
                        outcome.skipped++;
                    } else {
                        outcome.failed++;
                        allSaved = false;
                    }
                }
            } catch (IOException | JSONException | RuntimeException ex) {
                if (isLogin(ex)) {
                    outcome.stop(REASON_LOGIN);
                    return;
                }
                outcome.failed++;
                allSaved = false;
            }
            if (allSaved) {
                failedInARow = 0;
                this.store.mark(momentId);
                finished++;
                this.progress.update(finished, total);
            } else if (++failedInARow >= MAX_FAILED_MOMENTS_IN_A_ROW) {
                outcome.stop(REASON_FAILURES);
                return;
            }
        }
        return;
    }

    private MemorySaver.Result save(final MemoryPost post) {
        try {
            return this.saver.save(post);
        } catch (RuntimeException ex) {
            // A post without photo urls throws before the saver can report it
            return MemorySaver.Result.FAILED;
        }
    }

    private boolean pause() {
        if (this.delayMillis <= 0) {
            return true;
        }
        try {
            Thread.sleep(this.delayMillis);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isLogin(final Exception ex) {
        return ex instanceof IOException && "HTTP 401".equals(ex.getMessage());
    }

    static final class Outcome {

        int saved;

        int skipped;

        int failed;

        boolean cancelled;

        boolean stoppedEarly;

        String reason;

        private void stop(final String reason) {
            this.stoppedEarly = true;
            this.reason = reason;
        }
    }
}
