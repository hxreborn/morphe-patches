/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.shared

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.patch.PatchException

private const val SOURCE_INFORMATION_PREFIX = "C("
private const val SOURCE_PARAMETERS_PREFIX = "N("
private const val LAMBDA_SOURCE_PREFIX = "C"

internal fun composable(name: String, sourceFile: String? = null) = InstructionFilter { _, instruction ->
    val source = instruction.decryptedString()
    source != null && source.startsWith("$SOURCE_INFORMATION_PREFIX$name)") &&
        (sourceFile == null || source.contains(":$sourceFile#"))
}

internal fun composableLambda(sourceFile: String) = InstructionFilter { _, instruction ->
    val source = instruction.decryptedString()
    source != null && source.startsWith(LAMBDA_SOURCE_PREFIX) && source.getOrNull(LAMBDA_SOURCE_PREFIX.length)?.isDigit() == true &&
        source.contains(":$sourceFile#")
}

internal fun Match.composableParameters(): List<String> {
    val source = instructionMatches.first().instruction.decryptedString()
        ?.takeIf { it.startsWith(SOURCE_INFORMATION_PREFIX) }
        ?: throw PatchException("The first match in $originalMethod is not composable source information")
    val parameters = source.substringAfter(')', "")
    if (!parameters.startsWith(SOURCE_PARAMETERS_PREFIX)) return emptyList()
    return parameters.removePrefix(SOURCE_PARAMETERS_PREFIX).substringBefore(')').split(',')
        .map { it.substringBefore(':') }
}
