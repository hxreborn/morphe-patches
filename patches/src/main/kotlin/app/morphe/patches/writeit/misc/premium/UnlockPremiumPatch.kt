/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.writeit.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.decode
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val ENTITLEMENT = "pro"
private const val PRODUCT = "writeit.pro"
private const val JAVASCRIPT_DIRECTORY = "assets/public/_nuxt"
private const val CAPACITOR_CONFIG = "assets/capacitor.config.json"

private const val IDENTIFIER = "[A-Za-z_$][A-Za-z0-9_$]*"

private val noteMutation = Regex(
    """saveNote:\(($IDENTIFIER),($IDENTIFIER)\)=>($IDENTIFIER)\.saveUserEntryNote\.mutate\(\{entryId:\1,note:\2\}\)""",
)

private val statusMutation = Regex(
    """saveStatus:\(($IDENTIFIER),($IDENTIFIER)\)=>($IDENTIFIER)\.saveUserEntry\.mutate\(\{entryId:\1,status:\2\}\)""",
)

private val userEntryStoreReference = Regex(
    """userEntryFor:($IDENTIFIER)=>($IDENTIFIER)\.byEntryId\[\1\]""",
)

private val backgroundUpdateStrategy = Regex(
    """("autoUpdateStrategy"\s*:\s*)"background"""",
)

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks every deck and language, custom reviews, entry notes and word status controls, and removes ads.",
) {
    compatibleWith(AppCompatibilities.WRITE_IT)
    dependsOn(sharedRevenuecatExtensionPatch, removePairipProtectionPatch)

    execute {
        val disableDeploy = DisableDeployFingerprint.match()
        val (_, deployDisabledConstant) = disableDeploy.instructionMatches
        val deployDisabledRegister = deployDisabledConstant.getInstruction<OneRegisterInstruction>().registerA
        disableDeploy.method.replaceInstruction(deployDisabledConstant.index, "const/4 v$deployDisabledRegister, 0x1")

        decode("$JAVASCRIPT_DIRECTORY/*.js")
        val capacitorConfig = resourceFile(CAPACITOR_CONFIG)
        capacitorConfig.writeText(disableLiveUpdates(capacitorConfig.readText(), capacitorConfig.name))
        val javascriptFiles = resourceFile(JAVASCRIPT_DIRECTORY).listFiles { file -> file.extension == "js" }
            ?: throw PatchException("Could not read the JavaScript bundle directory")
        val entryChunks = javascriptFiles.map { file -> file to file.readText() }.filter { (_, javascript) ->
            noteMutation.containsMatchIn(javascript) || statusMutation.containsMatchIn(javascript)
        }
        val (entryChunk, javascript) = entryChunks.singleOrNull()
            ?: throw PatchException("Expected one entry update chunk, found ${entryChunks.size}")
        entryChunk.writeText(enableLocalEntryUpdates(javascript, entryChunk.name))
        grantRevenueCatEntitlements(ENTITLEMENT to PRODUCT)
    }
}

internal fun enableLocalEntryUpdates(javascript: String, fileName: String): String {
    val storeMatch = userEntryStoreReference.uniqueMatch(javascript, fileName, "user entry store")
    val (_, storeIdentifier) = storeMatch.destructured
    val noteMatch = noteMutation.uniqueMatch(javascript, fileName, "note mutation")
    val (noteEntryIdentifier, noteIdentifier) = noteMatch.destructured
    val withNotes = javascript.replaceRange(
        noteMatch.range,
        localEntryUpdate("saveNote", "note", storeIdentifier, noteEntryIdentifier, noteIdentifier),
    )
    val statusMatch = statusMutation.uniqueMatch(withNotes, fileName, "status mutation")
    val (statusEntryIdentifier, statusIdentifier) = statusMatch.destructured
    return withNotes.replaceRange(
        statusMatch.range,
        localEntryUpdate("saveStatus", "status", storeIdentifier, statusEntryIdentifier, statusIdentifier),
    )
}

private fun localEntryUpdate(
    handler: String,
    field: String,
    store: String,
    entryIdentifier: String,
    valueIdentifier: String,
): String {
    if (store == entryIdentifier || store == valueIdentifier) {
        throw PatchException("User entry store $store is shadowed by a parameter of $handler")
    }
    val entry = "$store.byEntryId[$entryIdentifier]"
    return "$handler:($entryIdentifier,$valueIdentifier)=>Promise.resolve(" +
        "$store.loaded[$entry.id]={...$entry,$field:$valueIdentifier})"
}

internal fun disableLiveUpdates(config: String, fileName: String): String {
    val strategyMatch = backgroundUpdateStrategy.uniqueMatch(config, fileName, "live update strategy")
    val (strategyPrefix) = strategyMatch.destructured
    return config.replaceRange(strategyMatch.range, "$strategyPrefix\"none\"")
}

private fun Regex.uniqueMatch(source: String, fileName: String, label: String): MatchResult {
    val matches = findAll(source).toList()
    return matches.singleOrNull() ?: throw PatchException(
        "$fileName: $label found ${if (matches.isEmpty()) "no match" else "${matches.size} matches"}",
    )
}
