/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/shared/FirefoxBytecode.kt
 */
package app.morphe.patches.firefox.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val TWO_STATE_PREFERENCE = "Landroidx/preference/TwoStatePreference;"

context(_: BytecodePatchContext)
internal fun settingsPreferenceDelegate(preferenceKey: String): FieldReference =
    settingsPreferenceDelegateFingerprint(preferenceKey).matchSingle()
        .instructionMatches.last().instruction.getReference<FieldReference>()!!

context(_: BytecodePatchContext)
internal fun settingsPreferenceGetter(preferenceKey: String): MutableMethod =
    settingsPreferenceGetterFingerprint(settingsPreferenceDelegate(preferenceKey)).matchSingle().method

context(_: BytecodePatchContext)
internal fun forceSettingsPreference(preferenceKey: String, value: Boolean, gate: String? = null) {
    val getter = settingsPreferenceGetter(preferenceKey)
    if (gate == null) {
        getter.returnEarly(value)
    } else {
        getter.whenEnabled(gate) { (scratch) ->
            """
                const/16 $scratch, ${if (value) "0x1" else "0x0"}
                return $scratch
            """
        }
    }
    settingsPreferenceRowFingerprint(preferenceKey).matchAll().forEach { match ->
        val row = match.instructionMatches.last()
        val register = row.getInstruction<OneRegisterInstruction>().registerA
        val lock = match.classDef.lockedPreferenceMethod(value, gate)
        match.method.addInstruction(row.index + 1, "invoke-static/range { v$register .. v$register }, $lock")
    }
}

private fun MutableClass.lockedPreferenceMethod(checked: Boolean, gate: String?): String {
    val name = (if (checked) "lockPreferenceChecked" else "lockPreferenceUnchecked") +
        if (gate == null) "" else "WhenEnabled"
    val descriptor = "$type->$name($TWO_STATE_PREFERENCE)V"
    if (methods.any { it.name == name }) return descriptor
    val unlessDisabled = if (gate == null) {
        ""
    } else {
        """
            invoke-static { }, $gate
            move-result v0
            if-nez v0, :lock
            return-void
            :lock
        """
    }
    return addStaticMethod(
        name,
        listOf(TWO_STATE_PREFERENCE),
        "V",
        2,
        """
            $unlessDisabled
            const/4 v0, ${if (checked) "0x1" else "0x0"}
            invoke-virtual { p0, v0 }, $TWO_STATE_PREFERENCE->setChecked(Z)V
            const/4 v0, 0x0
            invoke-virtual { p0, v0 }, Landroidx/preference/Preference;->setEnabled(Z)V
            return-void
        """,
    )
}

context(_: BytecodePatchContext)
internal fun MutableMethod.whenEnabled(
    gate: String,
    index: Int = 0,
    count: Int = 1,
    exclude: Int? = null,
    body: (List<String>) -> String,
) {
    var target = this
    var shift = 0
    val registers = try {
        val provider = if (exclude == null) {
            getFreeRegisterProvider(index, count)
        } else {
            getFreeRegisterProvider(index, count, exclude)
        }
        List(count) { "v${provider.getFreeRegister4Bit()}" }
    } catch (ex: IllegalArgumentException) {
        val first = implementation!!.registerCount
        val (clone, added) = addFreeRegisters(count)
        target = clone
        shift = added
        List(count) { "v${first + it}" }
    }
    val at = index + shift
    target.addInstructionsWithLabels(
        at,
        """
            invoke-static { }, $gate
            move-result ${registers[0]}
            if-eqz ${registers[0]}, :hx_stock
            ${body(registers)}
        """,
        ExternalLabel("hx_stock", target.getInstruction(at)),
    )
}
