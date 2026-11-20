/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.patch.bytecodePatch

private const val FIXTURE_PACKAGE = "Lapp/hxreborn/extension/foo/"

private const val FIXTURE_BAR = "${FIXTURE_PACKAGE}Bar;"

private fun fixtureCall(smali: String) = smali.length

internal val fixtureLoaderPatch = bytecodePatch {
    extendWith("extensions/hxreborn/foo.mpe")
}

internal val fixtureInterpolatedPatch = bytecodePatch {
    dependsOn(fixtureLoaderPatch)
    execute { fixtureCall("${FIXTURE_BAR}->run()V") }
}

internal val fixtureSiblingPatch = bytecodePatch {
    execute { fixtureCall("${FIXTURE_PACKAGE}Bar;->run()V") }
}

internal val fixtureHelperPatch = bytecodePatch {
    execute { fixtureHelper() }
}

private fun fixtureHelper() = fixtureCall("Lapp/hxreborn/extension/foo/Bar;->help()V")

internal val fixtureMissingOwnerPatch = bytecodePatch {
    execute { fixtureCall("Lapp/hxreborn/extension/foo/Missing;->run()V") }
}

internal val fixtureLibraryPatch = bytecodePatch {
    execute { fixtureCall("Lapp/morphe/extension/shared/Missing;->run()V") }
}

internal val fixtureQuietPatch = bytecodePatch {
    execute { fixtureCall("nothing") }
}

private const val FIXTURE_DOTTED_PACKAGE = "app.hxreborn.extension.foo"

private fun fixtureQualified(name: String) = "$FIXTURE_DOTTED_PACKAGE.$name"

internal val fixtureHelperQualifiedPatch = bytecodePatch {
    execute { fixtureCall(fixtureQualified("Bar")) }
}

internal val fixtureUnownedViewPatch = bytecodePatch {
    execute { fixtureCall(fixtureQualified("UnownedView")) }
}

internal val fixtureUnownedNestedPatch = bytecodePatch {
    execute { fixtureCall(fixtureQualified("Outer${'$'}InnerActivity")) }
}

internal val fixtureUnownedDeeplyNestedPatch = bytecodePatch {
    execute { fixtureCall(fixtureQualified("Outer${'$'}Middle${'$'}InnerActivity")) }
}
