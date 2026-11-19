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

#include "Arm64.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <utility>
#include <vector>

namespace {

using arm64::INSTRUCTION_SIZE;

constexpr size_t MAX_SEGMENTS = 8;
constexpr size_t PAIR_WINDOW = 8;
constexpr size_t GLOBAL_WINDOW = 8;
constexpr size_t TABLE_WINDOW = 64;
constexpr size_t ENTRY_SCAN_WINDOW = 0x4000;
constexpr size_t REGISTERS = 32;
constexpr size_t VECTOR_BYTES = 2 * sizeof(uintptr_t);
constexpr size_t FIELD_ALIGNMENT = sizeof(int32_t);

enum class Access {
    READ,
    READ_WRITE,
    READ_EXECUTE,
    EXECUTE,
};

struct Segment {
    uintptr_t start;
    uintptr_t end;
    bool readable;
    bool writable;
    bool executable;

    bool permits(Access access) const {
        switch (access) {
            case Access::READ:
                return readable;
            case Access::READ_WRITE:
                return readable && writable;
            case Access::READ_EXECUTE:
                return readable && executable;
            case Access::EXECUTE:
                return executable;
        }
        return false;
    }
};

struct TableLayout {
    uintptr_t vector;
    size_t stride;
    size_t fieldOffset;
};

struct Writer {
    uintptr_t pc;
    uint32_t instruction;
};

struct ControlFlow {
    uintptr_t start = 0;
    uintptr_t end = 0;
    std::vector<std::pair<uintptr_t, uintptr_t>> edges;
    std::vector<uintptr_t> outsideEntries;

    bool enteredFromOutside(uintptr_t pc) const {
        for (uintptr_t entry : outsideEntries) {
            if (entry == pc) return true;
        }
        return false;
    }

    bool enteredBetween(uintptr_t after, uintptr_t last) const {
        for (const auto &edge : edges) {
            if (edge.second > after && edge.second <= last) return true;
        }
        for (uintptr_t entry : outsideEntries) {
            if (entry > after && entry <= last) return true;
        }
        return false;
    }
};

struct Image {
    std::array<Segment, MAX_SEGMENTS> segments = {};
    size_t count = 0;

    bool add(Segment segment) {
        if (count == segments.size() || segment.end <= segment.start) return false;
        segments[count++] = segment;
        return true;
    }

    bool contains(uintptr_t address, size_t size, Access access) const {
        if (size == 0) return false;
        for (size_t index = 0; index < count; index++) {
            const auto &segment = segments[index];
            if (segment.permits(access) && address >= segment.start && address < segment.end && size <= segment.end - address) {
                return true;
            }
        }
        return false;
    }

    bool readWord(uintptr_t address, uintptr_t &value) const {
        if (address % sizeof(uintptr_t) != 0 || !contains(address, sizeof(value), Access::READ)) return false;
        std::memcpy(&value, reinterpret_cast<const void *>(address), sizeof(value));
        return true;
    }

    bool readInstruction(uintptr_t pc, uint32_t &value) const {
        if (pc % INSTRUCTION_SIZE != 0 || !contains(pc, INSTRUCTION_SIZE, Access::READ_EXECUTE)) return false;
        std::memcpy(&value, reinterpret_cast<const void *>(pc), sizeof(value));
        return true;
    }

    bool isCode(uintptr_t address) const {
        return address % INSTRUCTION_SIZE == 0 && contains(address, INSTRUCTION_SIZE, Access::EXECUTE);
    }

    uintptr_t uniqueString(const char *text) const {
        const size_t length = std::strlen(text) + 1;
        uintptr_t found = 0;
        size_t matches = 0;
        for (size_t index = 0; index < count; index++) {
            const auto &segment = segments[index];
            if (!segment.readable || segment.writable) continue;
            const char *begin = reinterpret_cast<const char *>(segment.start);
            const char *end = reinterpret_cast<const char *>(segment.end);
            const char *cursor = begin;
            while (static_cast<size_t>(end - cursor) >= length) {
                const void *hit = memmem(cursor, static_cast<size_t>(end - cursor), text, length);
                if (hit == nullptr) break;
                const char *at = static_cast<const char *>(hit);
                if (at == begin || at[-1] == '\0') {
                    found = reinterpret_cast<uintptr_t>(at);
                    matches++;
                }
                cursor = at + 1;
            }
        }
        return matches == 1 ? found : 0;
    }

