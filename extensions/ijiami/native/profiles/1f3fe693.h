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
#define ANCHOR_A_OFFSET 0x1218
#define ANCHOR_A_FIRST 0xf940aae8
#define ANCHOR_A_SECOND 0x394c4509
#define ANCHOR_B_OFFSET 0x1210
#define ANCHOR_B_WORD 0xf0000577

#define HX_PROFILE_PATCHES { \
    {0x29b24, 0x97ff5db3, MOV_W0_0, "component factory caller check"}, \
    {0x2c910, 0x97ff5238, MOV_W0_0, "component factory caller check"}, \
    {0x2e0e8, 0x97ff4c42, MOV_W0_0, "component factory caller check"}, \
    {0x1721c, 0x94001a53, MOV_W0_0, "debugger check"}, \
    {0x10fc4, 0x54ffdb60, 0x17fffedb, "added dex check"}, \
    {0x10f80, 0x350003e0, NOP, "archive digest check"}, \
    {0x163c4, 0x94000444, MOV_W0_0, "watchdog integrity check"}, \
    {0x174c0, 0x94000005, MOV_W0_0, "watchdog integrity check"}, \
    {0x1d9ec, 0x97ffe6ba, MOV_W0_0, "watchdog integrity check"}, \
    {0x10898, 0x9400039c, MOV_W0_1, "structure integrity check"}, \
}
