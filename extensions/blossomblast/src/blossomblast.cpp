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
#include "Anchors.h"
#include "CapturedOutput.h"
#include "ProcessMemory.h"
#include "SlotTable.h"

#include <dlfcn.h>
#include <elf.h>
#include <jni.h>
#include <link.h>

#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {

constexpr char GAME_LIBRARY[] = "libBlossomFF.so";
constexpr char LIBRARY_SYMBOL[] = "Java_com_king_http_HttpManager_on_1data";
constexpr char GNU_NOTE_NAME[] = "GNU";
constexpr size_t NOTE_ALIGNMENT = 4;
constexpr size_t MAX_PROGRAM_HEADERS = 64;
constexpr size_t MAX_NOTE_BYTES = 0x1000;
constexpr size_t MAX_BUILD_ID_BYTES = 64;
constexpr size_t EXECUTE_SLOT = 4;
constexpr size_t LOCATOR_SCAN_BYTES = 0x1000;
constexpr uint64_t LENGTH_HIGH_BIT = uint64_t{1} << 63;
constexpr size_t OUTPUT_SLOTS = 8;
constexpr size_t OUTPUT_WRITE_SLOT = 2;
constexpr size_t OUTPUT_SINKS = 256;
constexpr jint NO_OUTPUT_SINK = -1;
constexpr size_t LAYOUT_STRIDE = 0;
constexpr size_t LAYOUT_TYPE_OFFSET = 1;
constexpr size_t LAYOUT_LEVEL_OFFSET = 2;
constexpr size_t LAYOUT_VALUES = 3;
constexpr jint INVALID = -1;
constexpr jint NOT_LOADED = 0;
constexpr jint NOT_READY = 1;
constexpr jint DONE = 2;
constexpr jint FAILED = 3;

using Execute = bool (*)(uintptr_t engine, const char *text, uint64_t length, uintptr_t sink);

enum class LibraryState {
    NOT_LOADED,
    INVALID,
    LOADED,
};

enum class Resolution {
    UNRESOLVED,
    RESOLVED,
    INVALID,
};

struct LoadedLibrary {
    uintptr_t base = 0;
    Image image;
    std::vector<uint8_t> buildId;
};

struct Console {
    Resolution state = Resolution::UNRESOLVED;
    uintptr_t engineVtable = 0;
    Execute execute = nullptr;
    uintptr_t locatorGlobal = 0;
};

struct Slots {
    Resolution state = Resolution::UNRESOLVED;
    TableLayout layout = {};
};

struct Game {
    bool loaded = false;
    LoadedLibrary library;
    Console console;
    Slots slots;
};

std::mutex gameLock;
Game game;

struct CaptureSink {
    const uintptr_t *vtable;
};

std::array<CaptureSink, OUTPUT_SINKS> captureSinks = {};
std::mutex outputLock;
std::array<CapturedOutput, OUTPUT_SINKS> capturedOutputs;

uintptr_t ignoreOutput(uintptr_t) {
    return 0;
}

bool sinkIndex(uintptr_t sink, size_t &index) {
    const uintptr_t first = reinterpret_cast<uintptr_t>(captureSinks.data());
    if (sink < first || (sink - first) % sizeof(CaptureSink) != 0) return false;
    index = (sink - first) / sizeof(CaptureSink);
    return index < OUTPUT_SINKS;
}

uintptr_t captureOutput(uintptr_t sink, const char *text, uint64_t length) {
    size_t index = 0;
    if (!sinkIndex(sink, index)) return 0;
    std::lock_guard<std::mutex> lock(outputLock);
    capturedOutputs[index].append(reinterpret_cast<uintptr_t>(text), static_cast<size_t>(length & ~LENGTH_HIGH_BIT));
    return 0;
}

struct OutputSlots {
    std::array<uintptr_t, OUTPUT_SLOTS> slots;

    OutputSlots() {
        slots.fill(reinterpret_cast<uintptr_t>(&ignoreOutput));
        slots[OUTPUT_WRITE_SLOT] = reinterpret_cast<uintptr_t>(&captureOutput);
    }
};

