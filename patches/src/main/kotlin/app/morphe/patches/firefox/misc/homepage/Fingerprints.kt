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

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val HOMEPAGE_URL = "about:home"

internal object HomepageTabFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, name = "homepageTitle"),
        string(HOMEPAGE_URL, MatchAfterWithin(5)),
        methodCall(
            definingClass = "Lmozilla/components/feature/tabs/TabsUseCases\$AddNewTabUseCase;",
            name = "invoke\$default",
            location = MatchAfterWithin(15),
        ),
    ),
)

internal object HomepageButtonFingerprint : Fingerprint(
    filters = listOf(
        string(HOMEPAGE_URL),
        methodCall(
            definingClass = "Lmozilla/components/feature/session/SessionUseCases\$LoadUrlUseCase;",
            name = "invoke\$default",
            location = MatchAfterWithin(5),
        ),
    ),
)

private const val ADD_NEW_HOMEPAGE_TAB = "addNewHomepageTab\$default"
private const val FENIX_BROWSER_USE_CASES = "Lorg/mozilla/fenix/components/usecases/FenixBrowserUseCases;"

internal object OpenHomeIntentFingerprint : Fingerprint(
    filters = listOf(
        string("open_to_home"),
        methodCall(definingClass = FENIX_BROWSER_USE_CASES, name = ADD_NEW_HOMEPAGE_TAB),
        methodCall(
            definingClass = "Lorg/mozilla/fenix/ext/NavControllerKt;",
            name = "nav",
            location = MatchAfterWithin(20),
        ),
    ),
)

internal object TabManagerNewTabFingerprint : Fingerprint(
    filters = listOf(
        methodCall(definingClass = FENIX_BROWSER_USE_CASES, name = ADD_NEW_HOMEPAGE_TAB),
        methodCall(name = "handleNavigateToHome", location = MatchAfterWithin(3)),
    ),
)

internal object ColdStartHomepageTabFingerprint : Fingerprint(
    definingClass = "Lorg/mozilla/fenix/HomeActivity;",
    name = "onCreate",
    filters = listOf(
        methodCall(definingClass = "Lorg/mozilla/fenix/utils/Settings;", name = "getEnableHomepageAsNewTab"),
        methodCall(definingClass = "Lorg/mozilla/fenix/ext/ContextKt;", name = "getApplication", location = MatchAfterWithin(3)),
        methodCall(definingClass = FENIX_BROWSER_USE_CASES, name = ADD_NEW_HOMEPAGE_TAB, location = MatchAfterWithin(10)),
    ),
)
