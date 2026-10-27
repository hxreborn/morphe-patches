/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.privacy

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.asSequence
import app.morphe.util.setExtensionIsPatchIncluded
import org.w3c.dom.Element

private val contactsPermissions = setOf(
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
)

private val removeContactsPermissionsResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            val declared = manifest.childNodes.asSequence().filterIsInstance<Element>()
                .filter { it.tagName == "uses-permission" && it.getAttribute("android:name") in contactsPermissions }
                .toList()
            val missing = contactsPermissions - declared.map { it.getAttribute("android:name") }.toSet()
            if (missing.isNotEmpty()) throw PatchException("Permissions not declared: $missing")
            declared.forEach { manifest.removeChild(it) }
        }
    }
}

private val contactsPermissionExtensionPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")
}

@Suppress("unused")
val removeContactsPermissionPatch = bytecodePatch(
    name = "Remove contacts permission",
    description = "Removes the contacts permission. BeReal can't ask for or read your phone's contacts.",
) {
    compatibleWith(AppCompatibilities.BEREAL)

    dependsOn(removeContactsPermissionsResourcePatch, contactsPermissionExtensionPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}ContactsPermission;")
    }
}
