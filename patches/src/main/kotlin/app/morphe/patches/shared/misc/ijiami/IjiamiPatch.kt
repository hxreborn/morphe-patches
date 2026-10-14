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

private val PACKER_ABI_BY_LIBRARY_ABI = mapOf(
    "arm64-v8a" to "arm64-v8a",
    "armeabi-v7a" to "armeabi",
)

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
        val profiles = nativeProfiles()
        if (profiles.isEmpty()) {
            throw PatchException("No supported Ijiami packer library in assets/ijm_lib")
        }
        profiles.forEach { (abi, profile) -> installNativeLibrary(abi, profile) }

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

private fun packerLibrary(abi: String) = "assets/ijm_lib/$abi/libexec.so"

private fun ResourcePatchContext.nativeProfiles(): Map<String, NativeProfile> =
    PACKER_ABI_BY_LIBRARY_ABI.mapNotNull { (abi, packerAbi) ->
        val packer = get(packerLibrary(packerAbi))
        if (!packer.exists() || !shipsNativeLibraries(abi)) return@mapNotNull null

        val libexec = packer.readBytes()
        val profile = NativeProfiles.forLibexecOrNull(libexec)
        if (profile == null) {
            logger.warning("Unsupported Ijiami native packer build for $abi: libexec.so sha-256 ${NativeProfiles.sha256(libexec)}")
        }
        profile?.let { abi to it }
    }.toMap()

private fun ResourcePatchContext.installNativeLibrary(abi: String, profile: NativeProfile) {
    get("lib/$abi/$NATIVE_LIBRARY", copy = false).writeBytes(bundledIjiamiResource(profile.resource))
    logger.info("Native packer profile for $abi: ${profile.name}")
}

private fun ResourcePatchContext.shipsNativeLibraries(abi: String) =
    get("lib/$abi").isDirectory || !get("lib").isDirectory

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
