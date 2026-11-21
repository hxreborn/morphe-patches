/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.fix.tamper

import app.morphe.patches.shared.misc.AbiTarget
import app.morphe.patches.shared.misc.NativeCheck

internal object LiappTamperResponseTarget {
    val ARM64 = AbiTarget(
        "lib/arm64-v8a/libxxezvw.so",
        listOf(
            NativeCheck(
                "killProcessRoutine",
                "ff 83 01 d1 f3 53 00 a9 54 03 00 d0 fe 13 00 f9 f5 5b 01 a9 f5 03 00 2a",
                "e0 03 1f 2a c0 03 5f d6 54 03 00 d0 fe 13 00 f9 f5 5b 01 a9 f5 03 00 2a",
            ),
            NativeCheck(
                "killSyscall",
                "e2 03 00 2a e3 03 01 2a e0 03 02 aa e1 03 03 aa 28 10 80 d2 01 00 00 d4 c0 03 5f d6",
                "3f 24 00 71 80 00 00 54 28 10 80 d2 01 00 00 d4 c0 03 5f d6 e0 03 1f 2a c0 03 5f d6",
            ),
            NativeCheck(
                "crashRoutine",
                "ff c3 01 d1 c1 03 00 b0 f5 5b 01 a9 d5 03 00 90 fe 1b 00 f9 f7 63 02 a9 f7 03 00 2a",
                "e0 03 1f 2a c0 03 5f d6 f5 5b 01 a9 d5 03 00 90 fe 1b 00 f9 f7 63 02 a9 f7 03 00 2a",
            ),
        ),
    )

    val ARM32 = AbiTarget(
        "lib/armeabi-v7a/libxxezvw.so",
        listOf(
            NativeCheck(
                "killProcessRoutine",
                "7c 22 9f e5 fa 0f 50 e3 f0 41 2d e9 28 d0 4d e2",
                "00 00 a0 e3 1e ff 2f e1 f0 41 2d e9 28 d0 4d e2",
            ),
            NativeCheck(
                "crashRoutineAndSigkillFilter",
                "f0 4f 2d e9 44 d0 4d e2 8c 53 9f e5 00 80 a0 e1 88 23 9f e5 " +
                    "33 40 a0 e3 05 50 9f e7 02 20 8f e0 00 30 95 e5",
                "00 00 a0 e3 1e ff 2f e1 09 00 51 e3 00 00 a0 03 1e ff 2f 01 " +
                    "04 c0 9f e5 0c c0 8f e0 00 f0 9c e5 a0 3e 09 00",
            ),
            NativeCheck(
                "killPltStub",
                "00 c6 8f e2 9d ca 8c e2 9c fb bc e5",
                "39 27 00 ea 9d ca 8c e2 9c fb bc e5",
            ),
        ),
    )

    val targets = listOf(ARM64, ARM32)
}
