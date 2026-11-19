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

#include <sys/uio.h>
#include <unistd.h>

#include <cinttypes>
#include <cstdio>
#include <cstdlib>

namespace {

constexpr char MAPS_PATH[] = "/proc/self/maps";
constexpr size_t PERMISSIONS_LENGTH = 4;
constexpr char READABLE = 'r';
constexpr char WRITABLE = 'w';

struct ProcessMemory {
    bool read(uintptr_t address, void *buffer, size_t size) const {
        iovec local = {buffer, size};
        iovec remote = {reinterpret_cast<void *>(address), size};
        return size != 0 && process_vm_readv(getpid(), &local, 1, &remote, 1, 0) == static_cast<ssize_t>(size);
    }

    size_t write(uintptr_t address, const void *buffer, size_t size) const {
        iovec local = {const_cast<void *>(buffer), size};
        iovec remote = {reinterpret_cast<void *>(address), size};
        const ssize_t written = process_vm_writev(getpid(), &local, 1, &remote, 1, 0);
        return written < 0 ? 0 : static_cast<size_t>(written);
    }

    bool writable(uintptr_t address, size_t size) const {
        if (size == 0 || address > UINTPTR_MAX - size) return false;
        FILE *maps = std::fopen(MAPS_PATH, "re");
        if (maps == nullptr) return false;
        char *line = nullptr;
        size_t capacity = 0;
        bool found = false;
        while (!found && getline(&line, &capacity, maps) > 0) {
            uintptr_t start = 0;
            uintptr_t end = 0;
            char permissions[PERMISSIONS_LENGTH + 1] = {};
            if (std::sscanf(line, "%" SCNxPTR "-%" SCNxPTR " %4s", &start, &end, permissions) != 3) continue;
            found = address >= start && address + size <= end && permissions[0] == READABLE
                    && permissions[1] == WRITABLE;
        }
        std::free(line);
        std::fclose(maps);
        return found;
    }
};

}
