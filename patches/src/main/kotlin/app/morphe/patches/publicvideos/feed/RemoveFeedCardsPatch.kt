/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.feed

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.parameterRegister
import app.morphe.patches.shared.misc.smali.registerWidth
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val REMOVED_CARD_TYPES = listOf("CUSTOM_CARD", "MULTI_INFLUENCER_VIEW")
private const val DROP_REMOVED_CARD_TYPES_METHOD = "hxDropRemovedCardTypes"
private const val DROP_REMOVED_CARD_TYPES_REGISTERS = 8
private const val STRING_TYPE = "Ljava/lang/String;"

private val OBJECT_MOVES = setOf(Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_16)

private fun Method.checkedParameterIndex(check: Match.InstructionMatch): Int {
    val body = checkNotNull(implementation) { "$this has no body" }
    var checkedRegister = when (val call = check.instruction) {
        is FiveRegisterInstruction -> call.registerC
        is RegisterRangeInstruction -> call.startRegister
        else -> throw PatchException("Parameter check in $this is not an invoke instruction")
    }
    val instructions = body.instructions.toList()
    for (index in check.index - 1 downTo 0) {
        val instruction = instructions[index]
        if (instruction !is OneRegisterInstruction || instruction.registerA != checkedRegister || !instruction.opcode.setsRegister()) {
            continue
        }
        if (instruction !is TwoRegisterInstruction || instruction.opcode !in OBJECT_MOVES) break
        checkedRegister = instruction.registerB
    }
    var register = body.registerCount - parameterTypes.sumOf { it.registerWidth() }
    parameterTypes.forEachIndexed { index, type ->
        if (register == checkedRegister) return index
        register += type.registerWidth()
    }
    throw PatchException("Register $checkedRegister checked in $this is not a parameter")
}

@Suppress("unused")
val removeFeedCardsPatch = bytecodePatch(
    name = "Remove feed cards",
    description = "Removes follow suggestion cards and custom cards from the For You feed.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        val cardModel = FeedCardModelFingerprint.matchSingle()
        val (_, cardTypeCheck) = cardModel.instructionMatches
        val cardTypeClass = cardModel.method.parameterTypes[cardModel.method.checkedParameterIndex(cardTypeCheck)].toString()
        val cardTypeFields = cardModel.classDef.fields.filter { it.type == cardTypeClass }
        val cardTypeField = cardTypeFields.singleOrNull()
            ?: throw PatchException("${cardModel.classDef.type} has ${cardTypeFields.size} fields of type $cardTypeClass, expected 1")
        val removedTypeFields = REMOVED_CARD_TYPES.map { constantName ->
            val (_, _, assignment) = feedCardTypeConstantFingerprint(cardTypeClass, constantName).matchSingle().instructionMatches
            assignment.getInstruction<ReferenceInstruction>().reference as FieldReference
        }

        val feedState = FeedStateConstructorFingerprint.matchSingle()
        FeedStatePropertyOrderFingerprint.matchSingle()
        val (_, topicChipsCheck, _, cardListCheck) = feedState.instructionMatches
        val parameterTypes = feedState.method.parameterTypes.map(CharSequence::toString)
        val headerIdIndex = feedState.method.checkedParameterIndex(topicChipsCheck) + 1
        val cardListIndex = feedState.method.checkedParameterIndex(cardListCheck)
        val headerIdType = parameterTypes.getOrNull(headerIdIndex)
        if (headerIdType != STRING_TYPE) {
            throw PatchException(
                "Feed state constructor parameter $headerIdIndex after topicChips is $headerIdType, expected $STRING_TYPE",
            )
        }
        val listType = parameterTypes[cardListIndex]
        val toList = toImmutableListFingerprint(listType).matchSingle().method

        val removedChecks = removedTypeFields.joinToString("\n") { field ->
            """
                sget-object v4, ${descriptor(field)}
                if-eq v3, v4, :next
            """
        }
        val dropRemovedCardTypes = mutableClassDefBy(feedState.classDef).addStaticMethod(
            DROP_REMOVED_CARD_TYPES_METHOD,
            listOf(listType, STRING_TYPE),
            listType,
            DROP_REMOVED_CARD_TYPES_REGISTERS,
            """
                if-nez p1, :keep
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                invoke-interface {p0}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;
                move-result-object v1
                :next
                invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                move-result v2
                if-eqz v2, :done
                invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v2
                instance-of v3, v2, ${cardModel.classDef.type}
                if-eqz v3, :add
                move-object v3, v2
                check-cast v3, ${cardModel.classDef.type}
                iget-object v3, v3, ${descriptor(cardTypeField)}
                $removedChecks
                :add
                invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                goto :next
                :done
                invoke-static {v0}, ${descriptor(toList)}
                move-result-object p0
                :keep
                return-object p0
            """,
        )

        val headerRegister = feedState.method.parameterRegister(headerIdIndex)
        val listRegister = feedState.method.parameterRegister(cardListIndex)
        feedState.method.addInstructions(
            0,
            """
                invoke-static {$listRegister, $headerRegister}, $dropRemovedCardTypes
                move-result-object $listRegister
            """,
        )
    }
}
