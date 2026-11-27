/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.clone

import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.replaceStrings
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireStandardInstall
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import app.morphe.util.asSequence
import app.morphe.util.getNode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import org.w3c.dom.Element

private const val CONTENT_URI_PREFIX = "content://"
private const val ACCOUNT_TYPE = "com.dubox.drive.jkeng.account_type"

private val ACCOUNT_XML_FILES = listOf("res/xml/authenticator.xml", "res/xml/sync_adapter.xml")

private val APP_LABELS = setOf("TeraBox", "@string/app_name")

private fun cloneString(string: String, renames: Map<String, String>): String? {
    renames[string]?.let { return it }
    if (!string.startsWith(CONTENT_URI_PREFIX)) return null

    val authority = string.removePrefix(CONTENT_URI_PREFIX).substringBefore('/')
    return renames[authority]?.let { string.replaceFirst(authority, it) }
}

internal fun ClassDef.renameCloneStaticStrings(renames: Map<String, String>, resolveMutableClass: () -> MutableClass) {
    val mutableClass by lazy(resolveMutableClass)

    staticFields.forEach { field ->
        val string = (field.initialValue as? StringEncodedValue)?.value ?: return@forEach
        val clone = cloneString(string, renames) ?: return@forEach
        val mutableField = mutableClass.staticFields.single { it.name == field.name && it.type == field.type }
        (mutableField.initialValue as MutableStringEncodedValue).setValue(clone)
    }
}

internal fun renameCloneDexStrings(renames: Map<String, String>) {
    classDefForEach { classDef ->
        classDef.renameCloneStaticStrings(renames) { mutableClassDefBy(classDef) }
    }
    val filters = renames.keys.map { string(it) } + string(CONTENT_URI_PREFIX, StringComparisonType.STARTS_WITH)
    replaceStrings(filters) { _, _, value -> cloneString(value, renames) }
}

@Suppress("unused")
val cloneAppPatch: Patch = patch(
    name = "Clone app",
    description = "Installs TeraBox as a separate app alongside the original, with its own account. " +
        "Each copy needs a different clone number.",
    default = false,
) {
    compatibleWith(AppCompatibilities.TERABOX)
    availability(requireStandardInstall)

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

    val appName by stringOption(
        key = "appName",
        title = "App name",
        description = "Name shown on the launcher, in recent apps and in Settings. " +
            "Leave empty for TeraBox followed by the last part of the package name.",
    ) {
        it.isNullOrBlank() || it.trim().first() !in "@?"
    }

    val renames = mutableMapOf<String, String>()

    dependsOn(
        patchesSettingsPatch,
        patch {
            execute {
                val clonePackageName = packageName!!
                val originalPackageName = AppCompatibilities.TERABOX.packageName!!

                fun cloneName(name: String) =
                    if (name == originalPackageName || name.startsWith("$originalPackageName.")) {
                        clonePackageName + name.removePrefix(originalPackageName)
                    } else {
                        "${clonePackageName}_$name"
                    }

                renames.clear()
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

                    val cloneLabel = appName?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "TeraBox ${clonePackageName.substringAfterLast('.')}"

                    document.getElementsByTagName("*").asSequence()
                        .filterIsInstance<Element>()
                        .filter { it.getAttribute("android:label") in APP_LABELS }
                        .forEach { it.setAttribute("android:label", cloneLabel) }
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

    execute {
        markPatchApplied("cloneApp", cloneAppPatch)
    }

    finalize {
        renameCloneDexStrings(renames)
    }
}
