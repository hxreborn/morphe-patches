/*
 * Copyright (C) 2026 hxreborn
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
 */
#define IMAGE_MIN_SIZE 0x39000
#define ANCHOR_A_OFFSET 0x16448
#define ANCHOR_A_FIRST 0xd10143ff
#define ANCHOR_A_SECOND 0xa9017bfd
#define ANCHOR_B_OFFSET 0x1605c
#define ANCHOR_B_WORD 0x3947b908

#define HX_PROFILE_PATCHES { \
    {0x293dc, 0x97ff5de5, MOV_W0_0, "component factory caller check"}, \
    {0x2c24c, 0x97ff5249, MOV_W0_0, "component factory caller check"}, \
    {0x2db98, 0x97ff4bf6, MOV_W0_0, "component factory caller check"}, \
    {0x2dc18, 0x97ff4bd6, MOV_W0_0, "component factory caller check"}, \
    {0x16834, 0x94001bf1, MOV_W0_0, "debugger check"}, \
    {0x10cec, 0x54ffdb60, 0x17fffedb, "added dex check"}, \
    {0x10ca8, 0x350003e0, NOP, "archive digest check"}, \
}
