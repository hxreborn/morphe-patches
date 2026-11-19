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
#include "../src/Anchors.h"

#include <sys/mman.h>

#include <cassert>
#include <cstdio>

namespace {

using namespace arm64;

constexpr size_t TEXT_SIZE = 0x2000;
constexpr size_t RODATA_SIZE = 0x1000;
constexpr size_t DATA_SIZE = 0x1000;
constexpr uint32_t NOP = 0xd503201f;
constexpr uint32_t BRANCH_TO_NEXT = 0x14000001;
constexpr uint32_t CALL_NEXT = 0x94000001;
constexpr uint32_t LOAD_BYTE = 0x39400000;
constexpr uint32_t STORE_WORD = 0xb9000000;
constexpr uint32_t LOAD_PAIR_POST_INDEX = 0xa8c00000;
constexpr uint32_t STORE_PAIR = 0xa9000000;
constexpr char TYPE_NAME[] = "N4test6EngineE";
constexpr char REFERENCE_TEXT[] = "console_autoexec.txt";
constexpr size_t TYPE_NAME_OFFSET = 0x10;
constexpr size_t REFERENCE_TEXT_OFFSET = 0x40;
constexpr size_t TYPE_INFO_OFFSET = 0x100;
constexpr size_t VTABLE_OFFSET = 0x200;
constexpr size_t GLOBAL_OFFSET = 0x400;
constexpr size_t SECOND_GLOBAL_OFFSET = 0x480;
constexpr size_t TABLE_OFFSET = 0x800;
constexpr size_t METHOD_OFFSET = 0x1800;
constexpr size_t VIRTUAL_METHODS = 5;
constexpr size_t EXECUTE_SLOT = 4;
constexpr uintptr_t TYPE_INFO_VTABLE = 0x10;
constexpr uintptr_t GLOBAL_CODE = 0x100;
constexpr uintptr_t TABLE_CODE = 0x400;
constexpr uintptr_t SECOND_TABLE_CODE = 0x500;
constexpr uintptr_t SEGMENT_START = 0x1000;
constexpr uintptr_t SEGMENT_END = 0x2000;
constexpr uintptr_t PAST_SEGMENT_END = 0x2004;
constexpr uint32_t STRIDE = 0x1c;
constexpr uint32_t FIELD_OFFSET = 0x14;
constexpr uint32_t UNRELATED_FIELD_OFFSET = 0x200;
constexpr uint32_t ADRP_IMMEDIATE_MASK = (uint32_t{1} << ADRP_IMMEDIATE_BITS) - 1;
constexpr uint32_t VECTOR_REGISTER = 21;
constexpr uint32_t CALLER_SAVED_VECTOR_REGISTER = 10;
constexpr uint32_t STRIDE_REGISTER = 25;
constexpr uint32_t ELEMENT_REGISTER = 8;
constexpr uint32_t END_REGISTER = 9;
constexpr uint32_t INDEX_REGISTER = 9;
constexpr uint32_t OTHER_REGISTER = 11;
constexpr uint32_t FRAME_REGISTER = 28;
constexpr uint32_t LOCATOR_REGISTER = 8;
constexpr uint32_t REFERENCE_REGISTER = 1;
constexpr uint32_t COPY_REGISTER = 20;
constexpr uint32_t UNKNOWN_REGISTER = 22;
constexpr uint32_t BRANCH_OFFSET_MASK = (uint32_t{1} << BRANCH_OFFSET_BITS) - 1;
constexpr uintptr_t OUTSIDE_BRANCH_CODE = 0x1000;

enum Step {
    VECTOR_PAGE,
    VECTOR_ADD,
    STRIDE_MOVE,
    VECTOR_LOAD,
    MULTIPLY,
    UNRELATED_LOAD,
    FIELD_LOAD,
    REFERENCE_PAGE,
    REFERENCE_ADD,
    STEPS,
};

struct Fixture {
    uintptr_t text;
    uintptr_t rodata;
    uintptr_t data;
    Image image;

