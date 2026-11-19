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

#include "Anchors.h"

#include <cstdint>
#include <cstring>
#include <vector>

namespace {

constexpr size_t MAX_TABLE_ENTRIES = 64;
constexpr uintptr_t ADDRESS_TAG_MASK = (uintptr_t{1} << 56) - 1;

enum class SlotResult {
    INVALID,
    NOT_READY,
    DONE,
};

struct SlotTableExpectation {
    size_t stride;
    size_t typeOffset;
    size_t levelOffset;
    std::vector<int32_t> types;
    std::vector<int32_t> levels;
};

inline bool validFieldOffset(size_t offset, size_t stride) {
    return offset % FIELD_ALIGNMENT == 0 && offset < stride && stride - offset >= sizeof(int32_t);
}

inline int32_t fieldAt(const std::vector<uint8_t> &bytes, size_t offset) {
    int32_t value = 0;
    std::memcpy(&value, bytes.data() + offset, sizeof(value));
    return value;
}

template <typename Memory>
SlotResult unlockSlotTable(Memory &memory, const TableLayout &layout, const SlotTableExpectation &expected) {
    const size_t entries = expected.types.size();
    if (entries == 0 || entries > MAX_TABLE_ENTRIES || expected.levels.size() != entries
            || layout.stride != expected.stride || layout.fieldOffset != expected.levelOffset
            || expected.typeOffset == expected.levelOffset || !validFieldOffset(expected.typeOffset, expected.stride)
            || !validFieldOffset(expected.levelOffset, expected.stride)) return SlotResult::INVALID;
    uintptr_t bounds[2] = {};
    if (!memory.read(layout.vector, bounds, sizeof(bounds))) return SlotResult::INVALID;
    const uintptr_t begin = bounds[0] & ADDRESS_TAG_MASK;
    const uintptr_t end = bounds[1] & ADDRESS_TAG_MASK;
    if (begin == 0 && end == 0) return SlotResult::NOT_READY;
    const size_t size = entries * expected.stride;
    if (begin == 0 || begin % FIELD_ALIGNMENT != 0 || end < begin || end - begin != size) return SlotResult::INVALID;
    std::vector<uint8_t> original(size);
    if (!memory.read(begin, original.data(), size) || !memory.writable(begin, size)) return SlotResult::INVALID;
    std::vector<uint8_t> unlocked = original;
    bool changed = false;
    for (size_t index = 0; index < entries; index++) {
        const size_t entry = index * expected.stride;
        const int32_t level = fieldAt(original, entry + expected.levelOffset);
        if (fieldAt(original, entry + expected.typeOffset) != expected.types[index]) return SlotResult::INVALID;
        if (level == 0) continue;
        if (level != expected.levels[index]) return SlotResult::INVALID;
        std::memset(unlocked.data() + entry + expected.levelOffset, 0, sizeof(int32_t));
        changed = true;
    }
    if (!changed) return SlotResult::DONE;
    const size_t written = memory.write(begin, unlocked.data(), size);
    if (written == size) return SlotResult::DONE;
    if (written > 0) memory.write(begin, original.data(), written);
    return SlotResult::INVALID;
}

}
