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
#define _GNU_SOURCE

#include <android/log.h>
#include <elf.h>
#include <fcntl.h>
#include <jni.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#define LOG_TAG "hxMagicPoser"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOG_ERROR(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define GAME_LIBRARY "libnative-lib.so"
#define MAPS_PATH "/proc/self/maps"
#define MEMORY_PATH "/proc/self/mem"
#define MAPS_LINE_BYTES 1024

#define SEARCH_TIMEOUT_US 120000000L
#define SEARCH_INTERVAL_US 2000
#define LEVEL_INTERVAL_US 20000

#define USER_LEVEL_MASTER 2
#define INSTRUCTION_BYTES 4
#define PAGE_BYTES 4096
#define LEVEL_SCAN_WINDOW 4
#define LEVEL_ARGUMENT_WINDOW 8
#define LEVEL_CANDIDATES 8

#define ARM64_RET 0xd65f03c0u
#define ARM64_MOV_W0_2 0x52800040u
#define ARM64_BL_MASK 0xfc000000u
#define ARM64_BL 0x94000000u
#define ARM64_LDR_W_IMM_MASK 0xffc0001fu
#define ARM64_LDR_W0_IMM 0xb9400000u
#define ARM64_STR_W_IMM_MASK 0xffc0001fu
#define ARM64_STR_W0_IMM 0xb9000000u
#define ARM64_IMM12_SHIFT 10
#define ARM64_IMM12_MASK 0xfffu
#define ARM64_WORD_SCALE 4

#define DYNAMIC_HASH 4
#define DYNAMIC_STRTAB 5
#define DYNAMIC_SYMTAB 6

#if defined(__aarch64__)

typedef enum {
    RETURN_VOID,
    RETURN_MASTER_LEVEL,
} StubKind;

typedef struct {
    const char *symbol;
    StubKind kind;
} StubTarget;

#if defined(MPBRIDGE_PREMIUM)
static const StubTarget STUB_TARGETS[] = {
    {"_Z23MPNUserLevelToUserLevel12MPNUserLevel", RETURN_MASTER_LEVEL},
    {"_Z20GetStoreSkuUserLevel10StringView", RETURN_MASTER_LEVEL},
};
#elif defined(MPBRIDGE_TRACKING)
static const StubTarget STUB_TARGETS[] = {
    {"_ZN9Analytics12BeginSessionEm8StringId", RETURN_VOID},
    {"_ZN9Analytics10EndSessionEb", RETURN_VOID},
    {"_ZN9Analytics8AddEventE18AnalyticsEventType", RETURN_VOID},
    {"_ZN9Analytics8AddEventE18AnalyticsEventType10StringViewS1_", RETURN_VOID},
    {"_ZN9Analytics8AddEventE18AnalyticsEventType10StringViewj", RETURN_VOID},
    {"_ZN9Analytics5FlushEv", RETURN_VOID},
    {"_ZN9Analytics16UpdateNetworkingEv", RETURN_VOID},
    {"_Z25SendMobileAnalyticsEventsPKc", RETURN_VOID},
    {"_Z25SendMobileAnalyticsEventsPKcS0_S0_", RETURN_VOID},
    {"_Z25SendMobileAnalyticsEventsPKcS0_j", RETURN_VOID},
    {"_Z28SendMobileAnalyticsEventDataPKhj", RETURN_VOID},
    {"_ZN12MPNetworking15SendCrashReportE9ArrayViewIhE", RETURN_VOID},
};
#else
#error "define MPBRIDGE_PREMIUM or MPBRIDGE_TRACKING"
#endif

#define STUB_TARGET_COUNT (sizeof(STUB_TARGETS) / sizeof(STUB_TARGETS[0]))

typedef struct {
    const Elf64_Sym *symbols;
    const char *strings;
    uint32_t count;
    uintptr_t base;
} SymbolTable;

static uintptr_t findGameBase(void) {
    FILE *maps = fopen(MAPS_PATH, "re");
    if (maps == NULL) return 0;
    char line[MAPS_LINE_BYTES];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), maps) != NULL) {
        if (strstr(line, GAME_LIBRARY) == NULL) continue;
        unsigned long start = 0;
        unsigned long offset = 0;
        if (sscanf(line, "%lx-%*lx %*4s %lx", &start, &offset) != 2 || offset != 0) continue;
        base = start;
        break;
    }
    fclose(maps);
    return base;
}

static uintptr_t resolveDynamicPointer(uintptr_t base, uintptr_t value) {
    return value < base ? base + value : value;
}

