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
#include <sys/stat.h>
#include <sys/uio.h>
#include <unistd.h>

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <memory>
#include <vector>

namespace {

constexpr char LOG_TAG[] = "hxCandyCrush";
constexpr size_t MAPS_LINE_SIZE = 512;
constexpr size_t PERMISSION_TEXT_SIZE = 5;
constexpr uintptr_t GAME_LIBRARY_SIZE = 83963536;
constexpr uintptr_t BUILD_ID_OFFSET = 0x2e0;
constexpr uintptr_t HANDLE_VTABLE_OFFSET = 0x4c837b0;
constexpr uintptr_t GAME_INSTANCE_OFFSET = 0x5016d98;
constexpr uintptr_t GAME_VTABLE_OFFSET = 0x4c84330;
constexpr uintptr_t UPDATER_VTABLE_OFFSET = 0x4c84e18;
constexpr uintptr_t SYSTEM_VTABLE_OFFSET = 0x4e9da48;
constexpr uintptr_t DELIVERY_VTABLE_OFFSET = 0x4e9dc98;
constexpr uintptr_t DELIVERY_METHOD_OFFSET = 0x38652d0;
constexpr uintptr_t DELIVERY_MEMBER_OFFSET = 0x48;
constexpr uintptr_t DELIVERY_SLOT_OFFSET = 0x68;
constexpr uintptr_t GAME_UPDATER_OFFSET = 0x70;
constexpr uintptr_t UPDATER_SYSTEM_OFFSET = 0x1d20;
constexpr uintptr_t CONTEXT_OFFSET = 0xbd81e0;
constexpr uintptr_t POINTER_ADDRESS_MASK = 0x00ffffffffffffff;
constexpr size_t MAX_ITEMS = 16;
constexpr jint NOT_SUBMITTED = 0;
constexpr jint SUBMITTED = 1;
constexpr jint SUBMISSION_UNKNOWN = 2;
constexpr std::array<uint8_t, 16> BUILD_ID = {
    0x85, 0x26, 0x5c, 0x8b, 0xa4, 0x5b, 0x0b, 0x97,
    0x9b, 0x90, 0xf7, 0xa0, 0xc4, 0x5c, 0x58, 0x0b,
};
constexpr std::array<uint32_t, 8> DELIVERY_SIGNATURE = {
    0xa9ba7bfd, 0xa9016ffc, 0xa90267fa, 0xa9035ff8,
    0xa90457f6, 0xa9054ff4, 0xd10783ff, 0xd53bd048,
};

struct Item {
    uint32_t id;
    uint32_t amount;
};

struct Reward {
    Item *begin;
    Item *end;
    Item *capacity;
    uintptr_t remaining[3];
};

struct Context {
    uint64_t value;
    uint8_t hasValue;
    uint8_t firstPadding[7];
    uint64_t gameRoundId;
    uint8_t hasGameRoundId;
    uint8_t secondPadding[7];
};

static_assert(sizeof(Item) == 8);
static_assert(sizeof(Reward) == 0x30);
static_assert(sizeof(Context) == 0x20);
static_assert(offsetof(Context, gameRoundId) == 0x10);
static_assert(offsetof(Context, hasGameRoundId) == 0x18);

struct LoadSegment {
    uintptr_t address;
    uintptr_t size;
    uint32_t flags;
};

constexpr std::array<LoadSegment, 3> LOAD_SEGMENTS = {{
    {0, 0x4c7d930, PF_R | PF_X},
    {0x4c80000, 0x388000, PF_R | PF_W},
    {0x500b920, 0x1adc98, PF_R | PF_W},
}};

class ProcessMemory {
    struct Mapping {
        uintptr_t start;
        uintptr_t end;
        bool executable;
    };

    std::vector<Mapping> mappings;

public:
    ProcessMemory() {
        std::unique_ptr<FILE, decltype(&std::fclose)> maps(std::fopen("/proc/self/maps", "r"), &std::fclose);
        if (!maps) return;
        char line[MAPS_LINE_SIZE];
        while (std::fgets(line, sizeof(line), maps.get()) != nullptr) {
            unsigned long long start = 0;
            unsigned long long end = 0;
            char permissions[PERMISSION_TEXT_SIZE] = {};
            if (std::sscanf(line, "%llx-%llx %4s", &start, &end, permissions) == 3
                    && permissions[0] == 'r') {
                mappings.push_back({start, end, permissions[2] == 'x'});
            }
        }
        if (std::ferror(maps.get())) mappings.clear();
    }

