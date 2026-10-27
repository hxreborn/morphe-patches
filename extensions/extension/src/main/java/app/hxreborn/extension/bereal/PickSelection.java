/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import java.util.Collection;

final class PickSelection<T> {

    enum Side {

        BACK, FRONT

    }

    private T back;

    private T front;

    private Side target = Side.BACK;

    T back() {
        return this.back;
    }

    T front() {
        return this.front;
    }

    Side target() {
        return this.target;
    }

    void target(Side side) {
        this.target = side;
    }

    boolean isEmpty() {
        return this.back == null && this.front == null;
    }

    boolean isComplete() {
        return this.back != null && this.front != null;
    }

    void toggle(T item) {
        if (item.equals(this.back)) {
            this.back = null;
            this.target = Side.BACK;
        } else if (item.equals(this.front)) {
            this.front = null;
            this.target = Side.FRONT;
        } else {
            add(item);
        }
    }

    void add(T item) {
        if (this.target == Side.BACK) {
            this.back = item;
            if (this.front == null) {
                this.target = Side.FRONT;
            }
        } else {
            this.front = item;
            if (this.back == null) {
                this.target = Side.BACK;
            }
        }
    }

    void swap() {
        final T previousBack = this.back;
        this.back = this.front;
        this.front = previousBack;
        if (this.back == null && this.front != null) {
            this.target = Side.BACK;
        } else if (this.front == null && this.back != null) {
            this.target = Side.FRONT;
        }
    }

    void retain(Collection<T> available) {
        if (this.back != null && !available.contains(this.back)) {
            this.back = null;
            this.target = Side.BACK;
        }
        if (this.front != null && !available.contains(this.front)) {
            this.front = null;
            if (this.back != null) {
                this.target = Side.FRONT;
            }
        }
    }

    void clear() {
        this.back = null;
        this.front = null;
        this.target = Side.BACK;
    }

}
