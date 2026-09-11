/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
#define IMAGE_MIN_SIZE 0x39000
#define ANCHOR_A_OFFSET 0x16448
#define ANCHOR_A_FIRST 0xd10143ff
#define ANCHOR_A_SECOND 0xa9017bfd
#define ANCHOR_B_OFFSET 0x1605c
#define ANCHOR_B_WORD 0x3947b908

#define HX_PROFILE_PATCHES { \
    {0xb70, 0xd10143ff, MOV_W0_0, "component factory caller check"}, \
    {0xb74, 0xa9027bfd, RET, "component factory caller check"}, \
    {0x1d7f8, 0xd10383ff, MOV_W0_0, "debugger check"}, \
    {0x1d7fc, 0xa9097bfd, RET, "debugger check"}, \
    {0x10cec, 0x54ffdb60, 0x17fffedb, "added dex check"}, \
    {0x10ca8, 0x350003e0, NOP, "archive digest check"}, \
}