    Fixture() {
        void *memory = mmap(nullptr, TEXT_SIZE + RODATA_SIZE + DATA_SIZE, PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        assert(memory != MAP_FAILED);
        text = reinterpret_cast<uintptr_t>(memory);
        rodata = text + TEXT_SIZE;
        data = rodata + RODATA_SIZE;
        for (uintptr_t pc = text; pc < rodata; pc += INSTRUCTION_SIZE) put(pc, NOP);
        addSegments(true, true, true);
    }

    void addSegments(bool textReadable, bool rodataReadable, bool dataReadable) {
        image = Image();
        image.add({text, rodata, textReadable, false, true});
        image.add({rodata, data, rodataReadable, false, false});
        image.add({data, data + DATA_SIZE, dataReadable, true, false});
    }

    ~Fixture() {
        munmap(reinterpret_cast<void *>(text), TEXT_SIZE + RODATA_SIZE + DATA_SIZE);
    }

    Fixture(const Fixture &) = delete;
    Fixture &operator=(const Fixture &) = delete;

    static void put(uintptr_t address, uint32_t instruction) {
        std::memcpy(reinterpret_cast<void *>(address), &instruction, sizeof(instruction));
    }

    static void putWord(uintptr_t address, uintptr_t value) {
        std::memcpy(reinterpret_cast<void *>(address), &value, sizeof(value));
    }

    static uint32_t adrp(uint32_t reg, uintptr_t pc, uintptr_t target) {
        const uint32_t pages = static_cast<uint32_t>(((target >> PAGE_SHIFT) - (pc >> PAGE_SHIFT)) & ADRP_IMMEDIATE_MASK);
        return ADRP | ((pages & ADRP_LOW_MASK) << ADRP_LOW_SHIFT) | ((pages >> ADRP_LOW_BITS) << ADRP_HIGH_SHIFT) | reg;
    }

    static uint32_t add(uint32_t rd, uint32_t rn, uintptr_t target) {
        return ADD_IMMEDIATE | static_cast<uint32_t>(target & PAGE_OFFSET_MASK) << IMMEDIATE12_SHIFT
                | rn << BASE_REGISTER_SHIFT | rd;
    }

    static uint32_t loadDoubleword(uint32_t rt, uint32_t rn, uintptr_t target) {
        return LOAD_DOUBLEWORD | static_cast<uint32_t>((target & PAGE_OFFSET_MASK) / sizeof(uintptr_t))
                << IMMEDIATE12_SHIFT | rn << BASE_REGISTER_SHIFT | rt;
    }

    static uint32_t loadPair(uint32_t rt, uint32_t rt2, uint32_t rn) {
        return LOAD_PAIR_DOUBLEWORD | rt2 << SECOND_TRANSFER_SHIFT | rn << BASE_REGISTER_SHIFT | rt;
    }

    static uint32_t loadWord(uint32_t rt, uint32_t rn, uint32_t offset) {
        return LOAD_WORD | (offset / sizeof(uint32_t)) << IMMEDIATE12_SHIFT | rn << BASE_REGISTER_SHIFT | rt;
    }

    static uint32_t moveWord(uint32_t rd, uint32_t value) {
        return MOVE_WIDE_WORD | value << MOVE_IMMEDIATE_SHIFT | rd;
    }

    static uint32_t multiplyAdd(uint32_t rd, uint32_t rn, uint32_t rm, uint32_t ra) {
        return SIGNED_MULTIPLY_ADD_LONG | rm << SECOND_SOURCE_SHIFT | ra << IMMEDIATE12_SHIFT
                | rn << BASE_REGISTER_SHIFT | rd;
    }

    static uint32_t branch(uintptr_t pc, uintptr_t target) {
        const int64_t offset = (static_cast<int64_t>(target) - static_cast<int64_t>(pc))
                / static_cast<int64_t>(INSTRUCTION_SIZE);
        return UNCONDITIONAL_BRANCH | (static_cast<uint32_t>(offset) & BRANCH_OFFSET_MASK);
    }

    static uint32_t moveRegister(uint32_t rd, uint32_t rm) {
        return MOVE_REGISTER | rm << SECOND_SOURCE_SHIFT | rd;
    }

    static uintptr_t step(uintptr_t at, size_t index) {
        return at + index * INSTRUCTION_SIZE;
    }

    void addTypeName(size_t offset) {
        std::memcpy(reinterpret_cast<void *>(rodata + offset), TYPE_NAME, sizeof(TYPE_NAME));
    }

    uintptr_t vtableAddress() const {
        return data + VTABLE_OFFSET + 2 * sizeof(uintptr_t);
    }

    void addVtable() {
        putWord(data + TYPE_INFO_OFFSET, TYPE_INFO_VTABLE);
        putWord(data + TYPE_INFO_OFFSET + sizeof(uintptr_t), rodata + TYPE_NAME_OFFSET);
        putWord(data + VTABLE_OFFSET, 0);
        putWord(data + VTABLE_OFFSET + sizeof(uintptr_t), data + TYPE_INFO_OFFSET);
        for (size_t slot = 0; slot < VIRTUAL_METHODS; slot++) {
            putWord(vtableAddress() + slot * sizeof(uintptr_t), text + METHOD_OFFSET + slot * INSTRUCTION_SIZE);
        }
    }

    uintptr_t referenceText() {
        std::memcpy(reinterpret_cast<void *>(rodata + REFERENCE_TEXT_OFFSET), REFERENCE_TEXT, sizeof(REFERENCE_TEXT));
        return rodata + REFERENCE_TEXT_OFFSET;
    }

    uintptr_t addGlobalLoad(uintptr_t at, size_t globalOffset) {
        const uintptr_t global = data + globalOffset;
        const uintptr_t reference = referenceText();
        put(at, adrp(LOCATOR_REGISTER, at, global));
        put(step(at, 1), adrp(REFERENCE_REGISTER, step(at, 1), reference));
        put(step(at, 2), add(REFERENCE_REGISTER, REFERENCE_REGISTER, reference));
        put(step(at, 3), loadDoubleword(LOCATOR_REGISTER, LOCATOR_REGISTER, global));
        return step(at, 1);
    }

    uintptr_t addTableAccess(uintptr_t at) {
        const uintptr_t table = data + TABLE_OFFSET;
        const uintptr_t vtable = vtableAddress();
        put(step(at, VECTOR_PAGE), adrp(VECTOR_REGISTER, step(at, VECTOR_PAGE), table));
        put(step(at, VECTOR_ADD), add(VECTOR_REGISTER, VECTOR_REGISTER, table));
        put(step(at, STRIDE_MOVE), moveWord(STRIDE_REGISTER, STRIDE));
        put(step(at, VECTOR_LOAD), loadPair(ELEMENT_REGISTER, END_REGISTER, VECTOR_REGISTER));
        put(step(at, MULTIPLY), multiplyAdd(ELEMENT_REGISTER, INDEX_REGISTER, STRIDE_REGISTER, ELEMENT_REGISTER));
        put(step(at, UNRELATED_LOAD), loadWord(INDEX_REGISTER, FRAME_REGISTER, UNRELATED_FIELD_OFFSET));
        put(step(at, FIELD_LOAD), loadWord(ELEMENT_REGISTER, ELEMENT_REGISTER, FIELD_OFFSET));
        put(step(at, REFERENCE_PAGE), adrp(ELEMENT_REGISTER, step(at, REFERENCE_PAGE), vtable));
        put(step(at, REFERENCE_ADD), add(ELEMENT_REGISTER, ELEMENT_REGISTER, vtable));
        return vtable;
    }

    bool tableLayout(TableLayout &layout) const {
        return image.uniqueTableLayoutBefore(vtableAddress(), layout);
    }
};

void containsRejectsAddressPastSegmentEnd() {
    Image image;
    image.add({SEGMENT_START, SEGMENT_END, true, false, true});
    assert(!image.contains(PAST_SEGMENT_END, INSTRUCTION_SIZE, Access::EXECUTE));
    assert(!image.contains(SEGMENT_END, INSTRUCTION_SIZE, Access::READ));
    assert(!image.contains(SEGMENT_END - INSTRUCTION_SIZE / 2, INSTRUCTION_SIZE, Access::READ));
    assert(!image.contains(SEGMENT_START, 0, Access::READ));
    assert(image.contains(SEGMENT_END - INSTRUCTION_SIZE, INSTRUCTION_SIZE, Access::EXECUTE));
}

void containsChecksPermissions() {
    Image image;
    image.add({SEGMENT_START, SEGMENT_END, true, false, true});
    assert(image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ));
    assert(image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ_EXECUTE));
    assert(!image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ_WRITE));
}

