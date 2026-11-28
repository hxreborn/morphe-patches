/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pepper.links

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod

private const val EXPECTED_SESSION_ID_APPEND_COUNT = 2

private val identifierQueryKeys = listOf("user_id", "device_id", "analytics_context", "recombee_id")

@Suppress("unused")
val removeDealLinkIdentifiersPatch = patch(
    name = "Remove deal link identifiers",
    description = "Removes user and device identifiers from deal links.",
) {
    compatibleWith(AppCompatibilities.CHOLLOMETRO, AppCompatibilities.PEPPER)

    execute {
        DealUrlIdentifiersFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0 }, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
                move-result-object p0
                return-object p0
            """,
        )

        DealSessionRedirectFingerprint.method.addInstructions(0, "return-object p1")

        val appendQueryParameterMethod = AppendQueryParameterFingerprint.method
        val valueUnlessIdentifier = mutableClassDefBy(appendQueryParameterMethod.definingClass).addStaticMethod(
            name = "valueUnlessIdentifier",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
            returnType = "Ljava/lang/String;",
            registers = 3,
            smali = identifierQueryKeys.joinToString("\n") { key ->
                """
                    const-string v0, "$key"
                    invoke-virtual { v0, p0 }, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-nez v0, :identifier
                """
            } + """
                return-object p1
                :identifier
                const/4 v0, 0x0
                return-object v0
            """,
        )
        appendQueryParameterMethod.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, $valueUnlessIdentifier
                move-result-object p2
            """,
        )

        DealSessionIdAppendFingerprint.matchAll(
            EXPECTED_SESSION_ID_APPEND_COUNT..EXPECTED_SESSION_ID_APPEND_COUNT,
        ).forEach { match ->
            val appendStart = match.instructionMatches.first().index
            val appendEnd = match.instructionMatches.last().index
            match.method.removeInstructions(appendStart, appendEnd - appendStart + 1)
        }
    }
}
