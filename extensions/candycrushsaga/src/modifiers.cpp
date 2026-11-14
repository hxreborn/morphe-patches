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
#include "Redirects.h"

#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <link.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#include <array>
#include <atomic>
#include <cstdint>
#include <cstring>
#include <memory>
#include <limits>

namespace {

constexpr char LOG_TAG[] = "hxCandyCrush";
constexpr size_t GAME_LIBRARY_SIZE = 83963536;
constexpr uintptr_t BUILD_ID_OFFSET = 0x2e0;
constexpr uintptr_t UNLOCK_SLOT = 0x18;
constexpr jint UNLOCK_SLOTS = 1;
constexpr jint ADD_MOVES = 2;
constexpr int EXTRA_MOVES = 5;
constexpr uintptr_t MOVE_LIMIT_SLOT = 0x4e26a00;
constexpr uintptr_t MOVE_LIMIT_GETTER = 0x31bb6e0;
constexpr uintptr_t MOVE_LIMIT_SETTER = 0x31bb758;
constexpr uintptr_t TRACKER_MOVE_LIMIT_RETURN = 0x324a5ec;
std::atomic<uintptr_t> activeBase{0};
constexpr std::array<uint8_t, 16> BUILD_ID = {
    0x85, 0x26, 0x5c, 0x8b, 0xa4, 0x5b, 0x0b, 0x97,
    0x9b, 0x90, 0xf7, 0xa0, 0xc4, 0x5c, 0x58, 0x0b,
};

struct UnlockGetter {
    uintptr_t vtable;
    uintptr_t method;
};

constexpr std::array<UnlockGetter, 13> UNLOCK_GETTERS = {{
    {0x4ccf5e8, 0x21d1c94},
    {0x4ccf668, 0x21d1fd4},
    {0x4ccf6e8, 0x21d231c},
    {0x4ccf768, 0x21d265c},
    {0x4ccf7e8, 0x21d299c},
    {0x4ccf868, 0x21d2ce4},
    {0x4ccf8e8, 0x21d3024},
    {0x4ccf968, 0x21d336c},
    {0x4ccf9e8, 0x21d36b4},
    {0x4ccfa68, 0x21d39f4},
    {0x4ccfae8, 0x21d3cb0},
    {0x4ccfb80, 0x21d3cb0},
    {0x4ccfc00, 0x21d3ff0},
}};

bool unlocked(const void *, const void *) {
    return true;
}

bool segmentsMatch(uintptr_t base) {
    struct Check {
        uintptr_t base;
        bool matches;
    } check = {base, false};
    dl_iterate_phdr([](dl_phdr_info *info, size_t, void *data) {
        auto &check = *static_cast<Check *>(data);
        if (info->dlpi_addr != check.base) return 0;
        constexpr std::array<uintptr_t, 3> addresses = {0, 0x4c80000, 0x500b920};
        constexpr std::array<uintptr_t, 3> sizes = {0x4c7d930, 0x388000, 0x1adc98};
        constexpr std::array<uint32_t, 3> flags = {PF_R | PF_X, PF_R | PF_W, PF_R | PF_W};
        size_t count = 0;
        for (size_t index = 0; index < info->dlpi_phnum; index++) {
            const auto &header = info->dlpi_phdr[index];
            if (header.p_type != PT_LOAD) continue;
            if (count >= addresses.size() || header.p_vaddr != addresses[count]
                    || header.p_memsz != sizes[count] || header.p_flags != flags[count]) return 1;
            count++;
        }
        check.matches = count == addresses.size();
        return 1;
    }, &check);
    return check.matches;
}

int moveLimit(void *state) {
    const uintptr_t caller = reinterpret_cast<uintptr_t>(__builtin_return_address(0));
    const uintptr_t base = activeBase.load();
    const int limit = reinterpret_cast<int (*)(void *)>(base + MOVE_LIMIT_GETTER)(state);
    if (caller == base + TRACKER_MOVE_LIMIT_RETURN && limit > 0
            && limit <= std::numeric_limits<int>::max() - EXTRA_MOVES) {
        reinterpret_cast<void (*)(void *, int)>(base + MOVE_LIMIT_SETTER)(state, limit + EXTRA_MOVES);
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Extra moves added, amount=%d", EXTRA_MOVES);
    }
    return limit;
}

Redirects redirects;

jint activate(jint features) {
    if (redirects.pending) return redirects.recover();
    if (features == 0 || (features & ~(UNLOCK_SLOTS | ADD_MOVES)) != 0) return -1;
    void *handle = dlopen("libcandycrushsaga.so", RTLD_NOW | RTLD_NOLOAD);
    if (handle == nullptr) return 0;
    std::unique_ptr<void, decltype(&dlclose)> library(handle, &dlclose);
    void *symbol = dlsym(handle, "Java_com_king_http_HttpManager_on_1data");
    Dl_info info = {};
    struct stat file = {};
    if (symbol == nullptr || dladdr(symbol, &info) == 0 || info.dli_fbase == nullptr
            || info.dli_fname == nullptr || stat(info.dli_fname, &file) != 0
            || file.st_size != GAME_LIBRARY_SIZE) return -1;
    const uintptr_t base = reinterpret_cast<uintptr_t>(info.dli_fbase);
    if (!segmentsMatch(base)
            || std::memcmp(reinterpret_cast<void *>(base + BUILD_ID_OFFSET), BUILD_ID.data(), BUILD_ID.size()) != 0) {
        return -1;
    }
    redirects = {};
    auto &changes = redirects.changes;
    auto &changeCount = redirects.changeCount;
    if ((features & UNLOCK_SLOTS) != 0) {
        for (const auto &getter : UNLOCK_GETTERS) {
            changes[changeCount++] = {base + getter.vtable + UNLOCK_SLOT,
                    base + getter.method, reinterpret_cast<uintptr_t>(&unlocked)};
        }
    }
    if ((features & ADD_MOVES) != 0) {
        if (*reinterpret_cast<uintptr_t *>(base + MOVE_LIMIT_SLOT + sizeof(uintptr_t))
                != base + MOVE_LIMIT_SETTER) return -1;
        changes[changeCount++] = {base + MOVE_LIMIT_SLOT, base + MOVE_LIMIT_GETTER,
                reinterpret_cast<uintptr_t>(&moveLimit)};
    }
    const long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) return -1;
    const auto pageMask = static_cast<uintptr_t>(pageSize) - 1;
    redirects.pageSize = static_cast<size_t>(pageSize);
    auto &pages = redirects.pages;
    auto &pageCount = redirects.pageCount;
    for (size_t index = 0; index < changeCount; index++) {
        auto &change = changes[index];
        const uintptr_t current = reinterpret_cast<std::atomic<uintptr_t> *>(change.slot)->load();
        if (current != change.expected && current != change.replacement) return -1;
        change.original = current;
        const uintptr_t page = change.slot & ~pageMask;
        bool found = false;
        for (size_t existing = 0; existing < pageCount; existing++) {
            if (pages[existing].address == page) found = true;
        }
        if (!found) {
            if (pageCount == pages.size()) return -1;
            const int protection = protectionAt(page, pageSize);
            if (protection < 0 || (protection & PROT_READ) == 0) return -1;
            pages[pageCount++] = {page, protection};
        }
    }
    activeBase.store(base);
    const int result = redirects.install();
    if (result != 1) return result;
    if ((features & UNLOCK_SLOTS) != 0) {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Booster slots unlocked, count=%zu", UNLOCK_GETTERS.size());
    }
    if ((features & ADD_MOVES) != 0) {
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Extra moves enabled, amount=%d", EXTRA_MOVES);
    }
    return 1;
}

}

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_candycrushsaga_GameModifiers_activate(JNIEnv *, jclass, jint features) {
    return activate(features);
}
