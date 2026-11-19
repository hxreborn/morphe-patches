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
#include "../src/ProcessMemory.h"
#include "../src/SlotTable.h"

#include <sys/mman.h>

#include <cassert>
#include <cstdio>

namespace {

constexpr size_t STRIDE = 0x1c;
constexpr size_t TYPE_OFFSET = 0xc;
constexpr size_t LEVEL_OFFSET = 0x14;
constexpr size_t OTHER_OFFSET = 0x18;
constexpr int32_t OTHER_VALUE = 1;
constexpr size_t TABLE_PAGE_OFFSET = 0x40;
constexpr size_t PAGES = 3;
constexpr size_t GUARD_PAGE = 2;
constexpr size_t PARTIAL_WRITE_BYTES = 0x20;
constexpr size_t SECOND_ENTRY = 1;
constexpr int32_t UNEXPECTED_VALUE = 7;
const std::vector<int32_t> TYPES = {0, 1, 2, 12, 3, 5, 12};
const std::vector<int32_t> LEVELS = {5, 36, 52, 0, 0, 0, 0};

enum class Mapping {
    READ_WRITE,
    READ_ONLY,
    UNMAPPED,
};

struct Table {
    size_t pageSize = static_cast<size_t>(sysconf(_SC_PAGESIZE));
    uint8_t *pages = nullptr;
    uintptr_t vector = 0;
    uintptr_t begin = 0;
    size_t size = TYPES.size() * STRIDE;

    Table() {
        void *memory = mmap(nullptr, PAGES * pageSize, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        assert(memory != MAP_FAILED);
        pages = static_cast<uint8_t *>(memory);
        assert(mprotect(pages + GUARD_PAGE * pageSize, pageSize, PROT_NONE) == 0);
        vector = reinterpret_cast<uintptr_t>(pages);
        begin = reinterpret_cast<uintptr_t>(pages + pageSize + TABLE_PAGE_OFFSET);
        for (size_t index = 0; index < TYPES.size(); index++) {
            setField(index, TYPE_OFFSET, TYPES[index]);
            setField(index, LEVEL_OFFSET, LEVELS[index]);
            setField(index, OTHER_OFFSET, OTHER_VALUE);
        }
        setBounds(begin, begin + size);
    }

    ~Table() {
        munmap(pages, PAGES * pageSize);
    }

    Table(const Table &) = delete;
    Table &operator=(const Table &) = delete;

    void setBounds(uintptr_t first, uintptr_t last) {
        const uintptr_t bounds[2] = {first, last};
        std::memcpy(pages, bounds, sizeof(bounds));
    }

    void setField(size_t index, size_t offset, int32_t value) {
        std::memcpy(reinterpret_cast<void *>(begin + index * STRIDE + offset), &value, sizeof(value));
    }

    int32_t field(size_t index, size_t offset) const {
        int32_t value = 0;
        std::memcpy(&value, reinterpret_cast<const void *>(begin + index * STRIDE + offset), sizeof(value));
        return value;
    }

    std::vector<uint8_t> bytes() const {
        const auto *first = reinterpret_cast<const uint8_t *>(begin);
        return std::vector<uint8_t>(first, first + size);
    }

    void map(Mapping mapping) {
        if (mapping == Mapping::READ_ONLY) assert(mprotect(pages + pageSize, pageSize, PROT_READ) == 0);
        if (mapping == Mapping::UNMAPPED) assert(munmap(pages + pageSize, pageSize) == 0);
    }

    TableLayout layout() const {
        return {vector, STRIDE, LEVEL_OFFSET};
    }
};

struct PartialWriteMemory {
    ProcessMemory memory;
    size_t writes = 0;

    bool read(uintptr_t address, void *buffer, size_t size) const {
        return memory.read(address, buffer, size);
    }

    bool writable(uintptr_t address, size_t size) const {
        return memory.writable(address, size);
    }

