/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from fork-maintainers/iceraven-browser (MPL-2.0):
 * https://github.com/fork-maintainers/iceraven-browser/commit/56c08e8000c4e16c88010ddf2e991016ddb50a8c
 * Commit 56c08e8000c4e16c88010ddf2e991016ddb50a8c (2026-10-20),
 * app/src/main/java/org/mozilla/fenix/components/toolbar/BrowserToolbarMiddleware.kt
 */
package app.morphe.patches.firefox.misc.homepage

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.forceSettingsPreference
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.addStaticMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil
import java.net.URI

private const val NAV_CONTROLLER = "Landroidx/navigation/NavController;"
private const val CUSTOM_HOMEPAGE = "$PATCH_SETTINGS->customHomepage()Z"

private fun MutableClass.navigateToBrowserMethod(): String = addStaticMethod(
    "navigateToBrowser",
    listOf(NAV_CONTROLLER),
    "V",
    3,
    """
        sget v0, Lorg/mozilla/fenix/R${'$'}id;->browserFragment:I
        invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
        move-result-object v1
        invoke-static {p0, v1}, Lorg/mozilla/fenix/ext/NavControllerKt;->alreadyOnDestination(${NAV_CONTROLLER}Ljava/lang/Integer;)Z
        move-result v1
        if-nez v1, :on_browser
        const/4 v1, 0x0
        invoke-virtual {p0, v0, v1, v1}, $NAV_CONTROLLER->navigate(ILandroid/os/Bundle;Landroidx/navigation/NavOptions;)V
        :on_browser
        return-void
    """,
)

private fun isWebUrl(url: String) = runCatching { URI(url) }.getOrNull()
    ?.let { (it.scheme == "https" || it.scheme == "http") && !it.host.isNullOrEmpty() } == true

@Suppress("unused")
val customHomepagePatch = bytecodePatch(
    name = "Custom homepage",
    description = "Opens a chosen web page as the homepage and in new tabs.",
    default = false,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    val homepageUrl by stringOption(
        key = "homepageUrl",
        title = "Homepage URL",
        description = "Full address starting with https://, such as https://duckduckgo.com.",
        required = true,
    ) {
        it != null && isWebUrl(it.trim())
    }

    extendWith("extensions/extension.mpe")

    execute {
        val url = homepageUrl!!.trim()

        listInSheet("customHomepage")
        mutableClassDefBy(PATCH_SETTINGS).methods.single { it.name == "defaultHomepageUrl" }.returnEarly(url)

        forceSettingsPreference("pref_key_enable_homepage_as_new_tab", true, CUSTOM_HOMEPAGE)

        (HomepageTabFingerprint.matchAll() + HomepageButtonFingerprint.matchSingle()).forEach { match ->
            val homepage = match.instructionMatches.first { it.instruction.opcode == Opcode.CONST_STRING }
            val register = homepage.getInstruction<OneRegisterInstruction>().registerA
            match.method.addInstructions(
                homepage.index + 1,
                """
                    invoke-static/range { v$register .. v$register }, $PATCH_SETTINGS->homepage(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """,
            )
        }

        OpenHomeIntentFingerprint.matchSingle().let { match ->
            val nav = match.instructionMatches.last()
            val call = nav.getInstruction<FiveRegisterInstruction>()
            val reference = call.getReference<MethodReference>()!!
            val original = ReferenceUtil.getMethodDescriptor(reference)
            val navigate = match.classDef.navigateToBrowserMethod()
            val parameters = reference.parameterTypes.map(CharSequence::toString)
            val openHome = match.classDef.addStaticMethod(
                "navigateHome",
                parameters,
                reference.returnType,
                parameters.size + 1,
                """
                    invoke-static { }, $CUSTOM_HOMEPAGE
                    move-result v0
                    if-eqz v0, :stock
                    invoke-static { p0 }, $navigate
                    return-void
                    :stock
                    invoke-static { p0, p1, p2, p3 }, $original
                    return-void
                """,
            )
            match.method.replaceInstruction(
                nav.index,
                "invoke-static { v${call.registerC}, v${call.registerD}, v${call.registerE}, v${call.registerF} }, $openHome",
            )
        }

        TabManagerNewTabFingerprint.matchSingle().let { match ->
            val navigateHome = match.instructionMatches.last()
            val controller = navigateHome.getInstruction<FiveRegisterInstruction>().registerC
            val reference = navigateHome.instruction.getReference<MethodReference>()!!
            val original = ReferenceUtil.getMethodDescriptor(reference)
            val navigate = match.classDef.navigateToBrowserMethod()
            val openHome = match.classDef.addStaticMethod(
                "navigateHome",
                listOf(reference.definingClass),
                "V",
                2,
                """
                    invoke-static { }, $CUSTOM_HOMEPAGE
                    move-result v0
                    if-eqz v0, :stock
                    iget-object v0, p0, ${match.classDef.type}->navController:$NAV_CONTROLLER
                    invoke-static { v0 }, $navigate
                    return-void
                    :stock
                    invoke-virtual { p0 }, $original
                    return-void
                """,
            )
            match.method.replaceInstruction(navigateHome.index, "invoke-static { v$controller }, $openHome")
        }

        ColdStartHomepageTabFingerprint.matchSingle().let { match ->
            val (_, application, addTab) = match.instructionMatches
            val activity = application.getInstruction<FiveRegisterInstruction>().registerC
            val openBrowser = match.classDef.addStaticMethod(
                "openHomepageInBrowser",
                listOf(match.classDef.type),
                "V",
                3,
                """
                    invoke-static { }, $CUSTOM_HOMEPAGE
                    move-result v0
                    if-eqz v0, :done
                    sget-object v0, Lorg/mozilla/fenix/BrowserDirection;->FromGlobal:Lorg/mozilla/fenix/BrowserDirection;
                    const/4 v1, 0x0
                    invoke-virtual { p0, v0, v1 }, Lorg/mozilla/fenix/HomeActivity;->openToBrowser(Lorg/mozilla/fenix/BrowserDirection;Ljava/lang/String;)V
                    :done
                    return-void
                """,
            )
            match.method.addInstruction(addTab.index + 1, "invoke-static { v$activity }, $openBrowser")
        }
    }
}
