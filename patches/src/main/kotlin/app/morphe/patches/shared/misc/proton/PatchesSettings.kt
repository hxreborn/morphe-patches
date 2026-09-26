/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

internal const val SETTINGS_ROW_TITLE = "hxreborn patches"
private const val SETTINGS_ACTIVITY_CLASS = "app.hxreborn.extension.proton.PatchesSettingsActivity"
private const val BUNDLE_VERSION_RESOURCE = "/proton-bundle-version.txt"
private const val PATCHES_THEME_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchesTheme;"
private const val APP_COMPAT_PACKAGE = "Landroidx/appcompat/app/"
private const val APP_COMPAT_MODE_NIGHT_NO = 1
private const val APP_COMPAT_MODE_NIGHT_YES = 2

internal object CoreNightModeFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.type.startsWith(CORE_COMPOSE_THEME_PACKAGE) },
    filters = listOf(
        anyInstruction(
            fieldAccess(opcode = Opcode.SGET, type = "I"),
            methodCall(opcode = Opcode.INVOKE_STATIC, parameters = emptyList(), returnType = "I"),
        ),
        literal(APP_COMPAT_MODE_NIGHT_NO, location = MatchAfterWithin(2)),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        literal(APP_COMPAT_MODE_NIGHT_YES, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
    ),
)

internal fun patchesSettingsActivityPatch(themeStyle: String) = resourcePatch {
    finalize {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY_CLASS)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", themeStyle)
            application.appendChild(activity)
        }
    }
}

internal fun BytecodePatchContext.injectBundleVersion() {
    val bundleVersion = PatchesSettingsVersion::class.java
        .getResourceAsStream(BUNDLE_VERSION_RESOURCE)?.bufferedReader()?.use {
            it.readText().trim()
        } ?: throw PatchException("Patch bundle version resource $BUNDLE_VERSION_RESOURCE is unavailable")

    mutableClassDefBy(PATCHES_MENU_CLASS).methods.single { it.name == "bundleVersion" }
        .returnEarly(bundleVersion)
}

internal fun BytecodePatchContext.injectAppCompatDefaultNightMode() {
    val nightModeReads = CoreNightModeFingerprint.matchAll()
        .map { (it.instructionMatches.first().instruction as ReferenceInstruction).reference }
        .distinctBy(Any::toString)
    val nightModeRead = nightModeReads.singleOrNull()
        ?: throw PatchException("Expected one AppCompat default night mode read, found $nightModeReads")

    val (readInstructions, definingClass) = when (nightModeRead) {
        is FieldReference ->
            "sget v0, ${DexFormatter.INSTANCE.getFieldDescriptor(nightModeRead)}" to nightModeRead.definingClass
        is MethodReference ->
            "invoke-static { }, ${DexFormatter.INSTANCE.getMethodDescriptor(nightModeRead)}\nmove-result v0" to
                nightModeRead.definingClass
        else -> throw PatchException("Unexpected night mode read: $nightModeRead")
    }
    if (!definingClass.startsWith(APP_COMPAT_PACKAGE)) {
        throw PatchException("Core night mode reads $definingClass, expected a class in $APP_COMPAT_PACKAGE")
    }

    mutableClassDefBy(PATCHES_THEME_CLASS).methods.single { it.name == "appCompatDefaultNightMode" }.addInstructions(
        0,
        """
            $readInstructions
            return v0
        """,
    )
}

private object PatchesSettingsVersion
