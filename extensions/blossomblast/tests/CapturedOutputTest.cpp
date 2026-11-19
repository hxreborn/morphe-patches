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
#include "../src/CapturedOutput.h"

#include <sys/mman.h>

#include <cassert>
#include <cstdio>
#include <cstring>

namespace {

constexpr char REPLY[] = "{\n    \"syncResult\": \"true\"\n}";
constexpr char COMPACT_REPLY[] = "{     \"syncResult\": \"true\" }";
constexpr char LATER_TEXT[] = "true";
constexpr size_t PAGE_BYTES = 0x4000;
constexpr size_t CROSSING_BYTES = 8;

struct Pages {
    uintptr_t readable;
    uintptr_t unreadable;

    Pages() {
        void *memory = mmap(nullptr, 2 * PAGE_BYTES, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        assert(memory != MAP_FAILED);
        readable = reinterpret_cast<uintptr_t>(memory);
        unreadable = readable + PAGE_BYTES;
        assert(mprotect(reinterpret_cast<void *>(unreadable), PAGE_BYTES, PROT_NONE) == 0);
    }

    ~Pages() {
        munmap(reinterpret_cast<void *>(readable), 2 * PAGE_BYTES);
    }

    Pages(const Pages &) = delete;
    Pages &operator=(const Pages &) = delete;

    uintptr_t put(uintptr_t at, const char *text, size_t size) const {
        std::memcpy(reinterpret_cast<void *>(at), text, size);
        return at;
    }
};

void readableTextIsCapturedWithControlCharactersReplaced() {
    Pages pages;
    CapturedOutput output;
    output.append(pages.put(pages.readable, REPLY, std::strlen(REPLY)), std::strlen(REPLY));
    assert(output.take() == COMPACT_REPLY);
    assert(output.take().empty());
}

void unreadableSpanEmptiesTheSink() {
    Pages pages;
    CapturedOutput output;
    output.append(pages.put(pages.readable, REPLY, std::strlen(REPLY)), std::strlen(REPLY));
    output.append(pages.unreadable, std::strlen(REPLY));
    output.append(pages.put(pages.readable, LATER_TEXT, std::strlen(LATER_TEXT)), std::strlen(LATER_TEXT));
    assert(output.take().empty());
}

void spanEndingInUnreadablePageIsRejected() {
    Pages pages;
    CapturedOutput output;
    output.append(pages.unreadable - CROSSING_BYTES / 2, CROSSING_BYTES);
    assert(output.take().empty());
}

void nullSpanIsRejected() {
    Pages pages;
    CapturedOutput output;
    output.append(0, std::strlen(REPLY));
    output.append(pages.put(pages.readable, LATER_TEXT, std::strlen(LATER_TEXT)), std::strlen(LATER_TEXT));
    assert(output.take().empty());
}

void spanOverCaptureLimitIsRejected() {
    Pages pages;
    CapturedOutput output;
    output.append(pages.readable, MAX_CAPTURED_BYTES + 1);
    assert(output.take().empty());
}

void rejectedSinkDropsReplyAfterPoll() {
    Pages pages;
    CapturedOutput output;
    output.append(pages.unreadable, std::strlen(REPLY));
    assert(output.take().empty());
    output.append(pages.put(pages.readable, REPLY, std::strlen(REPLY)), std::strlen(REPLY));
    assert(output.take().empty());
}

}

int main() {
    readableTextIsCapturedWithControlCharactersReplaced();
    unreadableSpanEmptiesTheSink();
    spanEndingInUnreadablePageIsRejected();
    nullSpanIsRejected();
    spanOverCaptureLimitIsRejected();
    rejectedSinkDropsReplyAfterPoll();
    std::puts("CapturedOutputTest passed");
}
