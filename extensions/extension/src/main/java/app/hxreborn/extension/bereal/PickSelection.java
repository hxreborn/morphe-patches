/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.util.Collection;

final class PickSelection<T> {

    enum Step {

        BACK, FRONT, DONE

    }

    private T back;

    private T front;

    T back() {
        return this.back;
    }

    T front() {
        return this.front;
    }

    boolean isEmpty() {
        return this.back == null;
    }

    boolean isComplete() {
        return this.front != null;
    }

    Step step() {
        if (this.back == null) {
            return Step.BACK;
        }
        return (this.front == null) ? Step.FRONT : Step.DONE;
    }

    void toggle(T item) {
        if (item.equals(this.back)) {
            this.back = this.front;
            this.front = null;
        } else if (item.equals(this.front)) {
            this.front = null;
        } else {
            add(item);
        }
    }

    void add(T item) {
        if (this.back == null) {
            this.back = item;
        } else {
            this.front = item;
        }
    }

    void retain(Collection<T> available) {
        if (this.back != null && !available.contains(this.back)) {
            this.back = null;
        }
        if (this.front != null && !available.contains(this.front)) {
            this.front = null;
        }
        if (this.back == null) {
            this.back = this.front;
            this.front = null;
        }
    }

    void clear() {
        this.back = null;
        this.front = null;
    }

}