    bool mapped(uintptr_t address, size_t size, bool executable) const {
        const uintptr_t untagged = address & POINTER_ADDRESS_MASK;
        if (size == 0 || untagged > UINTPTR_MAX - size) return false;
        for (const auto &mapping : mappings) {
            if (untagged >= mapping.start && untagged + size <= mapping.end
                    && (!executable || mapping.executable)) return true;
        }
        return false;
    }

    bool readMemory(uintptr_t address, void *output, size_t size) const {
        if (!mapped(address, size, false)) return false;
        iovec local = {output, size};
        iovec remote = {reinterpret_cast<void *>(address & POINTER_ADDRESS_MASK), size};
        return process_vm_readv(getpid(), &local, 1, &remote, 1, 0) == static_cast<ssize_t>(size);
    }

    bool readPointer(uintptr_t address, uintptr_t &value) const {
        return readMemory(address, &value, sizeof(value));
    }
};

bool mappedSegmentsMatch(uintptr_t base) {
    struct Segments {
        uintptr_t base;
        bool matched;
    } segments = {base, false};
    dl_iterate_phdr([](dl_phdr_info *info, size_t, void *data) {
        auto &result = *static_cast<Segments *>(data);
        if (info->dlpi_addr != result.base) return 0;
        size_t count = 0;
        for (size_t index = 0; index < info->dlpi_phnum; index++) {
            const auto &header = info->dlpi_phdr[index];
            if (header.p_type != PT_LOAD) continue;
            if (count >= LOAD_SEGMENTS.size()) return 1;
            const auto &expected = LOAD_SEGMENTS[count++];
            if (header.p_vaddr != expected.address || header.p_memsz != expected.size
                    || header.p_flags != expected.flags) return 1;
        }
        result.matched = count == LOAD_SEGMENTS.size();
        return 1;
    }, &segments);
    return segments.matched;
}

uintptr_t gameBase(const ProcessMemory &memory) {
    void *handle = dlopen("libcandycrushsaga.so", RTLD_NOW | RTLD_NOLOAD);
    if (handle == nullptr) return 0;
    std::unique_ptr<void, decltype(&dlclose)> library(handle, &dlclose);
    void *symbol = dlsym(handle, "Java_com_king_http_HttpManager_on_1data");
    Dl_info info = {};
    struct stat file = {};
    if (symbol == nullptr || dladdr(symbol, &info) == 0 || info.dli_fbase == nullptr
            || info.dli_fname == nullptr || stat(info.dli_fname, &file) != 0
            || file.st_size != GAME_LIBRARY_SIZE) return 0;
    const uintptr_t base = reinterpret_cast<uintptr_t>(info.dli_fbase);
    std::array<uint8_t, BUILD_ID.size()> id = {};
    std::array<uint32_t, DELIVERY_SIGNATURE.size()> signature = {};
    if (!mappedSegmentsMatch(base)
            || !memory.readMemory(base + BUILD_ID_OFFSET, id.data(), id.size()) || id != BUILD_ID
            || !memory.mapped(base + DELIVERY_METHOD_OFFSET, sizeof(signature), true)
            || !memory.readMemory(base + DELIVERY_METHOD_OFFSET, signature.data(), sizeof(signature))
            || signature != DELIVERY_SIGNATURE
            || !memory.mapped(base + HANDLE_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + GAME_INSTANCE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + GAME_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + UPDATER_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + SYSTEM_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + DELIVERY_VTABLE_OFFSET + DELIVERY_SLOT_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + CONTEXT_OFFSET, sizeof(uint64_t), false)) return 0;
    return base;
}

bool validObject(const ProcessMemory &memory, uintptr_t address, uintptr_t expectedVtable) {
    uintptr_t vtable = 0;
    return address != 0 && memory.readPointer(address, vtable) && vtable == expectedVtable;
}

jint submit(uintptr_t handle, const Item *items, size_t count) {
    const ProcessMemory memory;
    const uintptr_t base = gameBase(memory);
    if (base == 0) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game library validation failed");
        return NOT_SUBMITTED;
    }
    if (handle == 0) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game handle is not ready");
        return NOT_SUBMITTED;
    }
    uintptr_t game = 0;
    uintptr_t updater = 0;
    uintptr_t system = 0;
    uintptr_t method = 0;
    if (!validObject(memory, handle, base + HANDLE_VTABLE_OFFSET)) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game handle target mismatch");
        return NOT_SUBMITTED;
    }
    if (!memory.readPointer(base + GAME_INSTANCE_OFFSET, game)
            || !validObject(memory, game, base + GAME_VTABLE_OFFSET)) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game singleton is not ready");
        return NOT_SUBMITTED;
    }
    if (!memory.readPointer(game + GAME_UPDATER_OFFSET, updater)
            || !validObject(memory, updater, base + UPDATER_VTABLE_OFFSET)) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game updater is not ready");
        return NOT_SUBMITTED;
    }
    if (!memory.readPointer(updater + UPDATER_SYSTEM_OFFSET, system)
            || !validObject(memory, system, base + SYSTEM_VTABLE_OFFSET)
            || !validObject(memory, system + DELIVERY_MEMBER_OFFSET, base + DELIVERY_VTABLE_OFFSET)
            || !memory.readPointer(base + DELIVERY_VTABLE_OFFSET + DELIVERY_SLOT_OFFSET, method)
            || method != base + DELIVERY_METHOD_OFFSET) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "game reward system is not ready");
        return NOT_SUBMITTED;
    }
    uint64_t contextValue = 0;
    if (!memory.readMemory(base + CONTEXT_OFFSET, &contextValue, sizeof(contextValue))) return NOT_SUBMITTED;
    auto *begin = const_cast<Item *>(items);
    Reward reward = {begin, begin + count, begin + count, {0, 0, 0}};
    Context context = {contextValue, 1, {}, 0, 1, {}};
    auto deliver = reinterpret_cast<void (*)(void *, void *, int, void *)>(method);
    try {
        deliver(reinterpret_cast<void *>(system + DELIVERY_MEMBER_OFFSET), &reward, 1, &context);
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "reward submission result unknown");
        return SUBMISSION_UNKNOWN;
    }
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "reward submitted items=%zu", count);
    return SUBMITTED;
}

}

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_candycrushsaga_Boosters_grant(JNIEnv *env, jclass, jlong nativeHandle,
        jintArray itemIds, jint amount) {
    try {
        if (itemIds == nullptr || amount <= 0) return NOT_SUBMITTED;
        const jsize count = env->GetArrayLength(itemIds);
        if (count <= 0 || static_cast<size_t>(count) > MAX_ITEMS) return NOT_SUBMITTED;
        std::array<jint, MAX_ITEMS> ids = {};
        env->GetIntArrayRegion(itemIds, 0, count, ids.data());
        if (env->ExceptionCheck()) return NOT_SUBMITTED;
        std::array<Item, MAX_ITEMS> items = {};
        for (jsize index = 0; index < count; index++) {
            if (ids[index] <= 0) return NOT_SUBMITTED;
            items[index] = {static_cast<uint32_t>(ids[index]), static_cast<uint32_t>(amount)};
        }
        return submit(static_cast<uintptr_t>(nativeHandle), items.data(), static_cast<size_t>(count));
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "reward submission failed");
        return NOT_SUBMITTED;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_hxreborn_extension_candycrushsaga_Boosters_libraryPath(JNIEnv *env, jclass) {
    try {
        const ProcessMemory memory;
        const uintptr_t base = gameBase(memory);
        if (base == 0) return nullptr;
        Dl_info info = {};
        if (dladdr(reinterpret_cast<void *>(base + DELIVERY_METHOD_OFFSET), &info) == 0
                || info.dli_fname == nullptr) return nullptr;
        return env->NewStringUTF(info.dli_fname);
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "game library lookup failed");
        return nullptr;
    }
}
