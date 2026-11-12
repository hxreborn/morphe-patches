/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.readera.shared.AppCompatAttachBaseContextFingerprint
import app.morphe.patches.readera.shared.CONTEXT_CLASS
import app.morphe.patches.readera.shared.ReadEraPalette
import app.morphe.patches.readera.shared.disableSignatureChecksPatch
import app.morphe.patches.readera.shared.getOrAddApplicationAttachBaseContext
import app.morphe.patches.readera.shared.patchesSettingsPatch
import app.morphe.patches.readera.shared.readEraPalette
import app.morphe.patches.readera.shared.registerReadEraPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.asSequence
import app.morphe.util.childElementsSequence
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/readera/AppTheme;"
private const val AMOLED_QUALIFIER = "wheel"
private const val CARD_VIEW = "androidx.cardview.widget.CardView"
private const val NAVIGATION_VIEW = "com.google.android.material.navigation.NavigationView"
private const val CARD_FILL_SITES = 9
private const val BAR_FOREGROUND_SITES = 3
private const val DRAWER_THEME = "hx_readera_drawer"
private const val CARD_FILL = "hx_readera_card_fill"
private const val OUTLINE = "hx_readera_outline"
private const val BAR_FOREGROUND = "hx_readera_bar_foreground"
private val themeIcons = listOf("theme", "stock", "amoled")
private val amoledControlColors = mapOf(
    "hx_readera_control_outline" to "#777777",
    "hx_readera_control_outline_disabled" to "#61777777",
    "hx_readera_control_thumb" to "#bdbdbd",
)
private val amoledMenuDrawableColors = mapOf(
    "hx_readera_switch_track" to mapOf("#1c3239" to "#171717", "#506267" to "#777777"),
    "hx_readera_switch_thumb_on" to mapOf("#051e25" to "#000000"),
)

private fun ResourcePatchContext.addColors(path: String, colors: Map<String, String>) {
    get(path).parentFile.mkdirs()
    if (!get(path).exists()) get(path).writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources />\n")
    document(path).use { document ->
        colors.forEach { (name, value) ->
            document.documentElement.appendChild(document.createElement("color").also {
                it.setAttribute("name", name)
                it.textContent = value
            })
        }
    }
}

private fun amoledPath(path: String): String {
    val directory = path.substringBeforeLast('/')
    val version = Regex("-v\\d+$").find(directory)?.value.orEmpty()
    return "${directory.removeSuffix(version)}-$AMOLED_QUALIFIER$version/${path.substringAfterLast('/')}"
}

private fun ResourcePatchContext.replaceColorReferences(
    paths: List<String>,
    select: (Element) -> List<String>,
    replace: (String) -> String?,
): Map<String, Int> = paths.associateWith { path ->
    document(path).use { document ->
        document.getElementsByTagName("*").asSequence().map { it as Element }.sumOf { element ->
            select(element).count { attribute ->
                replace(element.getAttribute(attribute))?.also { element.setAttribute(attribute, it) } != null
            }
        }
    }
}.filterValues { it > 0 }

private fun ResourcePatchContext.splitSharedColorRoles(palette: ReadEraPalette): Map<String, String> {
    val layouts = decode("res/layout*/*.xml")
    val drawables = decode("res/drawable*/*.xml")
    val cardFills = replaceColorReferences(layouts, { element ->
        if (element.tagName != CARD_VIEW) emptyList()
        else (0 until element.attributes.length).map { element.attributes.item(it).nodeName }
    }) { value -> "@color/$CARD_FILL".takeIf { value == "@color/${palette.page}" } }
    check(cardFills.isNotEmpty() && cardFills.values.all { it == CARD_FILL_SITES }) {
        "Expected layouts with $CARD_FILL_SITES card fills each, found $cardFills"
    }

    val outlines = replaceColorReferences(drawables, { element ->
        if (element.tagName == "stroke" && (element.parentNode as Element).childElementsSequence()
                .any { it.tagName == "solid" && it.getAttribute("android:color") == "@color/${palette.page}" }
        ) listOf("android:color") else emptyList()
    }) { value -> "@color/$OUTLINE".takeIf { value == "@color/${palette.card}" } }
    check(outlines.values.sum() == 1) { "Expected one outlined page shape, found $outlines" }

    val styleForegrounds = decode("res/values*/styles.xml").sumOf { path ->
        document(path).use { document ->
            document.getElementsByTagName("item").asSequence().map { it as Element }.filter {
                it.getAttribute("name") == "android:indeterminateTint" && it.textContent == "@color/${palette.bar}"
            }.onEach { it.textContent = "@color/$BAR_FOREGROUND" }.count()
        }
    }
    val selectorForegrounds = replaceColorReferences(decode("res/color*/*.xml") + drawables, { element ->
        if (element.tagName == "item" && (element.parentNode as Element).tagName == "selector") listOf("android:color") else emptyList()
    }) { value -> "@color/$BAR_FOREGROUND".takeIf { value == "@color/${palette.bar}" } }.values.sum()
    val foregrounds = styleForegrounds + selectorForegrounds
    check(foregrounds == BAR_FOREGROUND_SITES) { "Expected $BAR_FOREGROUND_SITES foreground uses of the bar color, found $foregrounds" }

    return mapOf(
        CARD_FILL to "@color/${palette.page}",
        OUTLINE to "@color/${palette.card}",
        BAR_FOREGROUND to "@color/${palette.bar}",
    )
}

