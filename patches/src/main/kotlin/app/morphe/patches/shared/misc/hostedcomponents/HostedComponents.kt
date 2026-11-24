/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.hostedcomponents

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.mutableClassDefByOrNull
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.extension.sharedHostedComponentsExtensionPatch
import app.morphe.patches.shared.misc.smali.addMethod
import app.morphe.util.asSequence
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Document
import org.w3c.dom.Element

internal const val CORE_COMPONENT_FACTORY = "androidx.core.app.CoreComponentFactory"
private const val HOSTED_COMPONENTS_CLASS = "Lapp/hxreborn/extension/shared/HostedComponents;"
private const val HOSTED_COMPONENT_FACTORY_CLASS = "Lapp/hxreborn/extension/shared/HostedComponentFactory;"
private const val APP_COMPONENT_FACTORY_CLASS = "Landroid/app/AppComponentFactory;"
private const val ROUTE_METHOD =
    "$HOSTED_COMPONENTS_CLASS->routeOrShowUnreachableMessage(Landroid/content/Context;Landroid/content/Intent;)Landroid/content/Intent;"
private const val LAUNCH_METHOD = "startActivityForResult"
private const val LAUNCH_SIGNATURE = "(Landroid/content/Intent;ILandroid/os/Bundle;)V"
private const val LAUNCH_REGISTERS = 4
private const val ENTRY_SEPARATOR = ";"
private const val HOST_SEPARATOR = "="
private const val ANDROID = "android:"
private val launchParameters = listOf("Landroid/content/Intent;", "I", "Landroid/os/Bundle;")
private val standardLaunchModes = setOf("", "standard")
private val checkedHostAttributes = listOf(
    "permission", "process", "taskAffinity", "documentLaunchMode", "noHistory",
    "screenOrientation", "excludeFromRecents", "theme", "configChanges", "windowSoftInputMode",
)
private val routeLaunch = """
    invoke-static/range { p0 .. p1 }, $ROUTE_METHOD
    move-result-object p1
    if-nez p1, :launch
    return-void
"""

internal class ComponentHost(val tag: String, val name: String, val attributes: Map<String, String> = emptyMap())

internal const val TRANSLUCENT_THEME = "@android:style/Theme.Translucent.NoTitleBar"

internal val licenseCheckActivityHost = ComponentHost("activity", "com.pairip.licensecheck.LicenseActivity")

internal val googleSignInActivityHost = ComponentHost(
    "activity",
    "com.google.android.gms.auth.api.signin.internal.SignInHubActivity",
    mapOf("excludeFromRecents" to "true", "theme" to TRANSLUCENT_THEME),
)

internal val workManagerReceiverHost = ComponentHost("receiver", "androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver")

internal class HostedComponents(private val componentFactory: String) {

    private val hosts = linkedMapOf<String, ComponentHost>()

    private val launchingActivities = linkedSetOf<String>()

    fun host(component: String, host: ComponentHost) {
        hosts[component] = host
    }

    fun routeLaunchesFrom(activity: String) {
        launchingActivities += activity
    }

    val patch: Patch = patch {
        dependsOn(sharedHostedComponentsExtensionPatch)

        execute {
            hosts.clear()
            launchingActivities.clear()
        }

        finalize {
            if (hosts.isEmpty()) throw PatchException("No component registered for a stock host")
            val packageName = document("AndroidManifest.xml").use { manifest ->
                val packageName = manifest.documentElement.getAttribute("package")
                val application = manifest.getElementsByTagName("application").item(0) as Element
                val declaredFactory = application.getAttribute("${ANDROID}appComponentFactory")
                if (declaredFactory != componentFactory) {
                    throw PatchException("Unexpected appComponentFactory $declaredFactory, expected $componentFactory")
                }
                hosts.values.distinctBy { it.tag to it.name }.forEach { manifest.requireHost(it, packageName) }
                packageName
            }
            val encoded = hosts.entries.joinToString(ENTRY_SEPARATOR) { (component, host) ->
                component + HOST_SEPARATOR + host.name.qualifiedIn(packageName)
            }
            mutableClassDefBy(HOSTED_COMPONENTS_CLASS).methods.single { it.name == "hostedComponents" }
                .returnEarly(encoded)
            extendComponentFactory()
            launchingActivities.forEach { routeLaunches(it.qualifiedIn(packageName)) }
        }
    }