    template <typename Visit>
    void forEachWordEqualTo(uintptr_t value, Visit visit) const {
        for (size_t index = 0; index < count; index++) {
            const auto &segment = segments[index];
            if (!segment.permits(Access::READ_WRITE)) continue;
            const uintptr_t first = (segment.start + sizeof(uintptr_t) - 1) & ~(sizeof(uintptr_t) - 1);
            for (uintptr_t slot = first; slot < segment.end && segment.end - slot >= sizeof(uintptr_t);
                    slot += sizeof(uintptr_t)) {
                uintptr_t candidate = 0;
                if (readWord(slot, candidate) && candidate == value) visit(slot);
            }
        }
    }

    uintptr_t vtable(const char *typeName) const {
        const uintptr_t name = uniqueString(typeName);
        if (name == 0) return 0;
        uintptr_t typeInfo = 0;
        size_t typeInfos = 0;
        forEachWordEqualTo(name, [&](uintptr_t slot) {
            uintptr_t typeInfoVtable = 0;
            if (slot >= sizeof(uintptr_t) && readWord(slot - sizeof(uintptr_t), typeInfoVtable) && typeInfoVtable != 0) {
                typeInfo = slot - sizeof(uintptr_t);
            }
            typeInfos++;
        });
        if (typeInfos != 1 || typeInfo == 0) return 0;
        uintptr_t found = 0;
        size_t vtables = 0;
        forEachWordEqualTo(typeInfo, [&](uintptr_t slot) {
            uintptr_t offsetToTop = 1;
            uintptr_t firstMethod = 0;
            if (slot >= sizeof(uintptr_t) && readWord(slot - sizeof(uintptr_t), offsetToTop) && offsetToTop == 0
                    && readWord(slot + sizeof(uintptr_t), firstMethod) && isCode(firstMethod)) {
                found = slot + sizeof(uintptr_t);
            }
            vtables++;
        });
        return vtables == 1 ? found : 0;
    }

    bool virtualMethod(uintptr_t vtable, size_t slot, uintptr_t &method) const {
        for (size_t index = 0; index <= slot; index++) {
            if (!readWord(vtable + index * sizeof(uintptr_t), method) || !isCode(method)) return false;
        }
        return true;
    }

    bool pairedAddress(uintptr_t pc, uintptr_t &address, uint32_t &destination) const {
        uint32_t page = 0;
        if (!readInstruction(pc, page) || !arm64::isAdrp(page)) return false;
        const uint32_t reg = arm64::destination(page);
        if (reg == arm64::ZERO_REGISTER) return false;
        for (size_t step = 1; step <= PAIR_WINDOW; step++) {
            uint32_t candidate = 0;
            if (!readInstruction(pc + step * INSTRUCTION_SIZE, candidate)) return false;
            if (arm64::isAddImmediate(candidate) && arm64::base(candidate) == reg) {
                destination = arm64::destination(candidate);
                address = arm64::adrpPage(page, pc) + arm64::immediate12(candidate);
                return destination != arm64::ZERO_REGISTER;
            }
            if (arm64::isBranch(candidate) || arm64::writesRegister(candidate, reg)) return false;
        }
        return false;
    }

    template <typename Visit>
    void forEachAddressReference(uintptr_t target, Visit visit) const {
        for (size_t index = 0; index < count; index++) {
            const auto &segment = segments[index];
            if (!segment.executable) continue;
            for (uintptr_t pc = segment.start; pc < segment.end && segment.end - pc >= INSTRUCTION_SIZE;
                    pc += INSTRUCTION_SIZE) {
                uint32_t candidate = 0;
                if (!readInstruction(pc, candidate) || !arm64::isAdrp(candidate)
                        || arm64::adrpPage(candidate, pc) != (target & ~arm64::PAGE_OFFSET_MASK)) continue;
                uintptr_t address = 0;
                uint32_t destination = 0;
                if (pairedAddress(pc, address, destination) && address == target) visit(pc);
            }
        }
    }

    uintptr_t uniqueAddressReference(uintptr_t target) const {
        uintptr_t found = 0;
        size_t references = 0;
        forEachAddressReference(target, [&](uintptr_t pc) {
            found = pc;
            references++;
        });
        return references == 1 ? found : 0;
    }

