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
#define HX_ABI "armeabi-v7a"

#define IMAGE_MIN_SIZE 0x50000
#define ANCHOR_A_OFFSET 0x11db8
#define ANCHOR_A_FIRST 0xaf03b5f0
#define ANCHOR_A_SECOND 0x0b00e92d
#define ANCHOR_B_OFFSET 0x93cc
#define ANCHOR_B_WORD 0x01d8f8c1

#define HX_PROFILE_PATCHES { \
    {0x0de0, 0xaf03b5f0, THUMB_RETURN_0, "component factory caller check"}, \
    {0xcfa0, 0xff0af004, THUMB_MOVS_R0_0_NOP, "debugger check"}, \
    {0x93c8, 0x2024d099, 0x2024e799, "added dex check"}, \
    {0x9594, 0xfe74f014, THUMB_MOVS_R0_0_NOP, "archive digest check"}, \
    {0x918c, 0xfc94f000, THUMB_MOVS_R0_1_NOP, "structure integrity check"}, \
}