void executeOnlySegmentIsNotReadable() {
    Image image;
    image.add({SEGMENT_START, SEGMENT_END, false, false, true});
    assert(image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::EXECUTE));
    assert(!image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ));
    assert(!image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ_EXECUTE));
    assert(!image.contains(SEGMENT_START, INSTRUCTION_SIZE, Access::READ_WRITE));
}

void writeOnlySegmentIsNotReadWrite() {
    Image image;
    image.add({SEGMENT_START, SEGMENT_END, false, true, false});
    assert(!image.contains(SEGMENT_START, sizeof(uintptr_t), Access::READ));
    assert(!image.contains(SEGMENT_START, sizeof(uintptr_t), Access::READ_WRITE));
}

void rejectsInstructionsInExecuteOnlyCode() {
    Fixture fixture;
    fixture.addTableAccess(fixture.text + TABLE_CODE);
    fixture.addSegments(false, true, true);
    uint32_t instruction = 0;
    assert(!fixture.image.readInstruction(fixture.text + TABLE_CODE, instruction));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsTypeNameInUnreadableSegment() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    fixture.addSegments(true, false, true);
    assert(fixture.image.uniqueString(TYPE_NAME) == 0);
    assert(fixture.image.vtable(TYPE_NAME) == 0);
}

void rejectsWordsInUnreadableSegment() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    fixture.addSegments(true, true, false);
    uintptr_t word = 0;
    assert(!fixture.image.readWord(fixture.vtableAddress(), word));
    assert(fixture.image.vtable(TYPE_NAME) == 0);
}

