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
    {0x11f0, 0xd10183ff, MOV_W0_0, "component factory caller check"}, \
    {0x11f4, 0xa9027bfd, RET, "component factory caller check"}, \
    {0x1db68, 0xd103c3ff, MOV_W0_0, "debugger check"}, \
    {0x1db6c, 0xa90a7bfd, RET, "debugger check"}, \
    {0x10fc4, 0x54ffdb60, 0x17fffedb, "added dex check"}, \
    {0x10f80, 0x350003e0, NOP, "archive digest check"}, \
    {0x174d4, 0xa9ba7bfd, MOV_W0_0, "watchdog integrity check"}, \
    {0x174d8, 0xa9016ffc, RET, "watchdog integrity check"}, \
    {0x11708, 0xd102c3ff, MOV_W0_1, "structure integrity check"}, \
    {0x1170c, 0xa9057bfd, RET, "structure integrity check"}, \
}
