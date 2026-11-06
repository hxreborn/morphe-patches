/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.duolingo.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.fillAppliedPatch
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.patches.shared.misc.smali.replaceResourceIdStubs
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

internal const val DUOLINGO_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/duolingo/"
internal const val PATCH_SETTINGS_CLASS = "${DUOLINGO_EXTENSION_PACKAGE}PatchSettings;"
private const val APPLIED_PATCHES_CLASS = "${DUOLINGO_EXTENSION_PACKAGE}AppliedPatches;"
private const val PATCH_CONTEXT_CLASS = "${DUOLINGO_EXTENSION_PACKAGE}PatchContext;"
private const val PATCHES_MENU_CLASS = "${DUOLINGO_EXTENSION_PACKAGE}PatchesMenu;"
private const val SETTINGS_STYLE_CLASS = "${DUOLINGO_EXTENSION_PACKAGE}SettingsStyle;"
private const val APPLICATION_CLASS = "Lcom/duolingo/core/DuoApp;"
private const val PATCHES_ROW_TAG = "hxrebornPatchesRowItem"
private const val PATCHES_SECTION_METHOD = "hxrebornPatchesSection"
private const val SECTION_TITLE = "Patches"
private const val COMPOSER_FROM_END = 2
private const val SECTION_PARAMETER_REGISTERS = 1
private const val HEADER_TITLE_SLOT = 0
private const val ROW_TITLE_SLOT = 0
private const val ROW_POSITION_SLOT = 1
private const val FUNCTION_TYPE = "Lkotlin/jvm/functions/Function0;"

internal val ICON_RESOURCES = listOf("patches", "premium", "dynamic_icon", "debug", "rampup", "reset", "info")
    .map { "hx_duolingo_ic_$it" }

private val logger = Logger.getLogger("app.morphe.patches.duolingo.misc.settings")

internal fun BytecodePatchContext.markPatchApplied(
    methodName: String,
    patch: BytecodePatch,
) = fillAppliedPatch(APPLIED_PATCHES_CLASS, methodName, patch)

private fun resource(path: String) = DuolingoBundleResources::class.java.getResourceAsStream(path)
    ?: throw PatchException("Missing patch resource $path")

private fun BytecodePatchContext.attachPatchContext() =
    mutableClassDefBy(APPLICATION_CLASS).methods
        .single { it.name == "onCreate" && it.parameterTypes.isEmpty() && it.returnType == "V" }
        .addInstruction(0, "invoke-static/range { p0 .. p0 }, $PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V")

private fun MutableMethod.lastInstructionWriting(callIndex: Int, register: Int) =
    (callIndex - 1 downTo 0).map { getInstruction<Instruction>(it) }.firstOrNull {
        it.opcode.setsRegister() && (it as? OneRegisterInstruction)?.registerA == register
    } ?: throw PatchException("No write to v$register before the call at $callIndex in $name")

private fun MutableMethod.argumentWrittenBefore(callIndex: Int, register: Int): String {
    val write = lastInstructionWriting(callIndex, register)
    return when {
        write is NarrowLiteralInstruction -> "const v%d, ${write.narrowLiteral}"
        write.opcode == Opcode.SGET_OBJECT ->
            "sget-object v%d, ${descriptor((write as ReferenceInstruction).reference as FieldReference)}"
        else -> throw PatchException("Unsupported argument write ${write.opcode} to v$register in $name")
    }
}

private fun MutableMethod.replayCallSmali(callIndex: Int, replaced: Map<Int, String>): String {
    val call = getInstruction<RegisterRangeInstruction>(callIndex)
    val target = call.getReference<MethodReference>()!!
    val arguments = target.parameterTypes.indices.joinToString("\n") { slot ->
        replaced[slot] ?: argumentWrittenBefore(callIndex, call.startRegister + slot).format(slot)
    }
    return "$arguments\ninvoke-static/range { v0 .. v${target.parameterTypes.lastIndex} }, ${descriptor(target)}"
}

