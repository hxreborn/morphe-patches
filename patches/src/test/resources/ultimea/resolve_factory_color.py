# Copyright (C) 2026 hxreborn
# SPDX-License-Identifier: GPL-3.0-only
import json
import struct
import sys
from pathlib import Path
from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM
from unicorn.arm64_const import UC_ARM64_REG_PC, UC_ARM64_REG_SP, UC_ARM64_REG_X0, UC_ARM64_REG_X15, UC_ARM64_REG_X29

library = Path(sys.argv[1]).read_bytes()
configuration = json.loads(Path(sys.argv[2]).read_text())
base, stack = 0x10000000, 0x20000000
machine = Uc(UC_ARCH_ARM64, UC_MODE_ARM)
page = 65536
identity, _, _, _, _, program_offset, _, _, _, program_entry_bytes, program_count, _, _, _ = struct.unpack_from(
    '<16sHHIQQQIHHHHHH', library)
assert identity[:6] == b'\x7fELF\x02\x01'
segments = []
for index in range(program_count):
    kind, _, offset, address, _, file_size, memory_size, _ = struct.unpack_from(
        '<IIQQQQQQ', library, program_offset + index * program_entry_bytes)
    if kind == 1 and memory_size:
        assert file_size <= memory_size and offset + file_size <= len(library)
        segments.append((address, offset, file_size, memory_size))
assert segments
ranges = []
for address, offset, file_size, memory_size in sorted(segments):
    start = (base + address) // page * page
    end = (base + address + memory_size + page - 1) // page * page
    if ranges and start <= ranges[-1][1]:
        ranges[-1] = (ranges[-1][0], max(end, ranges[-1][1]))
    else:
        ranges.append((start, end))
for start, end in ranges:
    machine.mem_map(start, end - start)
for address, offset, file_size, memory_size in segments:
    if file_size:
        machine.mem_write(base + address, library[offset:offset + file_size])
    if memory_size > file_size:
        assert bytes(machine.mem_read(base + address + memory_size - 1, 1)) == b'\0'
machine.mem_map(stack, page)
frame = stack + page // 2
machine.reg_write(UC_ARM64_REG_SP, stack + page // 4)
machine.reg_write(UC_ARM64_REG_X29, frame)
machine.reg_write(UC_ARM64_REG_X15, frame - 8)
machine.reg_write(UC_ARM64_REG_X0, 0xff191919)
callers = configuration['factories'] + [configuration['ink']]
for index, caller in enumerate(callers):
    position = frame + index * 256
    machine.mem_write(position, struct.pack('<QQ', position + 256, base + caller))
machine.mem_write(base + configuration['cache'], struct.pack('<II', 2, 0))
site = base + configuration['site']
machine.emu_start(site, site + 4, count=1000000)
assert machine.reg_read(UC_ARM64_REG_PC) == site + 4, hex(machine.reg_read(UC_ARM64_REG_PC))
assert machine.reg_read(UC_ARM64_REG_X0) == 0xffececee, hex(machine.reg_read(UC_ARM64_REG_X0))
assert machine.reg_read(UC_ARM64_REG_X15) == frame - 8
print('PASS three factory frames resolve outer INK from 0xff191919 to 0xffececee')
