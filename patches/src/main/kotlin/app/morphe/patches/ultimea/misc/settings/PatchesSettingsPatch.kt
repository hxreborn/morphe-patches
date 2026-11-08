/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.ultimea.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.fillAppliedPatch
import org.w3c.dom.Element

internal const val ULTIMEA_EXTENSION = "Lapp/hxreborn/extension/ultimea/"
private const val MENU = "${ULTIMEA_EXTENSION}PatchesMenu;"
private const val ACTIVITY = "app.hxreborn.extension.ultimea.PatchesSettingsActivity"
private val icons = listOf(
    "hx_ultimea_ic_back",
    "hx_ultimea_ic_reset",
    "hx_ultimea_ic_info",
    "hx_ultimea_ic_patches",
    "hx_ultimea_ic_distractions",
    "hx_ultimea_ic_popups",
    "hx_ultimea_ic_firmware",
)

internal fun BytecodePatchContext.markPatchApplied(methodName: String, patch: Patch<*>) =
    fillAppliedPatch("${ULTIMEA_EXTENSION}AppliedPatches;", methodName, patch, withDescription = false)

internal val patchesSettingsPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
    dependsOn(spoofSignaturePatch, resourcePatch {
        dependsOn(nativeMethodsPatch)
        execute {
            icons.forEach { name ->
                val stream = UltimeaResources::class.java.getResourceAsStream("/ultimea/drawable/$name.xml")
                    ?: throw PatchException("Missing patches menu icon $name")
                stream.use { get("res/drawable/$name.xml").outputStream().use(it::copyTo) }
            }
            listOf("values" to false, "values-night" to true).forEach { (qualifier, dark) ->
                document("res/$qualifier/styles.xml").use { document ->
                    val style = document.createElement("style")
                    style.setAttribute("name", "hx_ultimea_menu")
                    style.setAttribute("parent", if (dark) "@android:style/Theme.DeviceDefault.NoActionBar"
                        else "@android:style/Theme.DeviceDefault.Light.NoActionBar")
                    mapOf(
                        "android:statusBarColor" to if (dark) "#FF121212" else "#FFF6F6F6",
                        "android:navigationBarColor" to if (dark) "#FF121212" else "#FFF6F6F6",
                        "android:windowLightStatusBar" to (!dark).toString(),
                    ).forEach { (name, value) ->
                        val item = document.createElement("item")
                        item.setAttribute("name", name)
                        item.textContent = value
                        style.appendChild(item)
                    }
                    document.documentElement.appendChild(style)
                }
            }
            val library = dartAotLibrary()
            library.writeBytes(library.readBytes().apply { addPatchesMenu() })
            document("AndroidManifest.xml").use { document ->
                val application = document.getElementsByTagName("application").item(0) as Element
                val activity = document.createElement("activity")
                activity.setAttribute("android:name", ACTIVITY)
                activity.setAttribute("android:exported", "false")
                activity.setAttribute("android:theme", "@style/hx_ultimea_menu")
                application.appendChild(activity)
            }
        }
    })
    execute {
        FlutterInitFingerprint.matchAll(1..1).single().method.addInstructions(
            0,
            """
                invoke-static { p1, p2 }, ${ULTIMEA_EXTENSION}NativeSettings;->prepare(Landroid/content/Context;[Ljava/lang/String;)[Ljava/lang/String;
                move-result-object p2
            """,
        )
        LoganBindingFingerprint.matchAll(1..1)
        val method = LoganMethodCallFingerprint.matchAll(1..1).single().method
        val context = mutableClassDefBy(method.definingClass).fields.single { it.type == "Landroid/content/Context;" }
        val callType = method.parameterTypes.first().toString()
        val methodField = mutableClassDefBy(callType).fields.single { it.type == "Ljava/lang/String;" }
        val resultType = method.parameterTypes.last().toString()
        val success = mutableClassDefBy(resultType).methods.single {
            it.returnType == "V" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        method.addInstructionsWithLabels(
            0,
            """
                move-object/from16 v0, p1
                iget-object v1, v0, $callType->${methodField.name}:Ljava/lang/String;
                move-object/from16 v0, p0
                iget-object v0, v0, ${method.definingClass}->${context.name}:Landroid/content/Context;
                invoke-static { v0, v1 }, $MENU->open(Landroid/content/Context;Ljava/lang/String;)Z
                move-result v0
                if-eqz v0, :checkTracking
                const/4 v1, 0x0
                move-object/from16 v0, p2
                invoke-interface { v0, v1 }, $resultType->${success.name}(Ljava/lang/Object;)V
                return-void
                :checkTracking
                invoke-static {}, ${ULTIMEA_EXTENSION}AppliedPatches;->disableTracking()Z
                move-result v0
                if-eqz v0, :originalLogan
                move-object/from16 v0, p2
                invoke-static { v1 }, $MENU->suppressedLoganResult(Ljava/lang/String;)Ljava/lang/Object;
                move-result-object v1
                invoke-interface { v0, v1 }, $resultType->${success.name}(Ljava/lang/Object;)V
                return-void
            """,
            ExternalLabel("originalLogan", method.getInstruction(0)),
        )
    }
}

private object UltimeaResources
