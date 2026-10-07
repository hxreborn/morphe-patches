/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.pairip

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import app.morphe.util.removeFromParent
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import org.w3c.dom.Element

private const val KEY_IMPORT_CONSTANTS_CLASS = "Lcom/pairip/vmencryption/KeyImportConstants;"
private const val KEY_IMPORT_APPLICATION = "com.pairip.vmencryption.KeyImportApplication"
private const val KEY_IMPORT_FACTORY = "com.pairip.vmencryption.KeyImportAppComponentFactory"
private const val KEY_IMPORT_COMPONENT_PREFIX = "com.pairip.vmencryption.KeyImport"
private const val NAME_ATTRIBUTE = "android:name"
private const val FACTORY_ATTRIBUTE = "android:appComponentFactory"

private class KeyImportOriginals(val application: String, val componentFactory: String)

private lateinit var keyImportOriginals: KeyImportOriginals

private val readKeyImportOriginalsPatch = bytecodePatch {
    execute {
        val constants = classDefBy(KEY_IMPORT_CONSTANTS_CLASS)

        fun original(fieldName: String): String {
            val initialValue = constants.staticFields.singleOrNull { it.name == fieldName }?.initialValue
            return (initialValue as? StringEncodedValue)?.value?.takeIf { it.isNotEmpty() }
                ?: throw PatchException("$KEY_IMPORT_CONSTANTS_CLASS->$fieldName has no non-empty string value")
        }

        keyImportOriginals = KeyImportOriginals(
            original("originalApplicationClassName"),
            original("originalAppComponentFactoryClassName"),
        )
    }
}

private fun Element.restoreAttribute(name: String, expectedStub: String, original: String) {
    val declared = getAttribute(name)
    check(declared == expectedStub) { "Expected $name=$expectedStub in the manifest, found $declared" }
    setAttribute(name, original)
}

internal val removePairipKeyImportPatch = resourcePatch {
    dependsOn(readKeyImportOriginalsPatch)

    execute {
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element
            application.restoreAttribute(NAME_ATTRIBUTE, KEY_IMPORT_APPLICATION, keyImportOriginals.application)
            application.restoreAttribute(FACTORY_ATTRIBUTE, KEY_IMPORT_FACTORY, keyImportOriginals.componentFactory)

            application.childNodes.asSequence()
                .filterIsInstance<Element>()
                .filter { it.getAttribute(NAME_ATTRIBUTE).startsWith(KEY_IMPORT_COMPONENT_PREFIX) }
                .toList()
                .ifEmpty { throw PatchException("No KeyImport components found in the manifest") }
                .forEach { it.removeFromParent() }
        }
    }
}