const OutputSlots outputSlots;

bool validSink(jint sink) {
    return sink >= 0 && static_cast<size_t>(sink) < OUTPUT_SINKS;
}

size_t alignedNoteSize(size_t size) {
    return (size + NOTE_ALIGNMENT - 1) & ~(NOTE_ALIGNMENT - 1);
}

bool buildIdIn(const Image &image, uintptr_t start, size_t size, std::vector<uint8_t> &buildId) {
    if (size > MAX_NOTE_BYTES || !image.contains(start, size, Access::READ)) return false;
    size_t offset = 0;
    while (size - offset >= sizeof(ElfW(Nhdr))) {
        ElfW(Nhdr) note = {};
        std::memcpy(&note, reinterpret_cast<const void *>(start + offset), sizeof(note));
        const size_t name = offset + sizeof(ElfW(Nhdr));
        const size_t nameSize = alignedNoteSize(note.n_namesz);
        if (note.n_namesz > size || nameSize > size - name) return false;
        const size_t description = name + nameSize;
        const size_t descriptionSize = alignedNoteSize(note.n_descsz);
        if (note.n_descsz > size || descriptionSize > size - description) return false;
        if (note.n_type == NT_GNU_BUILD_ID && note.n_namesz == sizeof(GNU_NOTE_NAME)
                && std::memcmp(reinterpret_cast<const void *>(start + name), GNU_NOTE_NAME,
                        sizeof(GNU_NOTE_NAME)) == 0) {
            if (note.n_descsz == 0 || note.n_descsz > MAX_BUILD_ID_BYTES) return false;
            const auto *bytes = reinterpret_cast<const uint8_t *>(start + description);
            buildId.assign(bytes, bytes + note.n_descsz);
            return true;
        }
        offset = description + descriptionSize;
    }
    return false;
}

struct LibrarySearch {
    uintptr_t base;
    LoadedLibrary *library;
    bool found;
};

int readLoadedLibrary(dl_phdr_info *info, size_t, void *data) {
    auto &search = *static_cast<LibrarySearch *>(data);
    if (info->dlpi_addr != search.base) return 0;
    if (info->dlpi_phnum > MAX_PROGRAM_HEADERS) return 1;
    LoadedLibrary &library = *search.library;
    for (size_t index = 0; index < info->dlpi_phnum; index++) {
        const auto &header = info->dlpi_phdr[index];
        if (header.p_type != PT_LOAD) continue;
        const uintptr_t start = info->dlpi_addr + header.p_vaddr;
        if (start < info->dlpi_addr || header.p_memsz > UINTPTR_MAX - start
                || !library.image.add({start, static_cast<uintptr_t>(start + header.p_memsz),
                        (header.p_flags & PF_R) != 0, (header.p_flags & PF_W) != 0,
                        (header.p_flags & PF_X) != 0})) return 1;
    }
    for (size_t index = 0; index < info->dlpi_phnum; index++) {
        const auto &header = info->dlpi_phdr[index];
        if (header.p_type == PT_NOTE
                && buildIdIn(library.image, info->dlpi_addr + header.p_vaddr, header.p_memsz, library.buildId)) {
            search.found = true;
            return 1;
        }
    }
    return 1;
}

LibraryState loadGame(JNIEnv *env, jbyteArray expectedBuildId) {
    void *handle = dlopen(GAME_LIBRARY, RTLD_NOW | RTLD_NOLOAD);
    if (handle == nullptr) return LibraryState::NOT_LOADED;
    std::unique_ptr<void, decltype(&dlclose)> opened(handle, &dlclose);
    void *symbol = dlsym(handle, LIBRARY_SYMBOL);
    Dl_info info = {};
    if (symbol == nullptr || dladdr(symbol, &info) == 0 || info.dli_fbase == nullptr) return LibraryState::INVALID;
    const auto base = reinterpret_cast<uintptr_t>(info.dli_fbase);
    if (!game.loaded || game.library.base != base) {
        game = {};
        LoadedLibrary library;
        library.base = base;
        LibrarySearch search = {base, &library, false};
        dl_iterate_phdr(readLoadedLibrary, &search);
        if (!search.found) return LibraryState::INVALID;
        game.library = library;
        game.loaded = true;
    }
    const jsize expectedSize = expectedBuildId == nullptr ? 0 : env->GetArrayLength(expectedBuildId);
    if (expectedSize <= 0 || static_cast<size_t>(expectedSize) != game.library.buildId.size()) return LibraryState::INVALID;
    std::vector<uint8_t> expected(static_cast<size_t>(expectedSize));
    env->GetByteArrayRegion(expectedBuildId, 0, expectedSize, reinterpret_cast<jbyte *>(expected.data()));
    return expected == game.library.buildId ? LibraryState::LOADED : LibraryState::INVALID;
}

