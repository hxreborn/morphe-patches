/*
 * SPDX-FileCopyrightText: 2026 hoo-dles
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/d3c02d333b9477ec50039093fc3b3641e3f1cbd0
 * Commit d3c02d333b9477ec50039093fc3b3641e3f1cbd0 (2026-07-24),
 * patches/src/main/kotlin/hoodles/morphe/patches/shared/misc/pairip/Fingerprints.kt
 */
package app.morphe.patches.allinonecalculator.misc.pairip

import app.morphe.patcher.Fingerprint

internal object VMRunnerStaticInitializerFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "<clinit>",
)

internal object VMRunnerInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/VMRunner;",
    name = "invoke",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
)
