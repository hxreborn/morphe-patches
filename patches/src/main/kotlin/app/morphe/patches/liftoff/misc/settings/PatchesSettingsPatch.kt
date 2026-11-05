/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.liftoff.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.misc.reactnative.ScrollViewLayoutFingerprint
import app.morphe.patches.shared.misc.revenuecat.InvalidateCustomerInfoCacheFingerprint
import app.morphe.patches.shared.misc.smali.replaceStub
import com.android.tools.smali.util.StringUtils.escapeString
import org.w3c.dom.Element

internal const val LIFTOFF_PATCH_SETTINGS_CLASS = "Lapp/hxreborn/extension/liftoff/PatchSettings;"
private const val LIFTOFF_PATCH_CONTEXT_CLASS = "Lapp/hxreborn/extension/liftoff/PatchContext;"
private const val LIFTOFF_PATCHES_MENU_CLASS = "Lapp/hxreborn/extension/liftoff/PatchesMenu;"
private const val LIFTOFF_APPLIED_PATCHES_CLASS = "Lapp/hxreborn/extension/liftoff/AppliedPatches;"
private const val CACHE_INVALIDATION_REGISTERS = 0
private val ICON_NAMES = listOf("back", "premium", "customizations", "reset", "info", "patches", "entry", "expand")

internal fun BytecodePatchContext.markPatchApplied(methodName: String, patch: BytecodePatch) {
    val methods = mutableClassDefBy(LIFTOFF_APPLIED_PATCHES_CLASS).methods
    methods.single { it.name == methodName }.returnEarly(true)
    methods.single { it.name == methodName + "Name" }.returnEarly(escapeString(checkNotNull(patch.name)))
    methods.single { it.name == methodName + "Description" }.returnEarly(escapeString(checkNotNull(patch.description)))
}

internal val patchesSettingsPatch = run {
    lateinit var applicationType: String
    bytecodePatch {
        dependsOn(
            resourcePatch {
                execute {
                    document("AndroidManifest.xml").use { document ->
                        val application = document.getElementsByTagName("application").item(0) as Element
                        val packageName = document.documentElement.getAttribute("package")
                        val applicationName = application.getAttribute("android:name")
                        val resolvedName = when {
                            applicationName.startsWith('.') -> packageName + applicationName
                            '.' !in applicationName -> "$packageName.$applicationName"
                            else -> applicationName
                        }
                        applicationType = "L${resolvedName.replace('.', '/')};"
                    }
                    ICON_NAMES.forEach { name ->
                        val filename = "hx_liftoff_ic_$name.xml"
                        val resource = LiftoffResources::class.java.getResourceAsStream("/liftoff/drawable/$filename")
                            ?: throw PatchException("Missing patch icon $filename")
                        resource.use { icon ->
                            get("res/drawable/$filename").outputStream().use(icon::copyTo)
                        }
                    }
                }
            },
        )
        extendWith("extensions/extension.mpe")
        execute {
            applicationOnCreateFingerprint(applicationType).matchSingle().method.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $LIFTOFF_PATCH_CONTEXT_CLASS->attach(Landroid/app/Application;)V",
            )
            ScrollViewLayoutFingerprint.matchSingle().method.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $LIFTOFF_PATCHES_MENU_CLASS->onScrollLayout(Landroid/widget/ScrollView;)V",
            )
            val invalidation = InvalidateCustomerInfoCacheFingerprint.matchSingle().method
            mutableClassDefBy(LIFTOFF_PATCH_SETTINGS_CLASS).replaceStub(
                "invalidatePremiumCache",
                CACHE_INVALIDATION_REGISTERS,
                """
                    invoke-static {}, ${invalidation.definingClass}->${invalidation.name}()V
                    return-void
                """,
            )
        }
    }
}

private object LiftoffResources