static int loadSymbolTable(uintptr_t base, SymbolTable *table) {
    const Elf64_Ehdr *header = (const Elf64_Ehdr *) base;
    if (memcmp(header->e_ident, ELFMAG, SELFMAG) != 0 || header->e_machine != EM_AARCH64) return 0;
    const Elf64_Phdr *phdrs = (const Elf64_Phdr *) (base + header->e_phoff);
    const Elf64_Dyn *dynamic = NULL;
    for (int index = 0; index < header->e_phnum; index++) {
        if (phdrs[index].p_type == PT_DYNAMIC) dynamic = (const Elf64_Dyn *) (base + phdrs[index].p_vaddr);
    }
    if (dynamic == NULL) return 0;
    uintptr_t symtab = 0;
    uintptr_t strtab = 0;
    uintptr_t hash = 0;
    for (const Elf64_Dyn *entry = dynamic; entry->d_tag != DT_NULL; entry++) {
        if (entry->d_tag == DYNAMIC_SYMTAB) symtab = entry->d_un.d_ptr;
        if (entry->d_tag == DYNAMIC_STRTAB) strtab = entry->d_un.d_ptr;
        if (entry->d_tag == DYNAMIC_HASH) hash = entry->d_un.d_ptr;
    }
    if (symtab == 0 || strtab == 0 || hash == 0) return 0;
    const uint32_t *hashTable = (const uint32_t *) resolveDynamicPointer(base, hash);
    table->symbols = (const Elf64_Sym *) resolveDynamicPointer(base, symtab);
    table->strings = (const char *) resolveDynamicPointer(base, strtab);
    table->count = hashTable[1];
    table->base = base;
    return 1;
}

static const Elf64_Sym *findSymbol(const SymbolTable *table, const char *name) {
    for (uint32_t index = 1; index < table->count; index++) {
        const Elf64_Sym *symbol = &table->symbols[index];
        if (symbol->st_shndx == SHN_UNDEF || symbol->st_name == 0) continue;
        if (strcmp(table->strings + symbol->st_name, name) == 0) return symbol;
    }
    return NULL;
}

