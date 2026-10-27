/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.friends

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.returnEarlyWhen
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS = "${BEREAL_EXTENSION_PACKAGE}FriendSuggestions;"
private const val IS_REMOVED = "invoke-static { }, $EXTENSION_CLASS->removed()Z"

private fun MutableMethod.filterRegisterAfter(index: Int, body: (register: Int, temp: Int) -> String) {
    val register = when (val construction = getInstruction(index)) {
        is FiveRegisterInstruction -> construction.registerC
        is RegisterRangeInstruction -> construction.startRegister
        else -> throw PatchException("Unexpected constructor call: ${construction.opcode}")
    }
    val temp = getFreeRegisterProvider(index + 1, 1).getFreeRegister4Bit()
    addInstructionsWithLabels(
        index + 1,
        body(register, temp),
        ExternalLabel("keep", getInstruction(index + 1)),
    )
}

@Suppress("unused")
val removeFriendSuggestionsPatch = bytecodePatch(
    name = "Remove friend suggestions",
    description = "Removes suggested friends, contacts on BeReal and popular accounts from Friends and the feed.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch)

    execute {
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val recommendationType = FriendRecommendationToStringFingerprint.matchSingle().classDef.type
        friendRecommendationMapperFingerprint(recommendationType).matchSingle().method
            .returnEarlyWhen("$EXTENSION_CLASS->removed()Z", returnsNull = true)

        val sectionsType = RelationshipSectionsToStringFingerprint.matchSingle().classDef.type
        relationshipSectionsConstructorFingerprint(sectionsType).matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p7 .. p7 }, $EXTENSION_CLASS->suggestions(Ljava/util/List;)Ljava/util/List;
                move-result-object p7
            """,
        )

        val carouselType = FeedCarouselToStringFingerprint.matchSingle().classDef.type
        feedCarouselConstructionFingerprint(carouselType).matchAll(2..2).forEach { match ->
            val index = match.instructionMatches.first().index
            match.method.filterRegisterAfter(index) { register, temp ->
                """
                    $IS_REMOVED
                    move-result v$temp
                    if-eqz v$temp, :keep
                    const/16 v$register, 0x0
                """
            }
        }

        val widgetType = FeedWidgetToStringFingerprint.matchSingle().classDef.type
        feedWidgetConstructorFingerprint(widgetType).matchSingle().method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->shouldDisplay(Z)Z
                move-result p1
            """,
        )

        val placeholderType = ContactsPlaceholderToStringFingerprint.matchSingle().classDef.type
        contactsPlaceholderFingerprint(placeholderType).matchSingle().method.returnEarlyWhen("$EXTENSION_CLASS->removed()Z")
        SuggestionSectionFingerprint.matchSingle().method.returnEarlyWhen("$EXTENSION_CLASS->removed()Z")
        ContactsOnBeRealSectionFingerprint.matchSingle().method.returnEarlyWhen("$EXTENSION_CLASS->removed()Z")

        PopularAccountsToStringFingerprint.matchAll(2..2).forEach { visible ->
            val visibleType = visible.classDef
            val stateInterface = visibleType.interfaces.single()
            val popularAccounts = popularAccountsConstructionFingerprint(visibleType.type).matchSingle()
            val constructionIndex = popularAccounts.instructionMatches.first().index
            val removedState = (0 until constructionIndex)
                .map { popularAccounts.method.getInstruction(it) }
                .filter { it.opcode == Opcode.SGET_OBJECT }
                .map { (it as ReferenceInstruction).getReference<FieldReference>()!! }
                .single { it.type != visibleType.type && classDefBy(it.type).interfaces.contains(stateInterface) }
            popularAccounts.method.filterRegisterAfter(constructionIndex) { register, temp ->
                """
                    $IS_REMOVED
                    move-result v$temp
                    if-eqz v$temp, :keep
                    sget-object v$register, ${removedState.definingClass}->${removedState.name}:${removedState.type}
                """
            }
        }
    }
}