struct UtfChars {
    JNIEnv *env;
    jstring value;
    const char *chars;

    UtfChars(JNIEnv *env, jstring value) : env(env), value(value),
            chars(value == nullptr ? nullptr : env->GetStringUTFChars(value, nullptr)) {
    }

    ~UtfChars() {
        if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
    }

    UtfChars(const UtfChars &) = delete;
    UtfChars &operator=(const UtfChars &) = delete;

    bool empty() const {
        return chars == nullptr || chars[0] == '\0';
    }
};

Resolution resolveConsole(JNIEnv *env, jstring engineType, jstring engineReference) {
    Console &console = game.console;
    if (console.state != Resolution::UNRESOLVED) return console.state;
    console.state = Resolution::INVALID;
    const UtfChars engineName(env, engineType);
    const UtfChars reference(env, engineReference);
    if (engineName.empty() || reference.empty()) return console.state;
    const Image &image = game.library.image;
    const uintptr_t vtable = image.vtable(engineName.chars);
    const uintptr_t referenceText = image.uniqueString(reference.chars);
    const uintptr_t referenceCode = referenceText == 0 ? 0 : image.uniqueAddressReference(referenceText);
    const uintptr_t locatorGlobal = referenceCode == 0 ? 0 : image.globalLoadedNear(referenceCode);
    uintptr_t execute = 0;
    if (vtable == 0 || locatorGlobal == 0 || !image.virtualMethod(vtable, EXECUTE_SLOT, execute)) {
        return console.state;
    }
    console = {Resolution::RESOLVED, vtable, reinterpret_cast<Execute>(execute), locatorGlobal};
    return console.state;
}

Resolution resolveSlots(JNIEnv *env, jstring slotType) {
    Slots &slots = game.slots;
    if (slots.state != Resolution::UNRESOLVED) return slots.state;
    slots.state = Resolution::INVALID;
    const UtfChars slotName(env, slotType);
    if (slotName.empty()) return slots.state;
    const Image &image = game.library.image;
    const uintptr_t slotVtable = image.vtable(slotName.chars);
    if (slotVtable != 0 && image.uniqueTableLayoutBefore(slotVtable, slots.layout)) slots.state = Resolution::RESOLVED;
    return slots.state;
}

uintptr_t findEngine(const ProcessMemory &memory) {
    uintptr_t locator = 0;
    if (!memory.read(game.console.locatorGlobal, &locator, sizeof(locator))) return 0;
    locator &= ADDRESS_TAG_MASK;
    if (locator == 0) return 0;
    uintptr_t found = 0;
    size_t matches = 0;
    for (uintptr_t offset = 0; offset < LOCATOR_SCAN_BYTES; offset += sizeof(uintptr_t)) {
        uintptr_t object = 0;
        uintptr_t vtable = 0;
        if (memory.read(locator + offset, &object, sizeof(object)) && object != 0
                && memory.read(object & ADDRESS_TAG_MASK, &vtable, sizeof(vtable))
                && vtable == game.console.engineVtable) {
            found = object;
            matches++;
        }
    }
    return matches == 1 ? found : 0;
}