private fun BytecodePatchContext.addPatchesSettingsSection() {
    val support = SupportSectionFingerprint
    val (header, _, row) = support.instructionMatches.map { it.index }
    val rowTarget = support.method.getInstruction<RegisterRangeInstruction>(row).getReference<MethodReference>()!!
    val rowParameters = rowTarget.parameterTypes.map(CharSequence::toString)
    val composerSlot = rowParameters.lastIndex - COMPOSER_FROM_END
    val composerMoveTemplate = "move-object/from16 v%d, p0"
    val headerTarget = support.method.getInstruction<RegisterRangeInstruction>(header).getReference<MethodReference>()!!
    val headerComposerSlot = headerTarget.parameterTypes.lastIndex - COMPOSER_FROM_END

    val headerCall = support.method.replayCallSmali(
        header,
        mapOf(
            HEADER_TITLE_SLOT to "const-string v$HEADER_TITLE_SLOT, \"$SECTION_TITLE\"",
            headerComposerSlot to composerMoveTemplate.format(headerComposerSlot),
        ),
    )
    val onClick = rowParameters.indexOf(FUNCTION_TYPE)
    val tag = rowParameters.lastIndexOf(STRING_TYPE)
    val rowCall = support.method.replayCallSmali(
        row,
        mapOf(
            ROW_TITLE_SLOT to "sget-object v$ROW_TITLE_SLOT, $PATCHES_MENU_CLASS->SETTINGS_ROW_TITLE:$STRING_TYPE",
            ROW_POSITION_SLOT to
                "sget-object v$ROW_POSITION_SLOT, $GROUPED_LIST_ITEM_POSITION_CLASS->NONE:$GROUPED_LIST_ITEM_POSITION_CLASS",
            tag to "const-string v$tag, \"$PATCHES_ROW_TAG\"",
            onClick to """
                new-instance v$onClick, $PATCHES_MENU_CLASS
                invoke-direct/range { v$onClick .. v$onClick }, $PATCHES_MENU_CLASS-><init>()V
            """.trimIndent(),
            composerSlot to composerMoveTemplate.format(composerSlot),
        ),
    )

    val section = support.classDef.addMethod(
        PATCHES_SECTION_METHOD,
        listOf(rowParameters[composerSlot]),
        "V",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
        rowParameters.size + SECTION_PARAMETER_REGISTERS,
        """
            $headerCall
            $rowCall
            return-void
        """,
    )

    Fingerprint(
        filters = listOf(
            methodCall(
                definingClass = support.classDef.type,
                name = support.method.name,
                parameters = support.method.parameterTypes.map(CharSequence::toString),
            ),
        ),
    ).matchAll().forEach { caller ->
        val callIndex = caller.instructionMatches.single().index
        val composerRegister = caller.method.getInstruction<FiveRegisterInstruction>(callIndex).registerE
        caller.method.addInstruction(
            callIndex + 1,
            "invoke-static/range { v$composerRegister .. v$composerRegister }, ${descriptor(section)}",
        )
    }
}

internal val patchesSettingsPatch = bytecodePatch {
    dependsOn(
        resourcePatch {
            execute {
                ICON_RESOURCES.forEach { name ->
                    resource("/duolingo/drawable/$name.xml").use { icon ->
                        get("res/drawable/$name.xml").outputStream().use(icon::copyTo)
                    }
                }
            }
        },
    )
    extendWith("extensions/extension.mpe")

    execute {
        attachPatchContext()
        try {
            replaceSettingsStyleStubs()
            addPatchesSettingsSection()
        } catch (ex: PatchException) {
            logger.warning("Skipped the hxreborn patches section: ${ex.message}")
        }
    }
}

private fun BytecodePatchContext.replaceSettingsStyleStubs() {
    with(mutableClassDefBy(SETTINGS_STYLE_CLASS)) {
        replaceResourceIdStubs(
            ResourceType.COLOR,
            mapOf(
                "snowColor" to "juicySnow",
                "polarColor" to "juicyPolar",
                "swanColor" to "juicySwan",
                "eelColor" to "juicyEel",
                "wolfColor" to "juicyWolf",
                "macawColor" to "juicyMacaw",
                "whaleColor" to "juicyWhale",
            ),
        )
        replaceResourceIdStubs(
            ResourceType.FONT,
            mapOf("boldFont" to "din_next_for_duolingo_bold", "regularFont" to "din_next_for_duolingo"),
        )
        replaceResourceIdStubs(ResourceType.DRAWABLE, mapOf("backIconDrawable" to "ic_arrow_back_black_24dp"))
    }
}

private object DuolingoBundleResources
