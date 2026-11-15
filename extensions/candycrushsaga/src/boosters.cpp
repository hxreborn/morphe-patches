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
#include <chrono>
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
#if CANDY_CRUSH_SODA
constexpr char GAME_LIBRARY[] = "libstritz.so";
constexpr uintptr_t GAME_LIBRARY_SIZE = 84765280;
constexpr uintptr_t GAME_INSTANCE_OFFSET = 0x50ff3e0;
constexpr uintptr_t GAME_VTABLE_OFFSET = 0x4e350b0;
constexpr uintptr_t UPDATER_VTABLE_OFFSET = 0x4df6a80;
constexpr uintptr_t DELIVERY_VTABLE_OFFSET = 0x4e63740;
constexpr uintptr_t DELIVERY_METHOD_OFFSET = 0x237c240;
constexpr uintptr_t GAME_UPDATER_OFFSET = 0x290;
constexpr uintptr_t UPDATER_SYSTEM_OFFSET = 0xa8;
#else
constexpr char GAME_LIBRARY[] = "libcandycrushsaga.so";
constexpr uintptr_t GAME_LIBRARY_SIZE = 83963536;

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
#endif
constexpr uintptr_t BUILD_ID_OFFSET = 0x2e0;
constexpr uintptr_t POINTER_ADDRESS_MASK = 0x00ffffffffffffff;
constexpr size_t MAX_ITEMS = 16;
constexpr jint NOT_SUBMITTED = 0;
constexpr jint SUBMITTED = 1;
constexpr jint SUBMISSION_UNKNOWN = 2;
#if CANDY_CRUSH_SODA
constexpr std::array<uint8_t, 16> BUILD_ID = {
    0x45, 0x5f, 0x62, 0x63, 0x0a, 0x53, 0xc9, 0x01,
    0x04, 0x2a, 0x2f, 0x87, 0x86, 0x61, 0xff, 0x78,
};
constexpr std::array<uint32_t, 8> DELIVERY_SIGNATURE = {
    0xa9ba7bfd, 0xa9016ffc, 0xa90267fa, 0xa9035ff8,
    0xa90457f6, 0xa9054ff4, 0x910003fd, 0xd10ac3ff,
};
#else
constexpr std::array<uint8_t, 16> BUILD_ID = {
    0x85, 0x26, 0x5c, 0x8b, 0xa4, 0x5b, 0x0b, 0x97,
    0x9b, 0x90, 0xf7, 0xa0, 0xc4, 0x5c, 0x58, 0x0b,
};
constexpr std::array<uint32_t, 8> DELIVERY_SIGNATURE = {
    0xa9ba7bfd, 0xa9016ffc, 0xa90267fa, 0xa9035ff8,
    0xa90457f6, 0xa9054ff4, 0xd10783ff, 0xd53bd048,
};

#endif

struct Item {
    uint32_t id;
#if CANDY_CRUSH_SODA
    uint32_t padding;
    int64_t amount;
    int32_t scope;
    uint32_t trailingPadding;
#else
    uint32_t amount;
#endif
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

#if CANDY_CRUSH_SODA
static_assert(sizeof(Item) == 24);
static_assert(offsetof(Item, amount) == 8);
static_assert(offsetof(Item, scope) == 16);
#else
static_assert(sizeof(Item) == 8);
#endif
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
#if CANDY_CRUSH_SODA
    {0, 0x4dd64c0, PF_R | PF_X},
    {0x4dda4c0, 0x2f4b40, PF_R | PF_W},
    {0x50d2490, 0x1eff00, PF_R | PF_W},
#else
    {0, 0x4c7d930, PF_R | PF_X},
    {0x4c80000, 0x388000, PF_R | PF_W},
    {0x500b920, 0x1adc98, PF_R | PF_W},
#endif
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
    void *handle = dlopen(GAME_LIBRARY, RTLD_NOW | RTLD_NOLOAD);
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
#if !CANDY_CRUSH_SODA
            || !memory.mapped(base + HANDLE_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + GAME_INSTANCE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + GAME_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + UPDATER_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + SYSTEM_VTABLE_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + DELIVERY_VTABLE_OFFSET + DELIVERY_SLOT_OFFSET, sizeof(uintptr_t), false)
            || !memory.mapped(base + CONTEXT_OFFSET, sizeof(uint64_t), false)
#endif
            ) return 0;
    return base;
}

bool validObject(const ProcessMemory &memory, uintptr_t address, uintptr_t expectedVtable) {
    uintptr_t vtable = 0;
    return address != 0 && memory.readPointer(address, vtable) && vtable == expectedVtable;
}