jint executeCommand(JNIEnv *env, jbyteArray buildId, jstring engineType, jstring engineReference, jstring command,
        jint sink) {
    const UtfChars text(env, command);
    if (text.empty() || (sink != NO_OUTPUT_SINK && !validSink(sink))) return INVALID;
    std::lock_guard<std::mutex> lock(gameLock);
    const LibraryState library = loadGame(env, buildId);
    if (library != LibraryState::LOADED) return library == LibraryState::NOT_LOADED ? NOT_LOADED : INVALID;
    if (resolveConsole(env, engineType, engineReference) != Resolution::RESOLVED) return INVALID;
    const uintptr_t engine = findEngine(ProcessMemory());
    if (engine == 0) return NOT_READY;
    uintptr_t output = 0;
    if (sink != NO_OUTPUT_SINK) {
        CaptureSink &captureSink = captureSinks[static_cast<size_t>(sink)];
        captureSink.vtable = outputSlots.slots.data();
        output = reinterpret_cast<uintptr_t>(&captureSink);
    }
    return game.console.execute(engine, text.chars, std::strlen(text.chars) | LENGTH_HIGH_BIT, output) ? DONE : FAILED;
}

jstring takeCapturedOutput(JNIEnv *env, jint sink) {
    std::string output;
    if (validSink(sink)) {
        std::lock_guard<std::mutex> lock(outputLock);
        output = capturedOutputs[static_cast<size_t>(sink)].take();
    }
    return env->NewStringUTF(output.c_str());
}

bool readInts(JNIEnv *env, jintArray array, std::vector<int32_t> &values) {
    const jsize size = array == nullptr ? 0 : env->GetArrayLength(array);
    if (size <= 0 || static_cast<size_t>(size) > MAX_TABLE_ENTRIES) return false;
    values.resize(static_cast<size_t>(size));
    env->GetIntArrayRegion(array, 0, size, reinterpret_cast<jint *>(values.data()));
    return true;
}

jint unlockSlots(JNIEnv *env, jbyteArray buildId, jstring slotType, jintArray layout, jintArray types,
        jintArray levels) {
    std::vector<int32_t> layoutValues;
    SlotTableExpectation expected = {};
    if (!readInts(env, layout, layoutValues) || layoutValues.size() != LAYOUT_VALUES
            || !readInts(env, types, expected.types) || !readInts(env, levels, expected.levels)) return INVALID;
    for (int32_t value : layoutValues) {
        if (value < 0) return INVALID;
    }
    expected.stride = static_cast<size_t>(layoutValues[LAYOUT_STRIDE]);
    expected.typeOffset = static_cast<size_t>(layoutValues[LAYOUT_TYPE_OFFSET]);
    expected.levelOffset = static_cast<size_t>(layoutValues[LAYOUT_LEVEL_OFFSET]);
    std::lock_guard<std::mutex> lock(gameLock);
    const LibraryState library = loadGame(env, buildId);
    if (library != LibraryState::LOADED) return library == LibraryState::NOT_LOADED ? NOT_LOADED : INVALID;
    if (resolveSlots(env, slotType) != Resolution::RESOLVED) return INVALID;
    ProcessMemory memory;
    switch (unlockSlotTable(memory, game.slots.layout, expected)) {
        case SlotResult::DONE:
            return DONE;
        case SlotResult::NOT_READY:
            return NOT_READY;
        case SlotResult::INVALID:
            break;
    }
    return INVALID;
}

}

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_blossomblast_GameModifiers_executeGameCommand(JNIEnv *env, jclass, jbyteArray buildId,
        jstring engineType, jstring engineReference, jstring command, jint sink) {
    return executeCommand(env, buildId, engineType, engineReference, command, sink);
}

extern "C" JNIEXPORT jstring JNICALL
Java_app_hxreborn_extension_blossomblast_GameModifiers_takeCapturedOutput(JNIEnv *env, jclass, jint sink) {
    return takeCapturedOutput(env, sink);
}

extern "C" JNIEXPORT jint JNICALL
Java_app_hxreborn_extension_blossomblast_GameModifiers_unlockGameSlots(JNIEnv *env, jclass, jbyteArray buildId,
        jstring slotType, jintArray layout, jintArray types, jintArray levels) {
    return unlockSlots(env, buildId, slotType, layout, types, levels);
}
