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
#define HX_ABI "arm64-v8a"

#define IMAGE_MIN_SIZE 0x40000
#define ANCHOR_A_OFFSET 0x11304
#define ANCHOR_A_FIRST 0x910103e0
#define ANCHOR_A_SECOND 0x9101c3e1
#define ANCHOR_B_OFFSET 0x10c70
#define ANCHOR_B_WORD 0x52802001

#define HX_PROFILE_PATCHES { \
    {0x318b4, 0x97ff3ff8, MOV_W0_0, "component factory caller check"}, \
    {0x34964, 0x97ff33cc, MOV_W0_0, "component factory caller check"}, \
    {0x36328, 0x97ff2d5b, MOV_W0_0, "component factory caller check"}, \
    {0x1f498, 0x94001a7d, MOV_W0_0, "debugger check"}, \
    {0x11358, 0x54ffdb60, 0x17fffedb, "added dex check"}, \
    {0x11314, 0x350003e0, NOP, "archive digest check"}, \
    {0x10c2c, 0x9400039c, MOV_W0_1, "structure integrity check"}, \
}