#if CANDY_CRUSH_SODA
constexpr uintptr_t BOOSTER_MEMBER_OFFSET = 0x1528;
constexpr uintptr_t LIVES_MEMBER_OFFSET = 0x14f0;
constexpr uintptr_t DELIVERY_MEMBER_OFFSET = 0x1548;
constexpr uintptr_t BOOSTER_VTABLE_OFFSET = 0x4f07c60;
constexpr uintptr_t LIVES_VTABLE_OFFSET = 0x4f09b38;
constexpr uintptr_t BOOSTER_GETTER_OFFSET = 0x2d7b2dc;
constexpr uintptr_t BOOSTER_QUANTITY_OFFSET = 0x20;
constexpr uintptr_t LIVES_REPOSITORY_OFFSET = 8;
constexpr uintptr_t IDENTITY_PROVIDER_OFFSET = 0x38;
constexpr uintptr_t IDENTITY_GETTER_SLOT = 0x18;
constexpr uintptr_t LIVES_REMAINING_SLOT = 0x68;
constexpr uintptr_t LIVES_ACTIVE_SLOT = 0x70;
constexpr uint32_t LIVES_ITEM_ID = 12299;
constexpr int SECONDS_PER_MINUTE = 60;
constexpr int REWARD_SOURCE = 24;
constexpr jint CONFIRMED = 3;
constexpr jint CONFIRMATION_PENDING = 0;
constexpr jint CONFIRMATION_FAILED = -1;
constexpr size_t REWARD_TYPES = 2;

struct PendingReward {
    uint64_t userId;
    std::array<uint32_t, MAX_ITEMS> ids;
    std::array<int64_t, MAX_ITEMS> expected;
    size_t count;
    std::chrono::steady_clock::time_point submitted;
};

std::array<PendingReward, REWARD_TYPES> pendingRewards = {};

bool readSodaServices(const ProcessMemory &memory, uintptr_t base, uintptr_t &services) {
    uintptr_t game = 0;
    uintptr_t updater = 0;
    return memory.readPointer(base + GAME_INSTANCE_OFFSET, game)
            && validObject(memory, game, base + GAME_VTABLE_OFFSET)
            && memory.readPointer(game + GAME_UPDATER_OFFSET, updater)
            && validObject(memory, updater, base + UPDATER_VTABLE_OFFSET)
            && memory.readPointer(updater + UPDATER_SYSTEM_OFFSET, services)
            && memory.mapped(services, DELIVERY_MEMBER_OFFSET + sizeof(uintptr_t), false);
}

bool readSodaAmounts(const ProcessMemory &memory, uintptr_t base, uintptr_t services, PendingReward &reward,
        bool lives, bool &active) {
    uintptr_t owner = 0;
    if (!memory.readPointer(services + (lives ? LIVES_MEMBER_OFFSET : BOOSTER_MEMBER_OFFSET), owner)
            || !validObject(memory, owner, base + (lives ? LIVES_VTABLE_OFFSET : BOOSTER_VTABLE_OFFSET))) return false;
    uintptr_t delivery = 0;
    uintptr_t provider = 0;
    uintptr_t providerVtable = 0;
    uintptr_t identityGetter = 0;
    if (!memory.readPointer(services + DELIVERY_MEMBER_OFFSET, delivery)
            || !validObject(memory, delivery, base + DELIVERY_VTABLE_OFFSET)
            || !memory.readPointer(delivery + IDENTITY_PROVIDER_OFFSET, provider)
            || !memory.readPointer(provider, providerVtable)
            || !memory.readPointer(providerVtable + IDENTITY_GETTER_SLOT, identityGetter)
            || !memory.mapped(identityGetter, sizeof(uint32_t), true)) return false;
    const uint64_t userId = reinterpret_cast<uint64_t (*)(void *)>(identityGetter)(reinterpret_cast<void *>(provider));
    if (userId == 0 || (reward.userId != 0 && reward.userId != userId)) return false;
    reward.userId = userId;
    if (lives) {
        uintptr_t repository = 0;
        uintptr_t vtable = 0;
        uintptr_t remaining = 0;
        uintptr_t enabled = 0;
        if (!memory.readPointer(owner + LIVES_REPOSITORY_OFFSET, repository)
                || !memory.readPointer(repository, vtable)
                || !memory.readPointer(vtable + LIVES_REMAINING_SLOT, remaining)
                || !memory.readPointer(vtable + LIVES_ACTIVE_SLOT, enabled)
                || !memory.mapped(remaining, sizeof(uint32_t), true)
                || !memory.mapped(enabled, sizeof(uint32_t), true)) return false;
        active = reinterpret_cast<bool (*)(void *)>(enabled)(reinterpret_cast<void *>(repository));
        reward.expected[0] = active
                ? reinterpret_cast<int (*)(void *)>(remaining)(reinterpret_cast<void *>(repository)) : 0;
        return reward.expected[0] >= 0;
    }
    auto getter = reinterpret_cast<void *(*)(void *, uint32_t)>(base + BOOSTER_GETTER_OFFSET);
    for (size_t index = 0; index < reward.count; index++) {
        const uintptr_t item = reinterpret_cast<uintptr_t>(getter(reinterpret_cast<void *>(owner), reward.ids[index]));
        if (item == 0 || !memory.readMemory(item + BOOSTER_QUANTITY_OFFSET,
                &reward.expected[index], sizeof(int64_t)) || reward.expected[index] < 0) return false;
    }
    return true;
}

