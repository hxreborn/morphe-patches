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

#include "ProcessMemory.h"

#include <array>
#include <string>

namespace {

constexpr size_t MAX_CAPTURED_BYTES = 0x1000;
constexpr char FIRST_PRINTABLE = 0x20;
constexpr char LAST_PRINTABLE = 0x7e;
constexpr char NON_PRINTABLE_REPLACEMENT = ' ';

struct CapturedOutput {
    std::string text;
    bool rejected = false;

    void append(uintptr_t source, size_t size) {
        if (rejected || size == 0) return;
        std::array<char, MAX_CAPTURED_BYTES> copy;
        if (source == 0 || size > MAX_CAPTURED_BYTES - text.size()
                || !ProcessMemory().read(source, copy.data(), size)) {
            text.clear();
            rejected = true;
            return;
        }
        for (size_t offset = 0; offset < size; offset++) {
            const char character = copy[offset];
            text.push_back(character >= FIRST_PRINTABLE && character <= LAST_PRINTABLE ? character
                                                                                        : NON_PRINTABLE_REPLACEMENT);
        }
    }

    std::string take() {
        std::string taken;
        taken.swap(text);
        return taken;
    }
};

}