void readsRejectOutsideImage() {
    Fixture fixture;
    uint32_t instruction = 0;
    uintptr_t word = 0;
    assert(!fixture.image.readInstruction(fixture.rodata, instruction));
    assert(!fixture.image.readInstruction(fixture.text + 1, instruction));
    assert(!fixture.image.readWord(fixture.data + DATA_SIZE, word));
    assert(!fixture.image.readWord(fixture.data + DATA_SIZE - sizeof(uint32_t), word));
}

void classifiesRegisterWrites() {
    assert(writesRegister(CALL_NEXT, ELEMENT_REGISTER));
    assert(!writesRegister(CALL_NEXT, VECTOR_REGISTER));
    assert(!writesRegister(BRANCH_TO_NEXT, ELEMENT_REGISTER));
    assert(!writesRegister(STORE_WORD | ELEMENT_REGISTER, ELEMENT_REGISTER));
    assert(writesRegister(LOAD_WORD | ELEMENT_REGISTER, ELEMENT_REGISTER));
    assert(!writesRegister(STORE_PAIR | ELEMENT_REGISTER, ELEMENT_REGISTER));
    assert(writesRegister(Fixture::loadPair(OTHER_REGISTER, ELEMENT_REGISTER, VECTOR_REGISTER), ELEMENT_REGISTER));
    assert(writesRegister(LOAD_PAIR_POST_INDEX | VECTOR_REGISTER << BASE_REGISTER_SHIFT, VECTOR_REGISTER));
    assert(writesRegister(Fixture::moveWord(STRIDE_REGISTER, STRIDE), STRIDE_REGISTER));
}

void resolvesVtableFromTypeName() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    uintptr_t method = 0;
    assert(fixture.image.vtable(TYPE_NAME) == fixture.vtableAddress());
    assert(fixture.image.virtualMethod(fixture.vtableAddress(), EXECUTE_SLOT, method));
    assert(method == fixture.text + METHOD_OFFSET + EXECUTE_SLOT * INSTRUCTION_SIZE);
}

void rejectsDuplicateTypeName() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addTypeName(TYPE_NAME_OFFSET + 0x80);
    fixture.addVtable();
    assert(fixture.image.vtable(TYPE_NAME) == 0);
}

void rejectsTypeInfoReferencedTwice() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    Fixture::putWord(fixture.data + SECOND_GLOBAL_OFFSET, fixture.data + TYPE_INFO_OFFSET);
    assert(fixture.image.vtable(TYPE_NAME) == 0);
}

