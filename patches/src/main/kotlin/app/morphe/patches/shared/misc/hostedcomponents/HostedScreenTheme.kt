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

internal fun declarePublicStyle(name: String): Long = document("res/values/public.xml").use { declarations ->
    val styles = declarations.getElementsByTagName("public").asSequence().filterIsInstance<Element>()
        .filter { it.getAttribute("type") == STYLE_TYPE }.toList()
    if (styles.any { it.getAttribute("name") == name }) throw PatchException("Style $name is already declared")
    val id = styles.maxOf { it.getAttribute("id").removePrefix(HEX_PREFIX).toLong(HEX_RADIX) } + 1
    declarations.documentElement.appendChild(
        declarations.createElement("public").apply {
            setAttribute("type", STYLE_TYPE)
            setAttribute("name", name)
            setAttribute("id", HEX_PREFIX + id.toString(HEX_RADIX))
        },
    )
    id
}

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
