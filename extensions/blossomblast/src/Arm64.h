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

#include <cstddef>
#include <cstdint>

namespace arm64 {

constexpr size_t INSTRUCTION_SIZE = sizeof(uint32_t);
constexpr uint32_t REGISTER_MASK = 31;
constexpr uint32_t ZERO_REGISTER = 31;
constexpr uint32_t LAST_CALLER_SAVED_REGISTER = 18;
constexpr uint32_t LINK_REGISTER = 30;
constexpr unsigned BASE_REGISTER_SHIFT = 5;
constexpr unsigned SECOND_TRANSFER_SHIFT = 10;
constexpr unsigned SECOND_SOURCE_SHIFT = 16;
constexpr unsigned IMMEDIATE12_SHIFT = 10;
constexpr uint32_t IMMEDIATE12_MASK = 0xfff;
constexpr unsigned PAIR_IMMEDIATE_SHIFT = 15;
constexpr uint32_t PAIR_IMMEDIATE_MASK = 0x7f;
constexpr unsigned MOVE_IMMEDIATE_SHIFT = 5;
constexpr uint32_t MOVE_IMMEDIATE_MASK = 0xffff;
constexpr uint32_t ADRP_MASK = 0x9f000000;
constexpr uint32_t ADRP = 0x90000000;
constexpr unsigned ADRP_LOW_SHIFT = 29;
constexpr uint32_t ADRP_LOW_MASK = 3;
constexpr unsigned ADRP_HIGH_SHIFT = 5;
constexpr uint32_t ADRP_HIGH_MASK = 0x7ffff;
constexpr unsigned ADRP_LOW_BITS = 2;
constexpr unsigned ADRP_IMMEDIATE_BITS = 21;
constexpr unsigned PAGE_SHIFT = 12;
constexpr uintptr_t PAGE_OFFSET_MASK = (uintptr_t{1} << PAGE_SHIFT) - 1;
constexpr uint32_t OPCODE_MASK = 0xffc00000;
constexpr uint32_t ADD_IMMEDIATE = 0x91000000;
constexpr uint32_t LOAD_DOUBLEWORD = 0xf9400000;
constexpr uint32_t LOAD_WORD = 0xb9400000;
constexpr uint32_t LOAD_PAIR_DOUBLEWORD = 0xa9400000;
constexpr uint32_t MOVE_WIDE_WORD_MASK = 0xffe00000;
constexpr uint32_t MOVE_WIDE_WORD = 0x52800000;
constexpr uint32_t SIGNED_MULTIPLY_ADD_LONG_MASK = 0xffe08000;
constexpr uint32_t SIGNED_MULTIPLY_ADD_LONG = 0x9b200000;
constexpr uint32_t BRANCH_IMMEDIATE_MASK = 0x7c000000;
constexpr uint32_t BRANCH_IMMEDIATE = 0x14000000;
constexpr uint32_t BRANCH_LINK_MASK = 0xfc000000;
constexpr uint32_t BRANCH_LINK = 0x94000000;
constexpr uint32_t BRANCH_CONDITIONAL_MASK = 0xff000010;
constexpr uint32_t BRANCH_CONDITIONAL = 0x54000000;
constexpr uint32_t COMPARE_BRANCH_MASK = 0x7e000000;
constexpr uint32_t COMPARE_BRANCH = 0x34000000;
constexpr uint32_t TEST_BRANCH = 0x36000000;
constexpr uint32_t BRANCH_REGISTER_MASK = 0xfe000000;
constexpr uint32_t BRANCH_REGISTER = 0xd6000000;
constexpr unsigned BRANCH_REGISTER_OPCODE_SHIFT = 21;
constexpr uint32_t BRANCH_REGISTER_OPCODE_MASK = 0xf;
constexpr uint32_t BRANCH_LINK_REGISTER_OPCODE = 1;
constexpr uint32_t UNCONDITIONAL_BRANCH_MASK = 0xfc000000;
constexpr uint32_t UNCONDITIONAL_BRANCH = 0x14000000;
constexpr unsigned BRANCH_OFFSET_BITS = 26;
constexpr unsigned CONDITIONAL_OFFSET_SHIFT = 5;
constexpr unsigned CONDITIONAL_OFFSET_BITS = 19;
constexpr unsigned TEST_OFFSET_BITS = 14;
constexpr uint32_t MOVE_REGISTER_MASK = 0xffe0ffe0;
constexpr uint32_t MOVE_REGISTER = 0xaa0003e0;
constexpr uint32_t EXCEPTION_MASK = 0xff000000;
constexpr uint32_t EXCEPTION = 0xd4000000;
constexpr uint32_t SYSTEM_MASK = 0xffc00000;
constexpr uint32_t SYSTEM = 0xd5000000;
constexpr uint32_t SYSTEM_READ_BIT = uint32_t{1} << 21;
constexpr uint32_t LOAD_STORE_MASK = 0x0a000000;
constexpr uint32_t LOAD_STORE = 0x08000000;
constexpr uint32_t LOAD_STORE_CLASS_MASK = 0x3a000000;
constexpr uint32_t LOAD_STORE_PAIR = 0x28000000;
constexpr uint32_t LOAD_STORE_REGISTER = 0x38000000;
constexpr uint32_t LOAD_LITERAL_MASK = 0x3b000000;
constexpr uint32_t LOAD_LITERAL = 0x18000000;
constexpr uint32_t LOAD_BIT = uint32_t{1} << 22;
constexpr uint32_t PAIR_WRITEBACK_BIT = uint32_t{1} << 23;
constexpr uint32_t UNSIGNED_OFFSET_BIT = uint32_t{1} << 24;
constexpr uint32_t REGISTER_OFFSET_BIT = uint32_t{1} << 21;
constexpr uint32_t INDEXED_BIT = uint32_t{1} << 10;
constexpr uint32_t SECOND_INDEX_BIT = uint32_t{1} << 11;
constexpr unsigned LOAD_OPCODE_SHIFT = 22;
constexpr uint32_t LOAD_OPCODE_MASK = 3;

constexpr uint32_t destination(uint32_t instruction) {
    return instruction & REGISTER_MASK;
}

constexpr uint32_t base(uint32_t instruction) {
    return (instruction >> BASE_REGISTER_SHIFT) & REGISTER_MASK;
}

constexpr uint32_t secondTransfer(uint32_t instruction) {
    return (instruction >> SECOND_TRANSFER_SHIFT) & REGISTER_MASK;
}

constexpr uint32_t secondSource(uint32_t instruction) {
    return (instruction >> SECOND_SOURCE_SHIFT) & REGISTER_MASK;
}

constexpr uint32_t immediate12(uint32_t instruction) {
    return (instruction >> IMMEDIATE12_SHIFT) & IMMEDIATE12_MASK;
}

constexpr bool isAdrp(uint32_t instruction) {
    return (instruction & ADRP_MASK) == ADRP;
}

inline uintptr_t adrpPage(uint32_t instruction, uintptr_t pc) {
    const uint64_t immediate = (static_cast<uint64_t>((instruction >> ADRP_HIGH_SHIFT) & ADRP_HIGH_MASK)
            << ADRP_LOW_BITS) | ((instruction >> ADRP_LOW_SHIFT) & ADRP_LOW_MASK);
    const uint64_t signBit = uint64_t{1} << (ADRP_IMMEDIATE_BITS - 1);
    const int64_t pages = static_cast<int64_t>((immediate ^ signBit) - signBit);
    return (pc & ~PAGE_OFFSET_MASK) + static_cast<uintptr_t>(pages * (int64_t{1} << PAGE_SHIFT));
}

constexpr bool isAddImmediate(uint32_t instruction) {
    return (instruction & OPCODE_MASK) == ADD_IMMEDIATE;
}

constexpr bool isLoadDoubleword(uint32_t instruction) {
    return (instruction & OPCODE_MASK) == LOAD_DOUBLEWORD;
}

constexpr bool isLoadWord(uint32_t instruction) {
    return (instruction & OPCODE_MASK) == LOAD_WORD;
}

constexpr bool isLoadPairAtBase(uint32_t instruction) {
    return (instruction & OPCODE_MASK) == LOAD_PAIR_DOUBLEWORD
            && ((instruction >> PAIR_IMMEDIATE_SHIFT) & PAIR_IMMEDIATE_MASK) == 0;
}

constexpr bool isMoveWideWord(uint32_t instruction) {
    return (instruction & MOVE_WIDE_WORD_MASK) == MOVE_WIDE_WORD;
}

constexpr uint32_t moveImmediate(uint32_t instruction) {
    return (instruction >> MOVE_IMMEDIATE_SHIFT) & MOVE_IMMEDIATE_MASK;
}

constexpr bool isSignedMultiplyAddLong(uint32_t instruction) {
    return (instruction & SIGNED_MULTIPLY_ADD_LONG_MASK) == SIGNED_MULTIPLY_ADD_LONG;
}

constexpr bool isCall(uint32_t instruction) {
    return (instruction & BRANCH_LINK_MASK) == BRANCH_LINK
            || ((instruction & BRANCH_REGISTER_MASK) == BRANCH_REGISTER
                    && ((instruction >> BRANCH_REGISTER_OPCODE_SHIFT) & BRANCH_REGISTER_OPCODE_MASK)
                            == BRANCH_LINK_REGISTER_OPCODE);
}

constexpr bool isBranch(uint32_t instruction) {
    return (instruction & BRANCH_IMMEDIATE_MASK) == BRANCH_IMMEDIATE
            || (instruction & BRANCH_CONDITIONAL_MASK) == BRANCH_CONDITIONAL
            || (instruction & COMPARE_BRANCH_MASK) == COMPARE_BRANCH
            || (instruction & COMPARE_BRANCH_MASK) == TEST_BRANCH
            || (instruction & BRANCH_REGISTER_MASK) == BRANCH_REGISTER;
}

constexpr bool isConditionalBranch(uint32_t instruction) {
    return (instruction & BRANCH_CONDITIONAL_MASK) == BRANCH_CONDITIONAL
            || (instruction & COMPARE_BRANCH_MASK) == COMPARE_BRANCH
            || (instruction & COMPARE_BRANCH_MASK) == TEST_BRANCH;
}

constexpr bool fallsThrough(uint32_t instruction) {
    return !isBranch(instruction) || isCall(instruction) || isConditionalBranch(instruction);
}

constexpr int64_t signedField(uint32_t instruction, unsigned shift, unsigned bits) {
    const uint64_t field = (instruction >> shift) & ((uint64_t{1} << bits) - 1);
    const uint64_t signBit = uint64_t{1} << (bits - 1);
    return static_cast<int64_t>((field ^ signBit) - signBit);
}

inline bool branchTarget(uint32_t instruction, uintptr_t pc, uintptr_t &target) {
    int64_t offset = 0;
    if ((instruction & UNCONDITIONAL_BRANCH_MASK) == UNCONDITIONAL_BRANCH) {
        offset = signedField(instruction, 0, BRANCH_OFFSET_BITS);
    } else if ((instruction & BRANCH_CONDITIONAL_MASK) == BRANCH_CONDITIONAL
            || (instruction & COMPARE_BRANCH_MASK) == COMPARE_BRANCH) {
        offset = signedField(instruction, CONDITIONAL_OFFSET_SHIFT, CONDITIONAL_OFFSET_BITS);
    } else if ((instruction & COMPARE_BRANCH_MASK) == TEST_BRANCH) {
        offset = signedField(instruction, CONDITIONAL_OFFSET_SHIFT, TEST_OFFSET_BITS);
    } else {
        return false;
    }
    target = pc + static_cast<uintptr_t>(offset * static_cast<int64_t>(INSTRUCTION_SIZE));
    return true;
}

constexpr bool isMoveRegister(uint32_t instruction) {
    return (instruction & MOVE_REGISTER_MASK) == MOVE_REGISTER;
}

constexpr bool isLoadStore(uint32_t instruction) {
    return (instruction & LOAD_STORE_MASK) == LOAD_STORE;
}

constexpr bool loadStoreWrites(uint32_t instruction, uint32_t reg) {
    const uint32_t transfer = destination(instruction);
    if ((instruction & LOAD_STORE_CLASS_MASK) == LOAD_STORE_PAIR) {
        const bool load = (instruction & LOAD_BIT) != 0;
        const bool writeback = (instruction & PAIR_WRITEBACK_BIT) != 0;
        return (load && (transfer == reg || secondTransfer(instruction) == reg))
                || (writeback && base(instruction) == reg);
    }
    if ((instruction & LOAD_STORE_CLASS_MASK) == LOAD_STORE_REGISTER) {
        const bool unsignedOffset = (instruction & UNSIGNED_OFFSET_BIT) != 0;
        const bool registerForm = (instruction & REGISTER_OFFSET_BIT) != 0;
        const bool atomic = !unsignedOffset && registerForm
                && (instruction & (INDEXED_BIT | SECOND_INDEX_BIT)) == 0;
        const bool authenticated = !unsignedOffset && registerForm && (instruction & INDEXED_BIT) != 0;
        if (authenticated) return transfer == reg || base(instruction) == reg;
        const bool writeback = !unsignedOffset && !registerForm && (instruction & INDEXED_BIT) != 0;
        const bool load = ((instruction >> LOAD_OPCODE_SHIFT) & LOAD_OPCODE_MASK) != 0;
        return ((load || atomic) && transfer == reg) || (writeback && base(instruction) == reg);
    }
    if ((instruction & LOAD_LITERAL_MASK) == LOAD_LITERAL) return transfer == reg;
    return transfer == reg || secondTransfer(instruction) == reg || secondSource(instruction) == reg;
}

constexpr bool writesRegister(uint32_t instruction, uint32_t reg) {
    if (isCall(instruction)) return reg <= LAST_CALLER_SAVED_REGISTER || reg == LINK_REGISTER;
    if (isBranch(instruction) || (instruction & EXCEPTION_MASK) == EXCEPTION) return false;
    if ((instruction & SYSTEM_MASK) == SYSTEM) {
        return (instruction & SYSTEM_READ_BIT) != 0 && destination(instruction) == reg;
    }
    if (isLoadStore(instruction)) return loadStoreWrites(instruction, reg);
    return destination(instruction) == reg;
}

constexpr bool usesAsBase(uint32_t instruction, uint32_t reg) {
    return (isLoadStore(instruction) || isAddImmediate(instruction)) && base(instruction) == reg;
}

}