    bool loadedGlobal(uintptr_t pc, uintptr_t &global) const {
        uint32_t page = 0;
        if (!readInstruction(pc, page) || !arm64::isAdrp(page)) return false;
        const uint32_t reg = arm64::destination(page);
        if (reg == arm64::ZERO_REGISTER) return false;
        for (size_t step = 1; step <= PAIR_WINDOW; step++) {
            uint32_t candidate = 0;
            if (!readInstruction(pc + step * INSTRUCTION_SIZE, candidate)) return false;
            if (arm64::usesAsBase(candidate, reg)) {
                if (!arm64::isLoadDoubleword(candidate)) return false;
                global = arm64::adrpPage(page, pc) + arm64::immediate12(candidate) * sizeof(uintptr_t);
                return contains(global, sizeof(uintptr_t), Access::READ_WRITE);
            }
            if (arm64::isBranch(candidate) || arm64::writesRegister(candidate, reg)) return false;
        }
        return false;
    }

    uintptr_t globalLoadedNear(uintptr_t reference) const {
        const uintptr_t span = GLOBAL_WINDOW * INSTRUCTION_SIZE;
        if (reference < span || !isCode(reference)) return 0;
        uintptr_t found = 0;
        size_t globals = 0;
        for (uintptr_t pc = reference - span; pc <= reference + span; pc += INSTRUCTION_SIZE) {
            uintptr_t global = 0;
            if (loadedGlobal(pc, global)) {
                found = global;
                globals++;
            }
        }
        return globals == 1 ? found : 0;
    }

    bool controlFlowAround(uintptr_t reference, ControlFlow &flow) const {
        const uintptr_t span = TABLE_WINDOW * INSTRUCTION_SIZE;
        const uintptr_t scan = ENTRY_SCAN_WINDOW * INSTRUCTION_SIZE;
        if (reference < span + scan || !isCode(reference - span) || !isCode(reference + span - INSTRUCTION_SIZE)) {
            return false;
        }
        flow.start = reference - span;
        flow.end = reference + span;
        for (uintptr_t pc = flow.start - scan; pc < flow.end + scan; pc += INSTRUCTION_SIZE) {
            uint32_t instruction = 0;
            uintptr_t target = 0;
            if (!readInstruction(pc, instruction) || !arm64::branchTarget(instruction, pc, target)
                    || target < flow.start || target >= flow.end) continue;
            if (pc >= flow.start && pc < flow.end) {
                flow.edges.push_back({pc, target});
            } else {
                flow.outsideEntries.push_back(target);
            }
        }
        return true;
    }

    bool reachingWriters(const ControlFlow &flow, uintptr_t use, uint32_t reg, bool followCopies,
            std::vector<Writer> &writers) const {
        std::vector<bool> visited((flow.end - flow.start) / INSTRUCTION_SIZE * REGISTERS);
        std::vector<std::pair<uintptr_t, uint32_t>> pending = {{use, reg}};
        while (!pending.empty()) {
            const uintptr_t at = pending.back().first;
            const uint32_t tracked = pending.back().second;
            pending.pop_back();
            if (at <= flow.start || at >= flow.end || flow.enteredFromOutside(at)) return false;
            const size_t state = (at - flow.start) / INSTRUCTION_SIZE * REGISTERS + tracked;
            if (visited[state]) continue;
            visited[state] = true;
            std::vector<uintptr_t> predecessors;
            uint32_t previous = 0;
            if (!readInstruction(at - INSTRUCTION_SIZE, previous)) return false;
            if (arm64::fallsThrough(previous)) predecessors.push_back(at - INSTRUCTION_SIZE);
            for (const auto &edge : flow.edges) {
                if (edge.second == at) predecessors.push_back(edge.first);
            }
            if (predecessors.empty()) return false;
            for (uintptr_t pc : predecessors) {
                uint32_t instruction = 0;
                if (!readInstruction(pc, instruction)) return false;
                if (!arm64::writesRegister(instruction, tracked)) {
                    pending.push_back({pc, tracked});
                } else if (followCopies && arm64::isMoveRegister(instruction)
                        && arm64::secondSource(instruction) != arm64::ZERO_REGISTER) {
                    pending.push_back({pc, arm64::secondSource(instruction)});
                } else {
                    writers.push_back({pc, instruction});
                }
            }
        }
        return !writers.empty();
    }

    bool uniqueWriter(const ControlFlow &flow, uintptr_t use, uint32_t reg, bool followCopies, Writer &writer) const {
        std::vector<Writer> writers;
        if (!reachingWriters(flow, use, reg, followCopies, writers)) return false;
        for (const Writer &candidate : writers) {
            if (candidate.pc != writers.front().pc) return false;
        }
        writer = writers.front();
        return true;
    }