    private fun extendComponentFactory() {
        var factory = mutableClassDefBy(componentFactory.toDescriptor())
        while (factory.superclass != APP_COMPONENT_FACTORY_CLASS) {
            if (factory.superclass == HOSTED_COMPONENT_FACTORY_CLASS) {
                throw PatchException(
                    "$componentFactory already extends $HOSTED_COMPONENT_FACTORY_CLASS: " +
                        "register every hosted component on one HostedComponents for this app",
                )
            }
            factory = mutableClassDefByOrNull(factory.superclass!!)
                ?: throw PatchException("$componentFactory does not extend $APP_COMPONENT_FACTORY_CLASS")
        }
        factory.setSuperClass(HOSTED_COMPONENT_FACTORY_CLASS)
        factory.methods.single { it.name == "<init>" && it.parameterTypes.isEmpty() }.apply {
            val index = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_DIRECT &&
                    getReference<MethodReference>()?.definingClass == APP_COMPONENT_FACTORY_CLASS
            }
            replaceInstruction(index, "invoke-direct { p0 }, $HOSTED_COMPONENT_FACTORY_CLASS-><init>()V")
        }
    }
}

private fun String.qualifiedIn(packageName: String) = if (startsWith(".")) packageName + this else this

private fun String.toDescriptor() = "L${replace('.', '/')};"

private fun Element.androidAttribute(name: String) = getAttribute(ANDROID + name)

private fun Document.requireHost(host: ComponentHost, packageName: String) {
    val name = host.name.qualifiedIn(packageName)
    val declared = getElementsByTagName(host.tag).asSequence().filterIsInstance<Element>()
        .filter { it.androidAttribute("name").qualifiedIn(packageName) == name }.toList()
    val element = declared.singleOrNull() ?: throw PatchException("Missing host ${host.tag} $name")
    val exported = element.androidAttribute("exported")
    val hasFilters = element.getElementsByTagName("intent-filter").length > 0
    val changed = buildList {
        if (exported == "true" || (exported.isEmpty() && hasFilters)) add("exported")
        if (element.androidAttribute("enabled") == "false") add("enabled")
        if (element.androidAttribute("launchMode") !in standardLaunchModes) add("launchMode")
        checkedHostAttributes.filter { element.androidAttribute(it) != host.attributes[it].orEmpty() }
            .mapTo(this) { "$it=" + element.androidAttribute(it) }
    }
    if (changed.isNotEmpty()) {
        throw PatchException("Host ${host.tag} $name changed its manifest flags: ${changed.joinToString()}")
    }
}

private fun routeLaunches(activity: String) {
    val activityClass = mutableClassDefBy(activity.toDescriptor())
    val launch = activityClass.methods.singleOrNull {
        it.name == LAUNCH_METHOD && it.parameterTypes.map(CharSequence::toString) == launchParameters
    }
    if (launch != null) {
        launch.addInstructionsWithLabels(0, routeLaunch, ExternalLabel("launch", launch.getInstruction(0)))
        return
    }
    activityClass.addMethod(
        LAUNCH_METHOD,
        launchParameters,
        "V",
        AccessFlags.PUBLIC.value,
        LAUNCH_REGISTERS,
        routeLaunch + """
            :launch
            invoke-super { p0, p1, p2, p3 }, ${activityClass.superclass}->$LAUNCH_METHOD$LAUNCH_SIGNATURE
            return-void
        """,
    )
}
