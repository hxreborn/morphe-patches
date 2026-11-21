/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import app.morphe.patcher.patch.patch

private const val FIXTURE_PACKAGE = "Lapp/hxreborn/extension/foo/"

private const val FIXTURE_BAR = "${FIXTURE_PACKAGE}Bar;"

private fun fixtureCall(smali: String) = smali.length

internal val fixtureLoaderPatch = patch {
    extendWith("extensions/hxreborn/foo.mpe")
}

internal val fixtureInterpolatedPatch = patch {
    dependsOn(fixtureLoaderPatch)
    execute { fixtureCall("${FIXTURE_BAR}->run()V") }
}

internal val fixtureSiblingPatch = patch {
    execute { fixtureCall("${FIXTURE_PACKAGE}Bar;->run()V") }
}

internal val fixtureHelperPatch = patch {
    execute { fixtureHelper() }
}

private fun fixtureHelper() = fixtureCall("Lapp/hxreborn/extension/foo/Bar;->help()V")

internal val fixtureMissingOwnerPatch = patch {
    execute { fixtureCall("Lapp/hxreborn/extension/foo/Missing;->run()V") }
}

internal val fixtureLibraryPatch = patch {
    execute { fixtureCall("Lapp/morphe/extension/shared/Missing;->run()V") }
}

internal val fixtureQuietPatch = patch {
    execute { fixtureCall("nothing") }
}

private const val FIXTURE_DOTTED_PACKAGE = "app.hxreborn.extension.foo"

private fun fixtureQualified(name: String) = "$FIXTURE_DOTTED_PACKAGE.$name"

internal val fixtureHelperQualifiedPatch = patch {
    execute { fixtureCall(fixtureQualified("Bar")) }
}

internal val fixtureUnownedViewPatch = patch {
    execute { fixtureCall(fixtureQualified("UnownedView")) }
}

internal val fixtureUnownedNestedPatch = patch {
    execute { fixtureCall(fixtureQualified("Outer${'$'}InnerActivity")) }
}

internal val fixtureUnownedDeeplyNestedPatch = patch {
    execute { fixtureCall(fixtureQualified("Outer${'$'}Middle${'$'}InnerActivity")) }
}
