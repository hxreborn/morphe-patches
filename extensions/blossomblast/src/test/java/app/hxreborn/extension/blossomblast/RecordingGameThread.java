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
package app.hxreborn.extension.blossomblast;

import java.util.ArrayList;
import java.util.List;

final class RecordingGameThread implements GameThread {

    private final List<Runnable> tasks = new ArrayList<>();

    private final List<Object> applications = new ArrayList<>();

    private Object failingApplication;

    private Runnable beforeFailure = () -> {
    };

    private int failures;

    void failPosts(Object application, int count, Runnable action) {
        this.failingApplication = application;
        this.failures = count;
        this.beforeFailure = action;
    }

    @Override
    public void post(Object application, Runnable task) {
        if (application == this.failingApplication && this.failures > 0) {
            this.failures--;
            this.beforeFailure.run();
            throw new IllegalStateException("posting rejected");
        }
        this.applications.add(application);
        this.tasks.add(task);
    }

    int postedTasks() {
        return this.tasks.size();
    }

    Object application(int index) {
        return this.applications.get(index);
    }

    void runTask(int index) {
        this.tasks.get(index).run();
    }

    void runLastTask() {
        runTask(this.tasks.size() - 1);
    }

}