private val appThemeResourcesPatch = resourcePatch {
    lateinit var palette: ReadEraPalette

    execute {
        palette = readEraPalette()
        println("ReadEra palette: $palette")
        addColors("res/values/colors.xml", splitSharedColorRoles(palette))
        document("res/values/styles.xml").use { document ->
            document.documentElement.appendChild(document.createElement("style").also { style ->
                style.setAttribute("name", DRAWER_THEME)
                style.appendChild(document.createElement("item").also {
                    it.setAttribute("name", palette.barAttribute)
                    it.textContent = palette.accent
                })
            })
        }
        themeIcons.forEach { name ->
            AppThemeResources::class.java.getResourceAsStream("/readera/theme/hx_readera_$name.xml")!!.use { input ->
                get("res/drawable/hx_readera_$name.xml").outputStream().use(input::copyTo)
            }
        }
    }

    finalize {
        val definedColors = document("res/values/colors.xml").use { document ->
            document.getElementsByTagName("color").asSequence().map { (it as Element).getAttribute("name") }.toSet()
        }
        addColors(
            "res/values-$AMOLED_QUALIFIER/colors.xml",
            mapOf(palette.page to "#000000", palette.bar to "#0b0b0b", palette.raised to "#1c1c1c", palette.card to "#171717") +
                palette.panels.associateWith { "#101010" } +
                mapOf(palette.cardOverlay to "#12ffffff", palette.selectedOverlay to "#2bffffff") +
                mapOf(CARD_FILL to "#171717", OUTLINE to "#2e2e2e", BAR_FOREGROUND to palette.accent) +
                amoledControlColors.filterKeys { it in definedColors },
        )

        val drawer = decode("res/layout*/*.xml").filter { path ->
            document(path).use { document -> document.getElementsByTagName(NAVIGATION_VIEW).length > 0 }
        }.single()
        get(amoledPath(drawer)).apply { parentFile.mkdirs() }.writeText(get(drawer).readText())
        document(amoledPath(drawer)).use { document ->
            (document.getElementsByTagName(NAVIGATION_VIEW).asSequence().single() as Element)
                .setAttribute("android:theme", "@style/$DRAWER_THEME")
        }

        amoledMenuDrawableColors.forEach { (name, colors) ->
            val path = "res/drawable/$name.xml"
            val text = colors.entries.fold(get(path).readText()) { text, (stock, amoled) ->
                if (stock !in text) throw PatchException("$path has no color $stock")
                text.replace(stock, amoled)
            }
            get(amoledPath(path)).apply { parentFile.mkdirs() }.writeText(text)
        }
    }
}

@Suppress("unused")
val appThemePatch = bytecodePatch(
    name = "App theme",
    description = "Adds a black AMOLED theme for the library, menus and Settings, picked in the patches menu. " +
        "Book pages keep their own colors.",
) {
    compatibleWith(AppCompatibilities.READERA)

    availability(requireArm64)

    dependsOn(patchesSettingsPatch, disableSignatureChecksPatch, appThemeResourcesPatch)

    execute {
        registerReadEraPatch("App theme", "AMOLED turns the library, menus and Settings black. Book pages are unchanged.")
        getOrAddApplicationAttachBaseContext().addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->applyToApplication($CONTEXT_CLASS)$CONTEXT_CLASS
                move-result-object p1
            """,
        )
        AppCompatAttachBaseContextFingerprint.matchAll(1..1).single().let { match ->
            val (_, superCall) = match.instructionMatches
            val context = superCall.getInstruction<FiveRegisterInstruction>().registerD
            match.method.addInstructions(
                superCall.index,
                """
                    invoke-static { v$context }, $EXTENSION_CLASS_DESCRIPTOR->wrap($CONTEXT_CLASS)$CONTEXT_CLASS
                    move-result-object v$context
                """,
            )
            println("ReadEra App theme attachBaseContext hook: ${match.method.definingClass}")
        }
    }
}

private object AppThemeResources