    size_t write(uintptr_t address, const void *buffer, size_t size) {
        return memory.write(address, buffer, writes++ == 0 ? PARTIAL_WRITE_BYTES : size);
    }
};

SlotTableExpectation expectation() {
    return {STRIDE, TYPE_OFFSET, LEVEL_OFFSET, TYPES, LEVELS};
}

SlotResult unlock(const TableLayout &layout, const SlotTableExpectation &expected) {
    ProcessMemory memory;
    return unlockSlotTable(memory, layout, expected);
}

void unlocksStockTable() {
    Table table;
    assert(unlock(table.layout(), expectation()) == SlotResult::DONE);
    for (size_t index = 0; index < TYPES.size(); index++) {
        assert(table.field(index, LEVEL_OFFSET) == 0);
        assert(table.field(index, TYPE_OFFSET) == TYPES[index]);
        assert(table.field(index, OTHER_OFFSET) == OTHER_VALUE);
    }
}

void acceptsUnlockedTable() {
    Table table;
    assert(unlock(table.layout(), expectation()) == SlotResult::DONE);
    const std::vector<uint8_t> unlocked = table.bytes();
    assert(unlock(table.layout(), expectation()) == SlotResult::DONE);
    assert(table.bytes() == unlocked);
}

void waitsForEmptyVector() {
    Table table;
    table.setBounds(0, 0);
    assert(unlock(table.layout(), expectation()) == SlotResult::NOT_READY);
}

void rejectsReadOnlyTable() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    table.map(Mapping::READ_ONLY);
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsUnmappedTable() {
    Table table;
    table.map(Mapping::UNMAPPED);
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
}

void rejectsUnmappedVector() {
    Table table;
    TableLayout layout = table.layout();
    layout.vector = table.begin;
    table.map(Mapping::UNMAPPED);
    assert(unlock(layout, expectation()) == SlotResult::INVALID);
}

void rejectsResolvedStrideMismatch() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    TableLayout layout = table.layout();
    layout.stride = STRIDE + FIELD_ALIGNMENT;
    assert(unlock(layout, expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsResolvedFieldMismatch() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    TableLayout layout = table.layout();
    layout.fieldOffset = OTHER_OFFSET;
    assert(unlock(layout, expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsEntryCountMismatch() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    table.setBounds(table.begin, table.begin + table.size - STRIDE);
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsMisalignedTable() {
    Table table;
    table.setBounds(table.begin + 1, table.begin + 1 + table.size);
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
}

void rejectsUnexpectedTypeWithoutWriting() {
    Table table;
    table.setField(TYPES.size() - 1, TYPE_OFFSET, UNEXPECTED_VALUE);
    const std::vector<uint8_t> stock = table.bytes();
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsUnexpectedLevelWithoutWriting() {
    Table table;
    table.setField(SECOND_ENTRY, LEVEL_OFFSET, UNEXPECTED_VALUE);
    const std::vector<uint8_t> stock = table.bytes();
    assert(unlock(table.layout(), expectation()) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void rejectsOverlappingFields() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    SlotTableExpectation expected = expectation();
    expected.typeOffset = LEVEL_OFFSET;
    assert(unlock(table.layout(), expected) == SlotResult::INVALID);
    assert(table.bytes() == stock);
}

void restoresTableAfterPartialWrite() {
    Table table;
    const std::vector<uint8_t> stock = table.bytes();
    PartialWriteMemory memory;
    assert(unlockSlotTable(memory, table.layout(), expectation()) == SlotResult::INVALID);
    assert(memory.writes == 2);
    assert(table.bytes() == stock);
}

void checksWritableMappingRange() {
    Table table;
    ProcessMemory memory;
    assert(memory.writable(table.begin, table.size));
    assert(!memory.writable(table.begin, table.pageSize));
    assert(!memory.writable(table.begin, 0));
}

}

int main() {
    unlocksStockTable();
    acceptsUnlockedTable();
    waitsForEmptyVector();
    rejectsReadOnlyTable();
    rejectsUnmappedTable();
    rejectsUnmappedVector();
    rejectsResolvedStrideMismatch();
    rejectsResolvedFieldMismatch();
    rejectsEntryCountMismatch();
    rejectsMisalignedTable();
    rejectsUnexpectedTypeWithoutWriting();
    rejectsUnexpectedLevelWithoutWriting();
    rejectsOverlappingFields();
    restoresTableAfterPartialWrite();
    checksWritableMappingRange();
    std::puts("SlotTableTest passed");
}
