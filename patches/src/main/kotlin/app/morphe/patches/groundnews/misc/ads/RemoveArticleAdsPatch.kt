/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private lateinit var adHosts: String

private val articleAdHostsPatch = resourcePatch {
    execute {
        adHosts = get("assets/hosts").readLines().map(String::trim).filter(String::isNotEmpty)
            .onEach { require(it.all { character -> character.isLetterOrDigit() || character in ".-" }) }
            .distinct().joinToString("\\n")
        require(adHosts.isNotEmpty()) { "Article ad host list is empty" }
    }
}

@Suppress("unused")
val removeArticleAdsPatch = bytecodePatch(
    name = "Remove article ads",
    description = "Blocks advertising hosts in the in-app article browser.",
) {
    compatibleWith(AppCompatibilities.GROUND_NEWS)
    dependsOn(articleAdHostsPatch)

    execute {
        val initializer = AdBlockerInitializerFingerprint.matchSingle()
        val hosts = initializer.classDef.fields.single { it.type == "Ljava/util/Set;" }
        initializer.method.apply {
            val insertionIndex = implementation!!.instructions.lastIndex
            val registers = getFreeRegisterProvider(insertionIndex, 2)
            val hostsRegister = registers.getFreeRegister4Bit()
            val listRegister = registers.getFreeRegister4Bit()
            addInstructions(
                insertionIndex,
                """
                    const-string v$listRegister, "$adHosts"
                    const-string v$hostsRegister, "\n"
                    invoke-virtual {v$listRegister, v$hostsRegister}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;
                    move-result-object v$listRegister
                    invoke-static {v$listRegister}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;
                    move-result-object v$listRegister
                    sget-object v$hostsRegister, $hosts
                    invoke-interface {v$hostsRegister, v$listRegister}, Ljava/util/Set;->addAll(Ljava/util/Collection;)Z
                """,
            )
        }
    }
}
