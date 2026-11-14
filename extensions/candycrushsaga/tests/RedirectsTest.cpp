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
#include <sys/mman.h>
#include <unistd.h>

#include <cassert>
#include <cstdio>
#include <set>

int protectCalls = 0;
std::set<int> failures;

int injectedProtect(void *address, size_t size, int protection) {
    if (failures.count(++protectCalls) != 0) return -1;
    return mprotect(address, size, protection);
}

#define mprotect injectedProtect
#include "../src/Redirects.h"
#undef mprotect

void check(size_t pages, size_t entries, std::set<int> injected, bool pending, bool writable) {
    const size_t size = static_cast<size_t>(sysconf(_SC_PAGESIZE));
    auto *memory = static_cast<uintptr_t *>(mmap(nullptr, pages * size, PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
    assert(memory != MAP_FAILED);
    Redirects redirects;
    redirects.pageSize = size;
    redirects.pageCount = pages;
    redirects.changeCount = entries;
    for (size_t index = 0; index < pages; index++) {
        const uintptr_t address = reinterpret_cast<uintptr_t>(memory) + index * size;
        const int protection = PROT_READ | (writable ? PROT_WRITE : 0);
        redirects.pages[index] = {address, protection};
    }
    for (size_t index = 0; index < entries; index++) {
        const uintptr_t slot = reinterpret_cast<uintptr_t>(memory) + (index % pages) * size
                + (index / pages) * sizeof(uintptr_t);
        *reinterpret_cast<uintptr_t *>(slot) = 100 + index;
        redirects.changes[index] = {slot, 100 + index, 200 + index, 100 + index};
    }
    for (size_t index = 0; index < pages; index++) {
        const auto &page = redirects.pages[index];
        assert(mprotect(reinterpret_cast<void *>(page.address), size, page.protection) == 0);
    }
    protectCalls = 0;
    failures = injected;
    const int result = redirects.install();
    assert(result == (injected.empty() ? 1 : pending ? -2 : -3));
    assert(redirects.pending == pending);
    if (pending) {
        bool changed = false;
        for (size_t index = 0; index < entries; index++) {
            const auto &change = redirects.changes[index];
            if (*reinterpret_cast<uintptr_t *>(change.slot) != change.original) changed = true;
        }
        for (size_t index = 0; index < pages; index++) {
            const auto &page = redirects.pages[index];
            if (protectionAt(page.address, size) != page.protection) changed = true;
        }
        assert(changed);
        failures.clear();
        assert(redirects.recover() == -3);
        assert(!redirects.pending);
    }
    for (size_t index = 0; index < entries; index++) {
        const auto &change = redirects.changes[index];
        assert(*reinterpret_cast<uintptr_t *>(change.slot)
                == (injected.empty() ? change.replacement : change.original));
    }
    for (size_t index = 0; index < pages; index++) {
        const auto &page = redirects.pages[index];
        assert(protectionAt(page.address, size) == page.protection);
    }
    assert(munmap(memory, pages * size) == 0);
    std::printf("PASS pages=%zu entries=%zu failures=%zu pending=%d writable=%d\n",
            pages, entries, injected.size(), pending, writable);
}

int main() {
    for (bool writable : {false, true}) {
        for (size_t entries : {size_t(1), size_t(13), size_t(14)}) {
            const size_t pages = entries == 14 ? 2 : 1;
            check(pages, entries, {}, false, writable);
            for (size_t index = 1; index <= pages; index++) {
                check(pages, entries, {static_cast<int>(index)}, false, writable);
                check(pages, entries, {static_cast<int>(pages + index)}, false, writable);
            }
        }
    }
    check(2, 14, {3, 5}, true, false);
    check(2, 14, {4, 6}, true, false);
    check(2, 14, {3, 6}, true, false);
    check(2, 14, {3, 7}, true, false);
    check(2, 14, {3, 8}, true, false);
    check(1, 1, {2, 4}, true, false);
    check(1, 13, {2, 4}, true, false);
    check(2, 14, {1, 2}, false, false);
}
