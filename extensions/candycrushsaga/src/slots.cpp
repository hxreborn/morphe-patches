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

namespace {

constexpr char LOG_TAG[] = "hxCandyCrush";
constexpr size_t GAME_LIBRARY_SIZE = 83963536;
constexpr uintptr_t BUILD_ID_OFFSET = 0x2e0;
constexpr uintptr_t UNLOCK_SLOT = 0x18;
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

jint activate() {
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
    const auto replacement = reinterpret_cast<uintptr_t>(&unlocked);
    const long pageSize = sysconf(_SC_PAGESIZE);
    if (pageSize <= 0) return -1;
    const auto pageMask = static_cast<uintptr_t>(pageSize) - 1;
    const uintptr_t page = (base + UNLOCK_GETTERS.front().vtable) & ~pageMask;
    for (const auto &getter : UNLOCK_GETTERS) {
        const uintptr_t address = base + getter.vtable + UNLOCK_SLOT;
        if ((address & ~pageMask) != page) return -1;
        const uintptr_t current = reinterpret_cast<std::atomic<uintptr_t> *>(address)->load();
        if (current != base + getter.method && current != replacement) return -1;
    }
    if (mprotect(reinterpret_cast<void *>(page), pageSize, PROT_READ | PROT_WRITE) != 0) return -1;
    for (const auto &getter : UNLOCK_GETTERS) {
        auto *slot = reinterpret_cast<std::atomic<uintptr_t> *>(base + getter.vtable + UNLOCK_SLOT);
        slot->store(replacement);
    }
    if (mprotect(reinterpret_cast<void *>(page), pageSize, PROT_READ) != 0) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "Booster slot page protection failed");
        return -1;
    }
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Booster slots unlocked, count=%zu", UNLOCK_GETTERS.size());
    return 1;
}

}

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_candycrushsaga_BoosterSlots_activate(JNIEnv *, jclass) {
    return activate();
}