void rejectsVtableWithoutCode() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    Fixture::putWord(fixture.vtableAddress(), fixture.rodata);
    assert(fixture.image.vtable(TYPE_NAME) == 0);
}

void rejectsExecuteSlotOutsideCode() {
    Fixture fixture;
    fixture.addTypeName(TYPE_NAME_OFFSET);
    fixture.addVtable();
    Fixture::putWord(fixture.vtableAddress() + EXECUTE_SLOT * sizeof(uintptr_t), fixture.text + TEXT_SIZE);
    uintptr_t method = 0;
    assert(!fixture.image.virtualMethod(fixture.vtableAddress(), EXECUTE_SLOT, method));
}

void resolvesGlobalLoadedBesideReference() {
    Fixture fixture;
    const uintptr_t expected = fixture.addGlobalLoad(fixture.text + GLOBAL_CODE, GLOBAL_OFFSET);
    const uintptr_t text = fixture.image.uniqueString(REFERENCE_TEXT);
    assert(text == fixture.rodata + REFERENCE_TEXT_OFFSET);
    const uintptr_t reference = fixture.image.uniqueAddressReference(text);
    assert(reference == expected);
    assert(fixture.image.globalLoadedNear(reference) == fixture.data + GLOBAL_OFFSET);
}

void rejectsReferenceWhenBaseOverwrittenBeforeAdd() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    Fixture::put(Fixture::step(at, 3), Fixture::add(REFERENCE_REGISTER, REFERENCE_REGISTER, fixture.referenceText()));
    Fixture::put(Fixture::step(at, 2), Fixture::moveWord(REFERENCE_REGISTER, 0));
    assert(fixture.image.uniqueAddressReference(fixture.image.uniqueString(REFERENCE_TEXT)) == 0);
}

void rejectsReferenceAcrossBranch() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    Fixture::put(Fixture::step(at, 3), Fixture::add(REFERENCE_REGISTER, REFERENCE_REGISTER, fixture.referenceText()));
    Fixture::put(Fixture::step(at, 2), BRANCH_TO_NEXT);
    assert(fixture.image.uniqueAddressReference(fixture.image.uniqueString(REFERENCE_TEXT)) == 0);
}

void ignoresGlobalOutsideWritableSegments() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    const uintptr_t reference = fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    Fixture::put(at, Fixture::adrp(LOCATOR_REGISTER, at, fixture.rodata));
    Fixture::put(Fixture::step(at, 3), Fixture::loadDoubleword(LOCATOR_REGISTER, LOCATOR_REGISTER, fixture.rodata));
    assert(fixture.image.globalLoadedNear(reference) == 0);
}

void rejectsGlobalWhenBaseOverwritten() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    const uintptr_t reference = fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    Fixture::put(Fixture::step(at, 2), Fixture::moveWord(LOCATOR_REGISTER, 0));
    assert(fixture.image.globalLoadedNear(reference) == 0);
}

void rejectsGlobalWhenFirstUseIsByteLoad() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    const uintptr_t reference = fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    Fixture::put(Fixture::step(at, 2), LOAD_BYTE | LOCATOR_REGISTER << BASE_REGISTER_SHIFT | OTHER_REGISTER);
    assert(fixture.image.globalLoadedNear(reference) == 0);
}

void rejectsTwoGlobalsBesideReference() {
    Fixture fixture;
    const uintptr_t at = fixture.text + GLOBAL_CODE;
    const uintptr_t reference = fixture.addGlobalLoad(at, GLOBAL_OFFSET);
    const uintptr_t second = Fixture::step(at, 4);
    const uintptr_t global = fixture.data + SECOND_GLOBAL_OFFSET;
    Fixture::put(second, Fixture::adrp(OTHER_REGISTER, second, global));
    Fixture::put(Fixture::step(second, 1), Fixture::loadDoubleword(OTHER_REGISTER, OTHER_REGISTER, global));
    assert(fixture.image.globalLoadedNear(reference) == 0);
}

void resolvesTableLayoutBeforeVtableReference() {
    Fixture fixture;
    fixture.addTableAccess(fixture.text + TABLE_CODE);
    TableLayout layout = {};
    assert(fixture.tableLayout(layout));
    assert(layout.vector == fixture.data + TABLE_OFFSET);
    assert(layout.stride == STRIDE);
    assert(layout.fieldOffset == FIELD_OFFSET);
}

