/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.fix.signature

import app.morphe.patches.shared.misc.AbiTarget
import app.morphe.patches.shared.misc.NativeCheck

internal object NetworkFlowNativeCheckTarget {
    val ARM64 = AbiTarget(
        "lib/arm64-v8a/libengine.so",
        listOf(
            NativeCheck(
                "selfHashVerdict",
                "68 02 00 b4 08 01 00 90",
                "13 00 00 14 08 01 00 90",
            ),
            NativeCheck(
                "signingBlockCertificate",
                "ff 83 02 d1 fd 7b 04 a9 fc 6f 05 a9 fa 67 06 a9 f8 5f 07 a9 " +
                    "f6 57 08 a9 f4 4f 09 a9 fd 03 01 91 55 d0 3b d5",
                "00 00 80 52 c0 03 5f d6 fc 6f 05 a9 fa 67 06 a9 f8 5f 07 a9 " +
                    "f6 57 08 a9 f4 4f 09 a9 fd 03 01 91 55 d0 3b d5",
            ),
            NativeCheck(
                "trafficStatsSelfHashVerdict",
                "68 00 00 b4 80 6e 8c 52",
                "03 00 00 14 80 6e 8c 52",
            ),
            NativeCheck(
                "versionSelfHashVerdict",
                "d3 00 00 34 60 6e 8c 52",
                "06 00 00 14 60 6e 8c 52",
            ),
            NativeCheck(
                "versionCode",
                "08 01 00 90 09 e2 84 52",
                "c0 0e 80 52 c0 03 5f d6",
            ),
        ),
    )
}