static int writeCode(uintptr_t address, const uint32_t *words, size_t count) {
    const size_t length = count * INSTRUCTION_BYTES;
    int memory = open(MEMORY_PATH, O_RDWR | O_CLOEXEC);
    int written = 0;
    if (memory >= 0) {
        written = pwrite(memory, words, length, (off_t) address) == (ssize_t) length;
        close(memory);
    }
    if (!written) {
        const uintptr_t page = address & ~(uintptr_t) (PAGE_BYTES - 1);
        const size_t span = address + length - page;
        if (mprotect((void *) page, span, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return 0;
        memcpy((void *) address, words, length);
        mprotect((void *) page, span, PROT_READ | PROT_EXEC);
    }
    __builtin___clear_cache((char *) address, (char *) address + length);
    return 1;
}

static int applyStub(const SymbolTable *table, const StubTarget *target) {
    const Elf64_Sym *symbol = findSymbol(table, target->symbol);
    if (symbol == NULL) {
        LOG_ERROR("missing %s", target->symbol);
        return 0;
    }
    uint32_t words[2];
    size_t count = 0;
    if (target->kind == RETURN_MASTER_LEVEL) words[count++] = ARM64_MOV_W0_2;
    words[count++] = ARM64_RET;
    if (symbol->st_size < count * INSTRUCTION_BYTES) {
        LOG_ERROR("%s is too small", target->symbol);
        return 0;
    }
    if (!writeCode(table->base + symbol->st_value, words, count)) {
        LOG_ERROR("write failed %s", target->symbol);
        return 0;
    }
    return 1;
}

#if defined(MPBRIDGE_PREMIUM)
static const char APP_STATE_SYMBOL[] = "g_AppState";
static const char *const LEVEL_WRITERS[] = {
    "_ZN12MPNetworking20StartProTrialRequestEv",
    "_ZN12MPNetworking20UpdateStolenRequestsEv",
};

#define LEVEL_WRITER_COUNT (sizeof(LEVEL_WRITERS) / sizeof(LEVEL_WRITERS[0]))

static int loadsLevelArgument(const uint32_t *code, size_t callIndex) {
    for (size_t back = 1; back <= LEVEL_ARGUMENT_WINDOW && back <= callIndex; back++) {
        if ((code[callIndex - back] & ARM64_LDR_W_IMM_MASK) == ARM64_LDR_W0_IMM) return 1;
    }
    return 0;
}

static uintptr_t nextSymbolStart(const SymbolTable *table, uintptr_t start) {
    uintptr_t next = UINTPTR_MAX;
    for (uint32_t index = 1; index < table->count; index++) {
        const Elf64_Sym *symbol = &table->symbols[index];
        if (symbol->st_shndx == SHN_UNDEF || symbol->st_value <= start) continue;
        if (symbol->st_value < next) next = symbol->st_value;
    }
    return next;
}

static int containsOffset(const uint32_t *offsets, size_t count, uint32_t offset) {
    for (size_t index = 0; index < count; index++) {
        if (offsets[index] == offset) return 1;
    }
    return 0;
}

static size_t collectLevelOffsets(const SymbolTable *table, const char *writer, uint32_t *offsets) {
    const Elf64_Sym *symbol = findSymbol(table, writer);
    if (symbol == NULL) return 0;
    const uintptr_t start = symbol->st_value;
    const uint32_t *code = (const uint32_t *) (table->base + start);
    const size_t length = (nextSymbolStart(table, start) - start) / INSTRUCTION_BYTES;
    size_t found = 0;
    for (size_t index = 1; index + 1 < length; index++) {
        if ((code[index] & ARM64_BL_MASK) != ARM64_BL) continue;
        if (!loadsLevelArgument(code, index)) continue;
        for (size_t next = index + 1; next < index + 1 + LEVEL_SCAN_WINDOW && next < length; next++) {
            if ((code[next] & ARM64_STR_W_IMM_MASK) != ARM64_STR_W0_IMM) continue;
            const uint32_t offset = ((code[next] >> ARM64_IMM12_SHIFT) & ARM64_IMM12_MASK) * ARM64_WORD_SCALE;
            if (found < LEVEL_CANDIDATES && !containsOffset(offsets, found, offset)) offsets[found++] = offset;
            break;
        }
    }
    return found;
}

static int commonLevelOffset(const SymbolTable *table, uint32_t *offset) {
    uint32_t candidates[LEVEL_WRITER_COUNT][LEVEL_CANDIDATES];
    size_t counts[LEVEL_WRITER_COUNT];
    for (size_t writer = 0; writer < LEVEL_WRITER_COUNT; writer++) {
        counts[writer] = collectLevelOffsets(table, LEVEL_WRITERS[writer], candidates[writer]);
        if (counts[writer] == 0) return 0;
    }
    int matches = 0;
    for (size_t first = 0; first < counts[0]; first++) {
        int sharedByAll = 1;
        for (size_t writer = 1; writer < LEVEL_WRITER_COUNT && sharedByAll; writer++) {
            int shared = 0;
            for (size_t other = 0; other < counts[writer]; other++) {
                if (candidates[writer][other] == candidates[0][first]) shared = 1;
            }
            sharedByAll = shared;
        }
        if (!sharedByAll) continue;
        if (matches > 0 && *offset != candidates[0][first]) return 0;
        *offset = candidates[0][first];
        matches = 1;
    }
    return matches;
}

static volatile int32_t *findLevelSlot(const SymbolTable *table) {
    const Elf64_Sym *state = findSymbol(table, APP_STATE_SYMBOL);
    if (state == NULL) {
        LOG_ERROR("missing %s", APP_STATE_SYMBOL);
        return NULL;
    }
    uint32_t offset = 0;
    if (!commonLevelOffset(table, &offset)) {
        LOG_ERROR("user level offset not found");
        return NULL;
    }
    if (offset + sizeof(int32_t) > state->st_size) {
        LOG_ERROR("user level offset %u outside %s", offset, APP_STATE_SYMBOL);
        return NULL;
    }
    LOG("user level slot %s+0x%x", APP_STATE_SYMBOL, offset);
    return (volatile int32_t *) (table->base + state->st_value + offset);
}

#endif

static void *patchGame(void *unused) {
    (void) unused;
    uintptr_t base = 0;
    for (long waited = 0; base == 0 && waited < SEARCH_TIMEOUT_US; waited += SEARCH_INTERVAL_US) {
        usleep(SEARCH_INTERVAL_US);
        base = findGameBase();
    }
    SymbolTable table;
    if (base == 0 || !loadSymbolTable(base, &table)) {
        LOG_ERROR("game library not found");
        return NULL;
    }
    size_t applied = 0;
    for (size_t index = 0; index < STUB_TARGET_COUNT; index++) {
        applied += applyStub(&table, &STUB_TARGETS[index]);
    }
    LOG("patched %zu of %zu functions", applied, STUB_TARGET_COUNT);
#if defined(MPBRIDGE_PREMIUM)
    volatile int32_t *level = findLevelSlot(&table);
    if (level == NULL) return NULL;
    for (;;) {
        if (*level != USER_LEVEL_MASTER) *level = USER_LEVEL_MASTER;
        usleep(LEVEL_INTERVAL_US);
    }
#else
    return NULL;
#endif
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;
    pthread_t thread;
    if (pthread_create(&thread, NULL, patchGame, NULL) == 0) {
        pthread_detach(thread);
    } else {
        LOG_ERROR("patch thread not started");
    }
    return JNI_VERSION_1_6;
}

#endif