void rejectsTableLayoutWithoutMultiply() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(Fixture::step(at, MULTIPLY), NOP);
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsFieldBeyondStride() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(Fixture::step(at, STRIDE_MOVE), Fixture::moveWord(STRIDE_REGISTER, FIELD_OFFSET));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsAccumulatorNotLoadedFromVector() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(Fixture::step(at, VECTOR_LOAD), Fixture::loadPair(OTHER_REGISTER, END_REGISTER, VECTOR_REGISTER));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void resolvesWhenCallPreservesVectorBase() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(at - INSTRUCTION_SIZE, Fixture::moveWord(STRIDE_REGISTER, STRIDE));
    Fixture::put(Fixture::step(at, STRIDE_MOVE), CALL_NEXT);
    TableLayout layout = {};
    assert(fixture.tableLayout(layout));
    assert(layout.vector == fixture.data + TABLE_OFFSET);
}

void rejectsVectorBaseOverwrittenByCall() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    const uintptr_t table = fixture.data + TABLE_OFFSET;
    fixture.addTableAccess(at);
    Fixture::put(at - INSTRUCTION_SIZE, Fixture::moveWord(STRIDE_REGISTER, STRIDE));
    Fixture::put(Fixture::step(at, VECTOR_PAGE),
            Fixture::adrp(CALLER_SAVED_VECTOR_REGISTER, Fixture::step(at, VECTOR_PAGE), table));
    Fixture::put(Fixture::step(at, VECTOR_ADD),
            Fixture::add(CALLER_SAVED_VECTOR_REGISTER, CALLER_SAVED_VECTOR_REGISTER, table));
    Fixture::put(Fixture::step(at, STRIDE_MOVE), CALL_NEXT);
    Fixture::put(Fixture::step(at, VECTOR_LOAD),
            Fixture::loadPair(ELEMENT_REGISTER, END_REGISTER, CALLER_SAVED_VECTOR_REGISTER));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsStrideRegisterOverwritten() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(Fixture::step(at, STRIDE_MOVE), Fixture::add(STRIDE_REGISTER, STRIDE_REGISTER, STRIDE));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsElementOverwrittenBeforeField() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(Fixture::step(at, UNRELATED_LOAD), Fixture::moveWord(ELEMENT_REGISTER, 0));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsTwoTableLayoutsBeforeReference() {
    Fixture fixture;
    const uintptr_t first = fixture.text + TABLE_CODE;
    const uintptr_t second = fixture.text + SECOND_TABLE_CODE - REFERENCE_PAGE * INSTRUCTION_SIZE;
    fixture.addTableAccess(first);
    fixture.addTableAccess(second);
    Fixture::put(Fixture::step(first, REFERENCE_PAGE), NOP);
    Fixture::put(Fixture::step(first, REFERENCE_ADD), NOP);
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsVectorLoadSkippedByBranch() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    Fixture::put(at - INSTRUCTION_SIZE, Fixture::moveWord(STRIDE_REGISTER, STRIDE));
    Fixture::put(Fixture::step(at, STRIDE_MOVE),
            Fixture::branch(Fixture::step(at, STRIDE_MOVE), Fixture::step(at, MULTIPLY)));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsBranchIntoFieldLoad() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    const uintptr_t after = Fixture::step(at, STEPS);
    Fixture::put(after, Fixture::branch(after, Fixture::step(at, FIELD_LOAD)));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void rejectsBranchFromOutsideRegionIntoVectorLoad() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    const uintptr_t outside = fixture.text + OUTSIDE_BRANCH_CODE;
    Fixture::put(outside, Fixture::branch(outside, Fixture::step(at, VECTOR_LOAD)));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

void resolvesVectorBaseCopiedAcrossBackEdge() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    const uintptr_t after = Fixture::step(at, STEPS);
    Fixture::put(after, Fixture::moveRegister(COPY_REGISTER, VECTOR_REGISTER));
    Fixture::put(Fixture::step(after, 1), Fixture::moveWord(VECTOR_REGISTER, 0));
    Fixture::put(Fixture::step(after, 2), Fixture::moveRegister(VECTOR_REGISTER, COPY_REGISTER));
    Fixture::put(Fixture::step(after, 3), Fixture::branch(Fixture::step(after, 3), Fixture::step(at, VECTOR_LOAD)));
    TableLayout layout = {};
    assert(fixture.tableLayout(layout));
    assert(layout.vector == fixture.data + TABLE_OFFSET);
}

