/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.clone

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getNode
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element

private const val ACCOUNT_TYPE = "com.dubox.drive.jkeng.account_type"

private val ACCOUNT_XML_FILES = listOf("res/xml/authenticator.xml", "res/xml/sync_adapter.xml")

@Suppress("unused")
val cloneAppPatch = bytecodePatch(
    name = "Clone app",
    description = "Installs TeraBox as a separate app alongside the original, with its own account. " +
        "Each copy needs a different clone number.",
    default = false,
) {
    compatibleWith(AppCompatibilities.TERABOX)

    val packageName by stringOption(
        key = "packageName",
        default = "com.dubox.drive.clone1",
        values = (1..5).associate { "Clone $it" to "com.dubox.drive.clone$it" },
        title = "Package name",
        description = "Package name of this copy.",
        required = true,
    ) {
        it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")) && it != AppCompatibilities.TERABOX.packageName
    }

    val renames = mutableMapOf<String, String>()

    dependsOn(
        resourcePatch {
            execute {
                val clonePackageName = packageName!!
                val originalPackageName = AppCompatibilities.TERABOX.packageName!!

                fun cloneName(name: String) =
                    if (name == originalPackageName || name.startsWith("$originalPackageName.")) {
                        clonePackageName + name.removePrefix(originalPackageName)
                    } else {
                        "${clonePackageName}_$name"
                    }

                renames[originalPackageName] = clonePackageName
                renames[ACCOUNT_TYPE] = cloneName(ACCOUNT_TYPE)

                document("AndroidManifest.xml").use { document ->
                    val manifest = document.getNode("manifest") as Element
                    manifest.setAttribute("package", clonePackageName)

                    fun rename(tag: String, attribute: String, prefixedOnly: Boolean) =
                        document.getElementsByTagName(tag).asSequence()
                            .filterIsInstance<Element>()
                            .filter { !prefixedOnly || it.getAttribute(attribute).startsWith("$originalPackageName.") }
                            .forEach {
                                val name = it.getAttribute(attribute)
                                val clone = cloneName(name)
                                renames[name] = clone
                                it.setAttribute(attribute, clone)
                            }

                    rename("provider", "android:authorities", prefixedOnly = false)
                    rename("permission", "android:name", prefixedOnly = true)
                    rename("uses-permission", "android:name", prefixedOnly = true)

                    val cloneLabel = "TeraBox ${clonePackageName.substringAfterLast('.')}"

                    (document.getNode("application") as Element).setAttribute("android:label", cloneLabel)

                    document.getElementsByTagName("category").asSequence()
                        .filterIsInstance<Element>()
                        .filter { it.getAttribute("android:name") == "android.intent.category.LAUNCHER" }
                        .map { category -> category.parentNode.parentNode as Element }
                        .forEach { launcherActivity -> launcherActivity.setAttribute("android:label", cloneLabel) }
                }

                ACCOUNT_XML_FILES.forEach { path ->
                    document(path).use { document ->
                        val element = document.documentElement
                        listOf("android:accountType", "android:contentAuthority").forEach { attribute ->
                            renames[element.getAttribute(attribute)]?.let { element.setAttribute(attribute, it) }
                        }
                    }
                }
            }
        },
    )

    finalize {
        fun cloneString(string: String): String? {
            renames[string]?.let { return it }
            if (!string.startsWith("content://")) return null

            val authority = string.removePrefix("content://").substringBefore('/')
            return renames[authority]?.let { string.replaceFirst(authority, it) }
        }

        getAllClassesWithStrings().forEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }

            classDef.methods.forEach { method ->
                val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }

                method.implementation?.instructions?.forEachIndexed { index, instruction ->
                    val string = instruction.getReference<StringReference>()?.string ?: return@forEachIndexed
                    val clone = cloneString(string) ?: return@forEachIndexed
                    val register = (instruction as OneRegisterInstruction).registerA

                    mutableMethod.replaceInstruction(index, "const-string v$register, \"$clone\"")
                }
            }
        }
    }
}
