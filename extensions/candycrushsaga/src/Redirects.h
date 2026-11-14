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
#pragma once

#include <sys/mman.h>

#include <array>
#include <atomic>
#include <cstdint>
#include <cstdio>

namespace {

struct Change {
    uintptr_t slot;
    uintptr_t expected;
    uintptr_t replacement;
    uintptr_t original = 0;
};

struct Page {
    uintptr_t address;
    int protection;
};

int protectionAt(uintptr_t address, size_t size) {
    FILE *maps = std::fopen("/proc/self/maps", "r");
    if (maps == nullptr) return -1;
    char line[512];
    int result = -1;
    while (std::fgets(line, sizeof(line), maps) != nullptr) {
        unsigned long begin = 0;
        unsigned long end = 0;
        char mode[5] = {};
        if (std::sscanf(line, "%lx-%lx %4s", &begin, &end, mode) == 3
                && address >= begin && address < end && size <= end - address) {
            result = (mode[0] == 'r' ? PROT_READ : 0) | (mode[1] == 'w' ? PROT_WRITE : 0)
                    | (mode[2] == 'x' ? PROT_EXEC : 0);
            break;
        }
    }
    std::fclose(maps);
    return result;
}

struct Redirects {
    std::array<Change, 14> changes = {};
    std::array<Page, 2> pages = {};
    size_t changeCount = 0;
    size_t pageCount = 0;
    size_t pageSize = 0;
    bool pending = false;

    int recover() {
        bool recovered = true;
        for (size_t index = 0; index < pageCount; index++) {
            const auto &page = pages[index];
            bool changed = false;
            for (size_t entry = 0; entry < changeCount; entry++) {
                const auto &change = changes[entry];
                if (change.slot >= page.address && change.slot < page.address + pageSize
                        && reinterpret_cast<std::atomic<uintptr_t> *>(change.slot)->load() != change.original) {
                    changed = true;
                }
            }
            if (changed) {
                if (mprotect(reinterpret_cast<void *>(page.address), pageSize, page.protection | PROT_WRITE) != 0) {
                    recovered = false;
                } else {
                    for (size_t entry = 0; entry < changeCount; entry++) {
                        const auto &change = changes[entry];
                        if (change.slot >= page.address && change.slot < page.address + pageSize) {
                            reinterpret_cast<std::atomic<uintptr_t> *>(change.slot)->store(change.original);
                        }
                    }
                }
            }
            mprotect(reinterpret_cast<void *>(page.address), pageSize, page.protection);
        }
        for (size_t index = 0; index < changeCount; index++) {
            const auto &change = changes[index];
            if (reinterpret_cast<std::atomic<uintptr_t> *>(change.slot)->load() != change.original) recovered = false;
        }
        for (size_t index = 0; index < pageCount; index++) {
            if (protectionAt(pages[index].address, pageSize) != pages[index].protection) recovered = false;
        }
        pending = !recovered;
        return recovered ? -3 : -2;
    }

    int install() {
        pending = true;
        for (size_t index = 0; index < pageCount; index++) {
            const auto &page = pages[index];
            if (mprotect(reinterpret_cast<void *>(page.address), pageSize, page.protection | PROT_WRITE) != 0) {
                return recover();
            }
        }
        for (size_t index = 0; index < changeCount; index++) {
            const auto &change = changes[index];
            reinterpret_cast<std::atomic<uintptr_t> *>(change.slot)->store(change.replacement);
        }
        bool restored = true;
        for (size_t index = 0; index < pageCount; index++) {
            const auto &page = pages[index];
            if (mprotect(reinterpret_cast<void *>(page.address), pageSize, page.protection) != 0) restored = false;
        }
        if (!restored) return recover();
        pending = false;
        return 1;
    }
};

}
