/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ijiami

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.misc.signature.stockSigningCertificate
import app.morphe.util.inputStreamFromBundledResource
import org.w3c.dom.Element
import java.util.logging.Logger

private const val NATIVE_LIBRARY = "libhxpatch.so"
private const val NATIVE_LIBRARY_ENTRY = "lib/arm64-v8a/$NATIVE_LIBRARY"

private const val PACKER_ARM64_LIBRARY = "assets/ijm_lib/arm64-v8a/libexec.so"

private const val CERTIFICATE_ASSET = "assets/hx.cer"
private const val FACTORY_ASSET = "assets/hx.factory"

private const val DELEGATE_FACTORY = "hx.Delegate"
private const val FACTORY_ATTRIBUTE = "android:appComponentFactory"

private const val SIGNATURE_CHECK_CLASS = "Lcom/ijiami/residconfusion/ConfusionUtils;"

private val logger = Logger.getLogger("app.morphe.patches.shared.misc.ijiami")

internal fun bundledIjiamiResource(name: String): ByteArray {
    val resource = inputStreamFromBundledResource("ijiami", name)
        ?: throw PatchException("Missing bundled resource: ijiami/$name")

    return resource.use { it.readBytes() }
}

internal fun ResourcePatchContext.editIjiamiPayload(block: IjiamiPayload.() -> Unit) {
    val classes = get("classes.dex")
    val container = IjiamiContainer.of(classes.readBytes())

    val payload = container.decrypt()
    val editable = IjiamiPayload(PayloadDex.split(payload), container.opaqueRanges)
    editable.block()
    editable.seal()

    classes.writeBytes(container.rebuild(payload))
}

internal val ijiamiPatch = resourcePatch {
    execute {
        val packer = get(PACKER_ARM64_LIBRARY)
        if (!packer.exists()) {
            throw PatchException(
                "Missing $PACKER_ARM64_LIBRARY. Ijiami support requires an arm64 APK.",
            )
        }

        val profile = NativeProfiles.forLibexec(packer.readBytes())
        get(NATIVE_LIBRARY_ENTRY, copy = false).writeBytes(bundledIjiamiResource(profile.resource))
        logger.info("Native packer profile: ${profile.name}")

        val dexEntry = freeDexEntry()
        get(dexEntry, copy = false).writeBytes(bundledIjiamiResource("boot.dex"))
        get(CERTIFICATE_ASSET, copy = false)
            .writeBytes(packageMetadata.stockSigningCertificate().encoded)

        val packerFactory = interposeComponentFactory()
        get(FACTORY_ASSET, copy = false).writeText(packerFactory)
        logger.info("Component factory: $packerFactory -> $DELEGATE_FACTORY ($dexEntry)")

        editIjiamiPayload {
            val check = methodOrNull(SIGNATURE_CHECK_CLASS, "check")
            if (check == null) {
                logger.fine("No editable $SIGNATURE_CHECK_CLASS->check in payload")
            } else {
                check.returnEarly()
                logger.info("Disabled $SIGNATURE_CHECK_CLASS->check")
            }
        }
    }
}

private fun ResourcePatchContext.interposeComponentFactory(): String =
    document("AndroidManifest.xml").use { manifest ->
        val application = manifest.getElementsByTagName("application").item(0) as? Element
            ?: throw PatchException("AndroidManifest.xml declares no application")

        val declared = application.getAttribute(FACTORY_ATTRIBUTE)
        if (declared.isEmpty()) {
            throw PatchException(
                "Missing $FACTORY_ATTRIBUTE in AndroidManifest.xml",
            )
        }
        if (declared == DELEGATE_FACTORY) {
            throw PatchException("Ijiami support is already applied")
        }

        val packageName = application.ownerDocument.documentElement.getAttribute("package")
            .ifEmpty { packageMetadata.packageName }
        application.setAttribute(FACTORY_ATTRIBUTE, DELEGATE_FACTORY)
        resolveComponentName(declared, packageName)
    }

private fun resolveComponentName(name: String, packageName: String) = when {
    name.startsWith(".") -> packageName + name
    '.' in name -> name
    else -> "$packageName.$name"
}

internal fun ResourcePatchContext.freeDexEntry() =
    generateSequence(2) { it + 1 }
        .map { "classes$it.dex" }
        .first { !get(it).exists() }