void ignoresWriterBeforeUnconditionalBranch() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    const uintptr_t table = fixture.data + TABLE_OFFSET;
    fixture.addTableAccess(at);
    const uintptr_t page = at - 6 * INSTRUCTION_SIZE;
    Fixture::put(page, Fixture::adrp(VECTOR_REGISTER, page, table));
    Fixture::put(Fixture::step(page, 1), Fixture::add(VECTOR_REGISTER, VECTOR_REGISTER, table));
    Fixture::put(Fixture::step(page, 2), Fixture::branch(Fixture::step(page, 2), at));
    Fixture::put(Fixture::step(page, 4), Fixture::moveWord(VECTOR_REGISTER, 0));
    Fixture::put(Fixture::step(page, 5), Fixture::branch(Fixture::step(page, 5), Fixture::step(at, STEPS + 2)));
    Fixture::put(Fixture::step(at, VECTOR_PAGE), NOP);
    Fixture::put(Fixture::step(at, VECTOR_ADD), NOP);
    TableLayout layout = {};
    assert(fixture.tableLayout(layout));
    assert(layout.vector == table);
}

void rejectsVectorBaseReplacedOnBackEdge() {
    Fixture fixture;
    const uintptr_t at = fixture.text + TABLE_CODE;
    fixture.addTableAccess(at);
    const uintptr_t after = Fixture::step(at, STEPS);
    Fixture::put(after, Fixture::moveWord(UNKNOWN_REGISTER, 0));
    Fixture::put(Fixture::step(after, 1), Fixture::moveRegister(VECTOR_REGISTER, UNKNOWN_REGISTER));
    Fixture::put(Fixture::step(after, 2), Fixture::branch(Fixture::step(after, 2), Fixture::step(at, VECTOR_LOAD)));
    TableLayout layout = {};
    assert(!fixture.tableLayout(layout));
}

}

int main() {
    containsRejectsAddressPastSegmentEnd();
    containsChecksPermissions();
    executeOnlySegmentIsNotReadable();
    writeOnlySegmentIsNotReadWrite();
    rejectsInstructionsInExecuteOnlyCode();
    rejectsTypeNameInUnreadableSegment();
    rejectsWordsInUnreadableSegment();
    readsRejectOutsideImage();
    classifiesRegisterWrites();
    resolvesVtableFromTypeName();
    rejectsDuplicateTypeName();
    rejectsTypeInfoReferencedTwice();
    rejectsVtableWithoutCode();
    rejectsExecuteSlotOutsideCode();
    resolvesGlobalLoadedBesideReference();
    rejectsReferenceWhenBaseOverwrittenBeforeAdd();
    rejectsReferenceAcrossBranch();
    ignoresGlobalOutsideWritableSegments();
    rejectsGlobalWhenBaseOverwritten();
    rejectsGlobalWhenFirstUseIsByteLoad();
    rejectsTwoGlobalsBesideReference();
    resolvesTableLayoutBeforeVtableReference();
    rejectsTableLayoutWithoutMultiply();
    rejectsFieldBeyondStride();
    rejectsAccumulatorNotLoadedFromVector();
    resolvesWhenCallPreservesVectorBase();
    rejectsVectorBaseOverwrittenByCall();
    rejectsStrideRegisterOverwritten();
    rejectsElementOverwrittenBeforeField();
    rejectsTwoTableLayoutsBeforeReference();
    rejectsVectorLoadSkippedByBranch();
    rejectsBranchIntoFieldLoad();
    rejectsBranchFromOutsideRegionIntoVectorLoad();
    resolvesVectorBaseCopiedAcrossBackEdge();
    rejectsVectorBaseReplacedOnBackEdge();
    ignoresWriterBeforeUnconditionalBranch();
    std::puts("AnchorsTest passed");
}