jint submit(uintptr_t handle, const Item *items, size_t count) {
    const ProcessMemory memory;
    const uintptr_t base = gameBase(memory);
    if (base == 0 || handle == 0) return NOT_SUBMITTED;
    uintptr_t services = 0;
    uintptr_t delivery = 0;
    if (!readSodaServices(memory, base, services)
            || !memory.readPointer(services + DELIVERY_MEMBER_OFFSET, delivery)
            || !validObject(memory, delivery, base + DELIVERY_VTABLE_OFFSET)) return NOT_SUBMITTED;
    const bool lives = count == 1 && items[0].id == LIVES_ITEM_ID;
    if (lives && items[0].amount > INT32_MAX / SECONDS_PER_MINUTE) return NOT_SUBMITTED;
    auto &pending = pendingRewards[lives ? 1 : 0];
    if (pending.count != 0) return SUBMISSION_UNKNOWN;
    PendingReward reward = {};
    reward.count = count;
    for (size_t index = 0; index < count; index++) reward.ids[index] = items[index].id;
    bool active = false;
    if (!readSodaAmounts(memory, base, services, reward, lives, active)) return NOT_SUBMITTED;
    for (size_t index = 0; index < count; index++) {
        const int64_t amount = items[index].amount * (lives ? SECONDS_PER_MINUTE : 1);
        if (amount <= 0 || reward.expected[index] > INT64_MAX - amount) return NOT_SUBMITTED;
        reward.expected[index] += amount;
    }
    reward.submitted = std::chrono::steady_clock::now();
    pending = reward;
    auto *begin = const_cast<Item *>(items);
    Reward vector = {begin, begin + count, begin + count, {0, 0, 0}};
    auto deliver = reinterpret_cast<void (*)(void *, void *, const char *, int, int, const char *, bool)>(
            base + DELIVERY_METHOD_OFFSET);
    try {
        deliver(reinterpret_cast<void *>(delivery), &vector, "", 0, REWARD_SOURCE, "", false);
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Reward submission result unknown");
        return SUBMISSION_UNKNOWN;
    }
    return SUBMITTED;
}

jint confirm(uint32_t firstId) {
    const bool lives = firstId == LIVES_ITEM_ID;
    const auto &pending = pendingRewards[lives ? 1 : 0];
    if (pending.count == 0 || pending.ids[0] != firstId) return CONFIRMATION_FAILED;
    const ProcessMemory memory;
    const uintptr_t base = gameBase(memory);
    uintptr_t services = 0;
    if (base == 0 || !readSodaServices(memory, base, services)) return CONFIRMATION_FAILED;
    auto observed = pending;
    bool active = false;
    if (!readSodaAmounts(memory, base, services, observed, lives, active)) return CONFIRMATION_FAILED;
    if (lives && !active) return CONFIRMATION_PENDING;
    const int64_t elapsed = std::chrono::duration_cast<std::chrono::seconds>(
            std::chrono::steady_clock::now() - pending.submitted).count();
    for (size_t index = 0; index < pending.count; index++) {
        const int64_t minimum = pending.expected[index] - (lives ? elapsed + 1 : 0);
        if (observed.expected[index] > pending.expected[index]) return CONFIRMATION_FAILED;
        if (observed.expected[index] < minimum) return CONFIRMATION_PENDING;
    }
    return CONFIRMED;
}
#else
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
#endif

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
#if CANDY_CRUSH_SODA
            items[index] = {static_cast<uint32_t>(ids[index]), 0, amount, -1, 0};
#else
            items[index] = {static_cast<uint32_t>(ids[index]), static_cast<uint32_t>(amount)};
#endif
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

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_candycrushsaga_Boosters_confirm(JNIEnv *, jclass, jint firstId) {
#if CANDY_CRUSH_SODA
    try {
        return confirm(static_cast<uint32_t>(firstId));
    } catch (...) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Reward confirmation failed");
        return CONFIRMATION_FAILED;
    }
#else
    return -1;
#endif
}