    bool sameWrittenInstruction(const ControlFlow &flow, uintptr_t use, uint32_t reg, uint32_t &instruction) const {
        std::vector<Writer> writers;
        if (!reachingWriters(flow, use, reg, false, writers)) return false;
        for (const Writer &candidate : writers) {
            if (candidate.instruction != writers.front().instruction) return false;
        }
        instruction = writers.front().instruction;
        return true;
    }

    bool vectorLoadedBefore(const ControlFlow &flow, uintptr_t load, uint32_t loadInstruction,
            uintptr_t &vector) const {
        const uint32_t address = arm64::base(loadInstruction);
        Writer add = {};
        if (address == arm64::ZERO_REGISTER || !uniqueWriter(flow, load, address, true, add)
                || !arm64::isAddImmediate(add.instruction) || arm64::base(add.instruction) == arm64::ZERO_REGISTER) {
            return false;
        }
        Writer page = {};
        if (!uniqueWriter(flow, add.pc, arm64::base(add.instruction), false, page) || !arm64::isAdrp(page.instruction)) {
            return false;
        }
        vector = arm64::adrpPage(page.instruction, page.pc) + arm64::immediate12(add.instruction);
        return contains(vector, VECTOR_BYTES, Access::READ_WRITE) && vector % sizeof(uintptr_t) == 0;
    }

    bool fieldLoadedAfter(const ControlFlow &flow, uintptr_t multiply, uintptr_t reference, uint32_t element,
            size_t &fieldOffset) const {
        for (uintptr_t pc = multiply + INSTRUCTION_SIZE; pc < reference; pc += INSTRUCTION_SIZE) {
            uint32_t candidate = 0;
            if (!readInstruction(pc, candidate)) return false;
            if (arm64::usesAsBase(candidate, element)) {
                if (!arm64::isLoadWord(candidate) || flow.enteredBetween(multiply, pc)) return false;
                fieldOffset = arm64::immediate12(candidate) * FIELD_ALIGNMENT;
                return true;
            }
            if (arm64::isBranch(candidate) || arm64::writesRegister(candidate, element)) return false;
        }
        return false;
    }

    bool tableLayoutAt(const ControlFlow &flow, uintptr_t multiply, uintptr_t reference, TableLayout &layout) const {
        uint32_t product = 0;
        if (!readInstruction(multiply, product) || !arm64::isSignedMultiplyAddLong(product)) return false;
        const uint32_t element = arm64::destination(product);
        const uint32_t strideRegister = arm64::secondSource(product);
        const uint32_t accumulator = arm64::secondTransfer(product);
        Writer load = {};
        if (!uniqueWriter(flow, multiply, accumulator, false, load) || !arm64::isLoadPairAtBase(load.instruction)
                || arm64::destination(load.instruction) != accumulator) return false;
        uint32_t move = 0;
        if (!sameWrittenInstruction(flow, multiply, strideRegister, move) || !arm64::isMoveWideWord(move)) return false;
        uintptr_t vector = 0;
        size_t fieldOffset = 0;
        if (!vectorLoadedBefore(flow, load.pc, load.instruction, vector)
                || !fieldLoadedAfter(flow, multiply, reference, element, fieldOffset)) return false;
        const size_t stride = arm64::moveImmediate(move);
        if (stride == 0 || stride % FIELD_ALIGNMENT != 0 || fieldOffset + sizeof(int32_t) > stride) return false;
        layout = {vector, stride, fieldOffset};
        return true;
    }

    bool tableLayoutBefore(uintptr_t reference, TableLayout &layout) const {
        ControlFlow flow;
        if (!isCode(reference) || !controlFlowAround(reference, flow)) return false;
        size_t layouts = 0;
        for (uintptr_t pc = flow.start; pc < reference; pc += INSTRUCTION_SIZE) {
            TableLayout candidate = {};
            if (tableLayoutAt(flow, pc, reference, candidate)) {
                layout = candidate;
                layouts++;
            }
        }
        return layouts == 1;
    }

    bool uniqueTableLayoutBefore(uintptr_t target, TableLayout &layout) const {
        size_t layouts = 0;
        forEachAddressReference(target, [&](uintptr_t pc) {
            TableLayout candidate = {};
            if (tableLayoutBefore(pc, candidate)) {
                layout = candidate;
                layouts++;
            }
        });
        return layouts == 1;
    }
};

}
