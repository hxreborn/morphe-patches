/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.hostedcomponents

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patches.shared.misc.smali.replaceStub
import app.morphe.util.asSequence
import com.android.tools.smali.dexlib2.AccessFlags
import org.w3c.dom.Element

private const val STYLE_TYPE = "style"
private const val HEX_PREFIX = "0x"
private const val HEX_RADIX = 16

internal fun declarePublicResources(type: String, names: List<String>): Map<String, Long> =
    document("res/values/public.xml").use { declarations ->
        val declared = declarations.getElementsByTagName("public").asSequence().filterIsInstance<Element>()
            .filter { it.getAttribute("type") == type }.toList()
        val declaredNames = declared.mapTo(HashSet()) { it.getAttribute("name") }
        var nextId = declared.maxOf { it.getAttribute("id").removePrefix(HEX_PREFIX).toLong(HEX_RADIX) } + 1
        names.associateWith { name ->
            if (!declaredNames.add(name)) throw PatchException("Resource $type/$name is already declared")
            declarations.documentElement.appendChild(
                declarations.createElement("public").apply {
                    setAttribute("type", type)
                    setAttribute("name", name)
                    setAttribute("id", HEX_PREFIX + nextId.toString(HEX_RADIX))
                },
            )
            nextId++
        }
    }

internal fun declarePublicStyle(name: String): Long = declarePublicResources(STYLE_TYPE, listOf(name)).getValue(name)

internal fun addStyle(name: String, parent: String): Long {
    document("res/values/styles.xml").use { styles ->
        styles.documentElement.appendChild(
            styles.createElement(STYLE_TYPE).apply {
                setAttribute("name", name)
                setAttribute("parent", parent)
            },
        )
    }
    return declarePublicStyle(name)
}

internal fun MutableClass.replaceThemeStub(stub: String, styleId: Long) {
    if (!AccessFlags.STATIC.isSet(methods.single { it.name == stub }.accessFlags)) {
        throw PatchException("Theme stub $type->$stub is not static")
    }
    replaceStub(
        stub,
        1,
        """
            const v0, $HEX_PREFIX${styleId.toString(HEX_RADIX)}
            return v0
        """,
    )
}
